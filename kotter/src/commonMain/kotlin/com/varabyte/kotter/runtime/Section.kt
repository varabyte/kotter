package com.varabyte.kotter.runtime

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.platform.concurrent.locks.*
import com.varabyte.kotter.platform.internal.collections.*
import com.varabyte.kotter.platform.internal.concurrent.*
import com.varabyte.kotter.platform.internal.concurrent.annotations.*
import com.varabyte.kotter.runtime.RunScope.*
import com.varabyte.kotter.runtime.concurrent.*
import com.varabyte.kotter.runtime.coroutines.*
import com.varabyte.kotter.runtime.internal.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.internal.text.*
import com.varabyte.kotter.runtime.render.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.min

internal val ActiveSectionKey = Section.Lifecycle.createKey<Section>()
internal val AsideRendersKey = Section.Lifecycle.createKey<MutableList<Renderer<AsideRenderScope>>>()

private val WIPE_CURRENT_LINE_COMMAND: String = "\r${Ansi.Csi.Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode()}"

/**
 * Common interface used for scopes that can appear in both the render and run blocks.
 *
 * For example:
 *
 * ```
 * section {
 *   ... // A
 * }.run {
 *   ... // B
 * }
 *
 * // A is a RenderScope and ALSO a SectionScope
 * // B is a RunScope and ALSO a SectionScope
 * ```
 */
interface SectionScope {
    val data: ConcurrentScopedData
}

/**
 * A scope associated with the [run] function.
 *
 * While its [Lifecycle] is probably *almost* the same as its section's lifecycle, it is a little shorter, and
 * this matters because some data may need to be cleaned up after running but before the block is actually finished.
 *
 * @property section The [Section] this run block is attached to.
 */
class RunScope(val section: Section, private val scope: CoroutineScope) : SectionScope {
    object Lifecycle : ConcurrentScopedData.Lifecycle {
        override val parent = Section.Lifecycle
    }

    /**
     * Data store for this session.
     *
     * It is exposed directly and publicly here so methods extending this [RunScope] can use it.
     */
    override val data = section.session.data

    private val waitLatch = CompletableDeferred<Unit>()

    /** Forcefully exit this run scope early, even if it's still in progress */
    internal fun abort() {
        signal() // In case abort is run inside a `runUntilSignal` block
        scope.cancel()
    }

    /**
     * Request an additional rerender pass against the underlying section.
     *
     * This method is provided for maximum flexibility, but prefer using a [LiveVar] (or similar auto-rerendering
     * technique) whenever possible.
     */
    fun rerender() = section.requestRerender()

    /**
     * Block this method from continuing until [signal] is called.
     *
     * If [signal] is called before this method is, then it won't block at all.
     *
     * Note: You may wish to use [runUntilSignal] instead, to avoid needing to call this method yourself.
     */
    suspend fun waitForSignal() = waitLatch.await()

    /**
     * Fire a signal so that [waitForSignal] is allowed to continue.
     *
     * A common pattern here is to handle waiting for some event to occur before continuing:
     *
     * ```
     * val downloader = ...
     * section { ... }.run {
     *    downloader.onDownloaded { file ->
     *       ... do something with the downloaded file and then ...
     *       signal()
     *    }
     *
     *    waitForSignal() // Block here or else the run block would exit immediately
     * }
     * ```
     */
    fun signal() = waitLatch.complete(Unit)
}

/**
 * The main [RenderScope] used for rendering a section.
 */
class MainRenderScope(renderer: Renderer<MainRenderScope>) : RenderScope(renderer) {
    object Lifecycle : ConcurrentScopedData.Lifecycle {
        override val parent = Section.Lifecycle
    }

    val width get() = renderer.session.terminal.width
    val height get() = renderer.session.terminal.height
}

