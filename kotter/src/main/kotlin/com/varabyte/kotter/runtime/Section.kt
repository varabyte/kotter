package com.varabyte.kotter.runtime

import com.varabyte.kotter.foundation.LiveVar
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.render.AsideRenderScope
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.runtime.concurrent.ConcurrentScopedData
import com.varabyte.kotter.runtime.concurrent.createKey
import com.varabyte.kotter.runtime.internal.ansi.Ansi
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
class RunScope(val section: Section, private val scope: CoroutineScope): SectionScope {
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
    fun waitForSignal() = waitLatch.await()

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

    private var consumed = AtomicBoolean(false)

    private val abortLock = ReentrantLock()
    @GuardedBy("abortLock")
    private var abortRequested = false
    @GuardedBy("abortLock")
    private var handleAbort: () -> Unit = { abortRequested = true }

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
            session.data.start(MainRenderScope.Lifecycle)
            // Rendering might crash, and if so, we should still propagate the exception but only after we've cleaned up
            // our rendering.
            var deferredException: Exception? = null
            // Make sure run logic doesn't modify values while we're in the middle of rendering
            session.data.lock.write {
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
                    }
                    // Only render asides once. Since we don't erase them, they'll be baked into the history.
                    clear()
                }


                try {
                    renderer.render(render)
                } catch (ex: Exception) {
                    deferredException = ex
                }
                // Send the whole set of instructions through "write" at once so the clear and updates are processed
                // in one pass.
                session.terminal.write(clearBlockCommand + asideTextBuilder.toString() + renderer.commands.toRawText())

                onRendered.removeIf {
                    val scope = OnRenderedScope()
                    it.invoke(scope)
                    scope.removeListener
                }
            }
            session.data.stop(MainRenderScope.Lifecycle)

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
     * various calls that allocate side state (like `input()`) could use this to see if they were called one frame and
     * not the next (at which point, they could clean up their resources). It may also be useful for tests.
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

                    val scope = abortLock.withLock {
                        if (!abortRequested) {
                            val scope = RunScope(self, this)
                            handleAbort = { scope.abort() }
                            scope
                        } else {
                            null
                        }
                    }

                    scope?.block()
                }
            } catch (ignored: CancellationException) {
                // This is expected as it can happen when abort() is called in `run`
            } catch (ex: Exception) {
                deferredException = ex
            } finally {
                abortLock.withLock {
                    abortRequested = false
                    handleAbort = { abortRequested = true }
                }
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
    }

    /**
     * Like [abort] but does not block the calling thread.
     */
    fun abortAsync() {
        abortLock.withLock {
            handleAbort()
        }
    }

    /**
     * Attempt to cancel this run block.
     *
     * This call will block until the section associated with the run block is torn down.
     *
     * This offers a way to forcefully shut down a section that is blocking on one thread when you have access to that
     * section instance (either directly or through [Session.activeSection]) from a different thread.
     *
     * Note: A user should almost never need to use this in production. This method was written for testing purposes,
     * where a library using Kotter on the backend wanted to send it user input and then shut things down before
     * verifying the most recent state of the terminal.
     */
    fun abort() {
        val latch = CountDownLatch(1)
        session.data.lock.write {
            if (session.data.isActive(RunScope.Lifecycle)) {
                session.data.onLifecycleDeactivated {
                    // Wait for the section to tear down, which is guaranteed to happen shortly after the run block
                    // finishes.
                    if (lifecycle === Section.Lifecycle) {
                        removeListener = true
                        latch.countDown()
                    }
                }
                abortAsync()
            } else {
                latch.countDown()
            }
        }
        latch.await()
    }
}