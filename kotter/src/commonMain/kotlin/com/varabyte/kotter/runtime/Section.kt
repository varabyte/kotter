package com.varabyte.kotter.runtime

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.platform.concurrent.locks.*
import com.varabyte.kotter.platform.internal.collections.*
import com.varabyte.kotter.platform.internal.concurrent.*
import com.varabyte.kotter.platform.internal.concurrent.annotations.*
import com.varabyte.kotter.runtime.concurrent.*
import com.varabyte.kotter.runtime.coroutines.*
import com.varabyte.kotter.runtime.internal.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.internal.text.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.terminal.TerminalSize
import com.varabyte.kotter.runtime.terminal.TextMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.min

internal val ActiveSectionKey = Section.Lifecycle.createKey<Section>()
internal val AsideRendersKey = Section.Lifecycle.createKey<MutableList<Renderer<AsideRenderScope>>>()

private val WIPE_CURRENT_LINE_COMMAND: String = "\r${Ansi.Csi.Codes.Erase.CursorToLineEnd.toFullEscapeCode()}"

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
     * Waits for the currently requested or active render pass to finish executing if one is in progress.
     *
     * If a render pass was triggered immediately prior to this call (e.g., via a [LiveVar] update or
     * [requestRerender][Section.requestRerender] call), this method guarantees that the render pass has completed
     * before resuming.
     *
     * If no render pass is active or queued, this method returns almost immediately.
     *
     * It is not expected that most end users will ever need this, but it can be very useful for tests!
     */
    suspend fun awaitActiveRender() = section.awaitActiveRender()

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
class MainRenderScope(
    renderer: Renderer<MainRenderScope>, private val provideTerminalSize: () -> TerminalSize
) : RenderScope(renderer) {
    object Lifecycle : ConcurrentScopedData.Lifecycle {
        override val parent = Section.Lifecycle
    }

    val width get() = provideTerminalSize().width
    val height get() = provideTerminalSize().height
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

    private val supervisorJob = Job()

    /**
     * A [CoroutineScope] whose lifecycle is tied to this section.
     *
     * It can be useful for various components to use this to launch their own coroutines, knowing they'll be
     * automatically cancelled after the section goes out of scope.
     */
    val coroutineScope = CoroutineScope(KotterDispatchers.IO + supervisorJob)

    private val terminalSizeState: StateFlow<TerminalSize> by lazy {
        renderer.session.terminal.events.sizeChanged
            .onEach { requestRerender() }
            .stateIn(
                coroutineScope,
                started = SharingStarted.Eagerly, // No need for Lazily here -- `by lazy` already does that
                initialValue = renderer.session.terminal.size,
            )
    }

    internal val renderer = Renderer(session) { MainRenderScope(it, provideTerminalSize = { terminalSizeState.value }) }
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

    // See docs for `RunScope.awaitActiveRender`, where the call is public
    // Although the section provides the functionality, it only ever makes sense to call this from inside a run block.
    // If you call it from a render pass, you will end up soft-locked!
    internal suspend fun awaitActiveRender() {
        renderLock.withLock {
            // No-op. However, if someone called `requestRerender` before us, we want to make 100% sure that that
            // finished and enqueued a render request before we continue
        }

        withContext(KotterDispatchers.Render) {
            // No-op again. If a render is in progress, this waits until it finishes. This works because
            // KotterDispatchers.Render is guaranteed to be a sequential dispatcher.
        }
    }

    /**
     * Manage a list of terminal commands, along with an operation to insert implicit newlines into it that gets cached.
     *
     * Essentially, we need to remember what we just rendered last time so we know how to erase it. And since
     * calculating newlines isn't free and generates extra allocations, we want to cache it.
     *
     * However, in very rare cases (when a user is resizing the window), the width value may have changed since the
     * last render took place. So we keep enough information in here that we can invalidate the cache if we need to.
     */
    private class CommandsCache(commands: List<TerminalCommand>, private val textMetrics: TextMetrics) {
        private var lastCommandsRendered = commands.toList() // Make a copy, it's ours now
        private var lastCommandsRenderedWithNewlines: List<TerminalCommand> = emptyList()
        private var lastWidth = -1

        fun withNewlines(width: Int): List<TerminalCommand> {
            if (lastWidth != width) {
                lastCommandsRenderedWithNewlines = lastCommandsRendered.withImplicitNewlines(textMetrics, width)
                lastWidth = width
            }
            return lastCommandsRenderedWithNewlines
        }
    }
    private var commandsCache: CommandsCache? = null

    private fun renderOnceAsync(): Job {
        return CoroutineScope(KotterDispatchers.Render).launch {
            session.data.start(MainRenderScope.Lifecycle)
            // Make sure run logic doesn't modify values while we're in the middle of rendering
            session.data.lock.write {
                renderLock.withLock { renderRequested = false }

                // It's possible the width will get recalculated under us -- at which point, we sadly need to throw out
                // our work and rerender, as previous autowrapping locations are invalided. This should be very rare
                // though!
                var textRendered = false
                while (!textRendered) {
                    val currTerminalSize = session.terminal.size

                    val clearBlockCommand = buildString {
                        commandsCache?.withNewlines(currTerminalSize.width)?.takeIf { it.isNotEmpty() }
                            ?.let { previouslyRenderedCommands ->
                                // To clear an existing block of 'n' lines, completely delete all but one of them, and then
                                // delete the last one down to the beginning (in other words, don't consume the \n of the
                                // previous line)
                                val numLinesToErase = min(
                                    previouslyRenderedCommands.count { it is NewlineCommand } + 1,
                                    currTerminalSize.height)
                                for (i in 0 until numLinesToErase) {
                                    append(WIPE_CURRENT_LINE_COMMAND)
                                    if (i < numLinesToErase - 1) {
                                        append(Ansi.Csi.Codes.Cursor.MoveToPrevLine.toFullEscapeCode())
                                    }
                                }
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

                    val commandsCache = CommandsCache(renderer.commands, session.textMetrics)
                    val textToRender = clearBlockCommand +
                            asideTextBuilder.toString() +
                            commandsCache.withNewlines(currTerminalSize.width)
                                .toText(currTerminalSize.height)

                    // Ideally, the terminal size hasn't changed since we started this render loop. If so, we must
                    // discard our results and try again.
                    //
                    // This check may be overkill! We saw the issue briefly during when the feature was first being
                    // developed, but that might have been due to other bugs that have since been fixed, as we haven't
                    // seen it recently. However, leaving this check in can protect against us issuing commands that
                    // would definitely be stale and doesn't seem to hurt (besides additional complexity that is very
                    // hard to test in the wild).
                    //
                    // For local testing, we added "&& (0..1).random() == 0" to the check, to at least make sure
                    // rendering still works even if the check fails frequently.
                    if (currTerminalSize == session.terminal.size) {
                        session.terminal.write(textToRender)
                        this@Section.commandsCache = commandsCache
                        textRendered = true
                   }
                }

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
            } catch (_: CancellationException) {
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

        supervisorJob.cancel()
        runBlocking { supervisorJob.join() }

        CoroutineScope(KotterDispatchers.Render).launch { allRendersFinished.complete(Unit) }
        runBlocking { allRendersFinished.await() }

        if (!renderer.commands.finalTextCommandIsNewline) {
            session.terminal.write("\n")
        }

        session.data.stop(Lifecycle)

        deferredException?.let { throw it }
    }
}