/**
 * The class which represents the state of a `section` block and its registered event handlers (e.g. [run] and
 * [onFinishing]).
 *
 * A user cannot instantiate this directly. Instead, use [Session.section].
 *
 * @property session The parent session this section was created by.
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

    internal val renderer = Renderer(session) { MainRenderScope(it) }
    private val renderLock = ReentrantLock()

    @GuardedBy("renderLock")
    private var renderRequested = false

    /**
     * A list of callbacks to trigger after every render.
     */
    private var onRendered = mutableListOf<OnRenderedScope.() -> Unit>()

    /**
     * A list of callbacks to trigger right before the block exits.
     */
    private var onFinishing = mutableListOf<() -> Unit>()

    private var consumed = AtomicReference(false)

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

    private var lastCommandsRendered = emptyList<TerminalCommand>()

    private fun renderOnceAsync(): Job {
        return CoroutineScope(KotterDispatchers.Render).launch {
            session.data.start(MainRenderScope.Lifecycle)
            // Make sure run logic doesn't modify values while we're in the middle of rendering
            session.data.lock.write {
                renderLock.withLock { renderRequested = false }

                val clearBlockCommand = buildString {
                    if (lastCommandsRendered.isNotEmpty()) {
                        // To clear an existing block of 'n' lines, completely delete all but one of them, and then delete the
                        // last one down to the beginning (in other words, don't consume the \n of the previous line)
                        // NOTE: We need to re-add auto newlines because the screen width might have changed since last time
                        val numLinesToErase = min(
                            lastCommandsRendered.withImplicitNewlines(session.terminal.width)
                                .count { it is NewlineCommand } + 1, session.terminal.height)
                        for (i in 0 until numLinesToErase) {
                            append(WIPE_CURRENT_LINE_COMMAND)
                            if (i < numLinesToErase - 1) {
                                append(Ansi.Csi.Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode())
                            }
                        }

                        lastCommandsRendered = emptyList()
                    }
                }

                val asideTextBuilder = StringBuilder()
                session.data.get(AsideRendersKey) {
                    if (this.isEmpty()) return@get

                    forEach { renderer ->
                        asideTextBuilder.append(renderer.commands.toText())
                    }
                    // Only render asides once. Since we don't erase them, they'll be baked into the history.
                    clear()
                }


                try {
                    renderer.render(render)
                } catch (t: Throwable) {
                    session.sectionExceptionHandler(t)
                }

                lastCommandsRendered = renderer.commands.withImplicitNewlines(session.terminal.width)

                // Send the whole set of instructions through `write` at once so the clear and updates are processed
                // in one pass.
                session.terminal.write(
                    clearBlockCommand
                            + asideTextBuilder.toString()
                            + lastCommandsRendered.toText(session.terminal.height)
                )

                onRendered.removeIf {
                    val scope = OnRenderedScope()
                    it.invoke(scope)
                    scope.removeListener
                }
            }
            session.data.stop(MainRenderScope.Lifecycle)
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
     * various calls that allocate side state (like `input()`) could use this to see if they were called one frame and
     * not the next (at which point, they could clean up their resources). It may also be useful for tests.
     */
    fun onRendered(block: OnRenderedScope.() -> Unit): Section {
        @Suppress("RemoveRedundantQualifierName") // Useful to show "Section.Lifecycle" for readability
        require(session.data.isActive(Section.Lifecycle))
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
        @Suppress("RemoveRedundantQualifierName") // Useful to show "Section.Lifecycle" for readability
        require(session.data.isActive(Section.Lifecycle))
        onFinishing.add(block)

        return this
    }

    /**
     * Run a section block, applying its commands, thereby rendering them.
     *
     * A run block blocks the calling thread, only returning control back once it has finished. This is in contrast to
     * the section render block, which runs in parallel on its own thread. It is expected that in most cases, the run
     * block logic will update values that trigger section rerenders.
     *
     * A run block may run for an arbitrarily long time (e.g. blocking until the user presses 'q' to quit), and while it
     * runs, it keeps the current section it is attached to active.
     *
     * Without calling this method, the contents of a [Section] block are inert and useless. In fact, it is considered
     * an error to create a section with no run block. If you forget to do so, the owning [Session] will notify you
     * about your mistake as early as it can. (When it happens, it is invariably user error).
     *
     * While you can always call this method directly, there are multiple convenience `run` variations that delegate to
     * this call under the hood, which may be more appropriate choices based on the purpose of your section,such as
     * [runUntilSignal], [runUntilInputEntered], and [runUntilKeyPressed].
     */
    fun run(block: (suspend RunScope.() -> Unit)? = null) {
        val wasConsumed = consumed.compareAndSet(expected = false, newValue = true)
        if (!wasConsumed) {
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
        val allRendersFinished = CompletableDeferred<Unit>()
        CoroutineScope(KotterDispatchers.Render).launch { allRendersFinished.complete(Unit) }
        runBlocking { allRendersFinished.await() }

        session.data.stop(Lifecycle)
        deferredException?.let { throw it }
    }
}
