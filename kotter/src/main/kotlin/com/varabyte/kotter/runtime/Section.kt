package com.varabyte.kotter.runtime

import com.varabyte.kotter.foundation.render.AsideRenderScope
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData
import com.varabyte.kotter.runtime.concurrent.createKey
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.kotter.runtime.internal.ansi.commands.TextCommand
import com.varabyte.kotter.runtime.internal.text.numLines
import com.varabyte.kotter.runtime.internal.text.toRawText
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotter.runtime.render.Renderer
import kotlinx.coroutines.*
import net.jcip.annotations.GuardedBy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.concurrent.write

internal val ActiveSectionKey = Section.Lifecycle.createKey<Section>()
internal val AsideRendersKey = Section.Lifecycle.createKey<MutableList<Renderer<AsideRenderScope>>>()

/**
 * Common interface used for scopes that can appear in both the render and run blocks.
 *
 * ```
 * section {
 *   ... # A
 * }.run {
 *   ... # B
 * }
 *
 * A is a RenderScope and ALSO a SectionScope
 * B is a RunScope and ALSO a SectionScope
 * ```
 */
interface SectionScope {
    val data: ConcurrentScopedData
}

/**
 * A scope associated with the [run] function.
 *
 * While the lifecycle is probably *almost* the same as its section's lifecycle, it is a little shorter, and
 * this matters because some data may need to be cleaned up after running but before the block is actually finished.
 */
class RunScope(
    val section: Section,
    private val scope: CoroutineScope,
): SectionScope {
    object Lifecycle : ConcurrentScopedData.Lifecycle {
        override val parent = Section.Lifecycle
    }

    /**
     * Data store for this session.
     *
     * It is exposed directly and publicly here so methods extending the RunScope can use it.
     */
    override val data = section.session.data

    private val waitLatch = CountDownLatch(1)
    /** Forcefully exit this runscope early, even if it's still in progress */
    internal fun abort() { scope.cancel() }
    fun rerender() = section.requestRerender()
    fun waitForSignal() = waitLatch.await()
    fun signal() = waitLatch.countDown()
}

/**
 * The main [RenderScope] used for rendering a section.
 *
 * While it seems unnecessary to create an empty class like this, this can be useful if library authors want to provide
 * extension methods that only apply to `section` render scopes.
 */
class MainRenderScope(renderer: Renderer<MainRenderScope>): RenderScope(renderer) {
    object Lifecycle : ConcurrentScopedData.Lifecycle {
        override val parent = Section.Lifecycle
    }
}

/**
 * The class which represents the state of a `section` block and its registered event handlers (e.g. [run] and
 * [onFinishing])
 */
class Section internal constructor(val session: Session, private val render: MainRenderScope.() -> Unit) {
    /**
     * A moderately long lifecycle that lives as long as the block is running.
     *
     * This lifecycle can be used for storing data relevant to the current block only.
     */
    object Lifecycle : ConcurrentScopedData.Lifecycle {
        override val parent = Session.Lifecycle
    }

    class OnRenderedScope(var removeListener: Boolean = false)

    init {
        session.data.start(Lifecycle)
    }

    internal val renderer = Renderer<MainRenderScope>(session) { MainRenderScope(it) }
    private val renderLock = ReentrantLock()
    @GuardedBy("renderLock")
    private var renderRequested = false

    /**
     * A list of callbacks to trigger after every render.
     *
     * It is not expected for a user to add more than one, but internal components might themselves add listeners as
     * well.
     */
    private var onRendered = mutableListOf<OnRenderedScope.() -> Unit>()

    /**
     * A list of callbacks to trigger right before the block exits.
     *
     * It is not expected for a user to add more than one, but internal components might themselves add listeners
     * behind the scenes to clean up their state.
     */
    private var onFinishing = mutableListOf<() -> Unit>()

    private var consumed = AtomicBoolean(false)

    /**
     * Let the block know we want to rerender an additional frame.
     *
     * This will not enqueue a render if one is already queued up.
     */
    fun requestRerender() {
        if (session.activeSection != this) return

        renderLock.withLock {
            // If we get multiple render requests in a short period of time, we only need to handle one of them - the
            // remaining requests are redundant and will be covered by the initial one.
            if (!renderRequested) {
                renderRequested = true
                renderOnceAsync()
            }
        }
    }

