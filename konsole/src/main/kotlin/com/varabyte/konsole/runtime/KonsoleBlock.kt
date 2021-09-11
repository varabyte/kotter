package com.varabyte.konsole.runtime

import com.varabyte.konsole.runtime.concurrent.ConcurrentScopedData
import com.varabyte.konsole.runtime.concurrent.createKey
import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.RESET_COMMAND
import com.varabyte.konsole.runtime.internal.text.MutableTextArea
import com.varabyte.konsole.runtime.text.TextArea
import kotlinx.coroutines.*
import net.jcip.annotations.GuardedBy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal val ActiveBlockKey = KonsoleBlock.Lifecycle.createKey<KonsoleBlock>()

class KonsoleBlock internal constructor(
    internal val app: KonsoleApp,
    private val block: RenderScope.() -> Unit) {
    /**
     * A moderately long lifecycle that lives as long as the block is running.
     *
     * This lifecycle can be used for storing data relevant to the current block only.
     */
    object Lifecycle : ConcurrentScopedData.Lifecycle

    /**
     * A scope associated with the [run] function.
     *
     * While the lifecycle is probably *almost* the same as the Konsole block's lifecycle, it is a little shorter, and
     * this matters because some data may need to be cleaned up before the block is actually finished.
     */
    class RunScope(
        internal val block: KonsoleBlock,
        private val scope: CoroutineScope,
    ) {
        object Lifecycle : ConcurrentScopedData.Lifecycle

        /**
         * Data store for this app.
         *
         * It is exposed directly and publicly here so methods extending the RunScope can use it.
         */
        val data = block.app.data

        private val waitLatch = CountDownLatch(1)
        /** Forcefully exit this runscope early, even if it's still in progress */
        internal fun abort() { scope.cancel() }
        fun rerender() = block.requestRerender()
        fun waitForSignal() = waitLatch.await()
        fun signal() = waitLatch.countDown()
    }

    private val _textArea = MutableTextArea()
    val textArea: TextArea = _textArea

    private val renderLock = ReentrantLock()
    @GuardedBy("renderLock")
    private var renderRequested = false

    /**
     * A list of callbacks to trigger right before the block exits.
     *
     * It is not expected for a user to add more than one, but internal components might themselves add listeners
     * behind the scenes to clean up their state.
     */
    private var onFinishing = mutableListOf<() -> Unit>()
    private var consumed = AtomicBoolean(false)

    init {
        app.data.start(Lifecycle)
    }

    /** Append this command to the end of this block's text area */
    internal fun appendCommand(command: KonsoleCommand) {
        _textArea.appendCommand(command)
    }

    /**
     * Let the block know we want to rerender an additional frame.
     *
     * This will not enqueue a render if one is already queued up.
     */
    fun requestRerender() {
        if (app.activeBlock != this) return

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
        val self = this
        return CoroutineScope(app.executor.asCoroutineDispatcher()).launch {
            renderLock.withLock { renderRequested = false }

            val clearBlockCommand = buildString {
                if (!textArea.isEmpty()) {
                    // To clear an existing block of 'n' lines, completely delete all but one of them, and then delete the
                    // last one down to the beginning (in other words, don't consume the \n of the previous line)
                    for (i in 0 until textArea.numLines) {
                        append('\r')
                        append(Ansi.Csi.Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode())
                        if (i < textArea.numLines - 1) {
                            append(Ansi.Csi.Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode())
                        }
                    }
                }
            }

            _textArea.clear()
            app.data.start(RenderScope.Lifecycle)
            RenderScope(self).apply {
                // Make sure we clear all state as we exit this block. This ensures that repaint passes don't carry
                // state leftover from its end back to the beginning.
                block()
                _textArea.appendCommand(RESET_COMMAND)
            }
            app.data.stop(RenderScope.Lifecycle)

            if (textArea.toRawText().lastOrNull() != '\n') {
                _textArea.appendCommand(NEWLINE_COMMAND)
            }

            // Send the whole set of instructions through "write" at once so the clear and updates are processed
            // in one pass.
            app.terminal.write(clearBlockCommand + textArea.toString())
        }
    }
    private fun renderOnce() = runBlocking {
        renderOnceAsync().join()
    }

    /**
     * Add a callback which will get triggered after this block has just about finished running and is about to shut
     * down.
     *
     * This is a good opportunity to change any values back to some initial state if necessary (such as a blinking
     * cursor). Changes made in `onFinishing` may potentially kick off one final render pass.
     */
    fun onFinishing(block: () -> Unit): KonsoleBlock {
        require(app.data.isActive(Lifecycle))
        onFinishing.add(block)

        return this
    }

    fun run(block: (suspend RunScope.() -> Unit)? = null) {
        if (consumed.get()) {
            throw IllegalStateException("Cannot run a Konsole block that was previously run")
        }
        consumed.set(true)

        // Note: The data we're adding here will be removed by the dispose call below
        if (!app.data.tryPut(ActiveBlockKey) { this }) {
            throw IllegalStateException("Cannot run this Konsole block while another block is already running")
        }

        app.data.start(RunScope.Lifecycle)
        renderOnce()
        if (block != null) {
            val self = this
            val job = CoroutineScope(Dispatchers.Default).launch {
                val scope = RunScope(self, this)
                scope.block()
            }

            runBlocking { job.join() }
        }
        app.data.stop(RunScope.Lifecycle)
        onFinishing.forEach { it() }

        // Our run block is done, let's just wait until any remaining renders are finished. We can do this by adding
        // ourselves to the end of the line and waiting to get through.
        val allRendersFinishedLatch = CountDownLatch(1)
        app.executor.submit { allRendersFinishedLatch.countDown() }
        allRendersFinishedLatch.await()

        app.data.stop(Lifecycle)
    }
}