    private fun renderOnceAsync(): Job {
        return CoroutineScope(session.executor.asCoroutineDispatcher()).launch {
            renderLock.withLock { renderRequested = false }

            val clearBlockCommand = buildString {
                if (renderer.commands.isNotEmpty()) {

                    // Note: This logic works when a terminal first starts up, but if the user keeps resizing their
                    // terminal while our session is running, it seems like the width value we get doesn't update. See
                    // also: bug #34
                    val totalNumLines = renderer.commands.numLines(session.terminal.width)

                    // To clear an existing block of 'n' lines, completely delete all but one of them, and then delete the
                    // last one down to the beginning (in other words, don't consume the \n of the previous line)
                    for (i in 0 until totalNumLines) {
                        append('\r')
                        append(Ansi.Csi.Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode())
                        if (i < totalNumLines - 1) {
                            append(Ansi.Csi.Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode())
                        }
                    }
                }
            }

            val asideTextBuilder = StringBuilder()
            session.data.get(AsideRendersKey) {
                if (this.isEmpty()) return@get

                forEach { renderer ->
                    asideTextBuilder.append(renderer.commands.toRawText())
                    if (renderer.commands.asSequence().filter { it is TextCommand }.lastOrNull() !== NEWLINE_COMMAND) {
                        asideTextBuilder.append('\n')
                    }
                }
                // Only render asides once. Since we don't erase them, they'll be baked into the history.
                clear()
            }

            session.data.start(MainRenderScope.Lifecycle)
            // Rendering might crash, and if so, we should still propagate the exception but only after we've cleaned up
            // our rendering.
            var deferredException: Exception? = null
            // Make sure run logic doesn't modify values while we're in the middle of rendering
            session.data.lock.write {
                try {
                    renderer.render(render)
                } catch (ex: Exception) {
                    deferredException = ex
                }
                onRendered.removeIf {
                    val scope = OnRenderedScope()
                    it.invoke(scope)
                    scope.removeListener
                }
            }
            session.data.stop(MainRenderScope.Lifecycle)

            // Send the whole set of instructions through "write" at once so the clear and updates are processed
            // in one pass.
            session.terminal.write(clearBlockCommand + asideTextBuilder.toString() + renderer.commands.toRawText())
            deferredException?.let { throw it }
        }
    }
    private fun renderOnce() = runBlocking {
        renderOnceAsync().join()
    }

    /**
     * Add a callback which will get triggered after this block has just about finished running and is about to shut
     * down.
     *
     * Users shouldn't need this - they can just put a counter variable directly inside a section for example - but
     * various components that allocate side state could use this to see if they were called one frame and not the
     * next (at which point, they could clean up their resources).
     */
    fun onRendered(block: OnRenderedScope.() -> Unit): Section {
        require(session.data.isActive(Lifecycle))
        onRendered.add(block)

        return this
    }

    /**
     * Add a callback which will get triggered after this block has just about finished running and is about to shut
     * down.
     *
     * This is a good opportunity to change any values back to some initial state if necessary (such as a blinking
     * cursor). Changes made in `onFinishing` may potentially kick off one final render pass.
     */
    fun onFinishing(block: () -> Unit): Section {
        require(session.data.isActive(Lifecycle))
        onFinishing.add(block)

        return this
    }

    fun run(block: (suspend RunScope.() -> Unit)? = null) {
        val prevValue = consumed.getAndSet(true)
        if (prevValue) {
            throw IllegalStateException("Cannot rerun a section that was previously run")
        }

        // Note: The data we're adding here will be removed by the `dispose` call below
        if (!session.data.tryPut(ActiveSectionKey) { this }) {
            throw IllegalStateException("Cannot run this section while another section is already running")
        }

        session.data.start(RunScope.Lifecycle)
        renderOnce()

        // Running might crash, and if so, we should still propagate the exception but only after we've cleaned up post
        // run.
        var deferredException: Exception? = null

        if (block != null) {
            val self = this
            try {
                runBlocking {
                    val scope = RunScope(self, this)
                    scope.block()
                }
            } catch (ignored: CancellationException) {
                // This is expected as it can happen when abort() is called in `run`
            } catch (ex: Exception) {
                deferredException = ex
            }
        }

        session.data.stop(RunScope.Lifecycle)

        onFinishing.forEach { it() }

        // Our run block is done, let's just wait until any remaining renders are finished. We can do this by adding
        // ourselves to the end of the line and waiting to get through.
        val allRendersFinishedLatch = CountDownLatch(1)
        session.executor.submit { allRendersFinishedLatch.countDown() }
        allRendersFinishedLatch.await()

        session.data.stop(Lifecycle)
        deferredException?.let { throw it }

        if (renderer.commands.asSequence().filter { it is TextCommand }.lastOrNull() !== NEWLINE_COMMAND) {
            session.terminal.write(NEWLINE_COMMAND.text)
        }
    }
}