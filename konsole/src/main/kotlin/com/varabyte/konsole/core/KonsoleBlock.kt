package com.varabyte.konsole.core

import com.varabyte.konsole.ansi.Ansi.Csi
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea
import com.varabyte.konsole.terminal.Terminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference


class KonsoleBlock internal constructor(
    private val executor: ExecutorService,
    private val terminal: Terminal,
    private val block: KonsoleScope.() -> Unit) {

    companion object {
        private val activeReference = AtomicReference<KonsoleBlock?>(null)
        internal val active get() = activeReference.get()
    }

    class RunScope(
        private val rerenderRequested: () -> Unit
    ) {
        private val waitLatch = CountDownLatch(1)
        fun rerender() = rerenderRequested()
        fun waitForSignal() { waitLatch.await() }
        fun signal() { waitLatch.countDown() }
    }

    private val textArea = MutableKonsoleTextArea()

    internal fun applyCommand(command: KonsoleCommand) {
        command.applyTo(textArea)
    }

    internal fun requestRerender() {
        // TODO: If multiple requestRerenders come in, batch
        // TODO: Test if this block is done running
        renderOnce()
    }

    private fun renderOnceAsync(): Future<*> {
        return executor.submit {

            val clearBlockCommand = buildString {
                if (!textArea.isEmpty()) {
                    // To clear an existing block of 'n' lines, completely delete all but one of them, and then delete the
                    // last one down to the beginning (in other words, don't consume the \n of the previous line)
                    for (i in 0 until textArea.numLines) {
                        append('\r')
                        append(Csi.Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode())
                        if (i < textArea.numLines - 1) {
                            append(Csi.Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode())
                        }
                    }
                }
            }

            textArea.clear()
            KonsoleScope(this).block()
            // Send the whole set of instructions through "write" at once so the clear and updates are processed
            // in one pass.
            terminal.write(clearBlockCommand + textArea.toString())
        }
    }
    private fun renderOnce() {
        renderOnceAsync().get()
    }

    /** Mark this block as active. Only one block can be active at a time! */
    private fun activate(block: () -> Unit) {
        if (!activeReference.compareAndSet(null, this)) {
            throw IllegalStateException("Cannot run this Konsole block while another block is already running")
        }
        block()
        activeReference.set(null)
    }

    fun run(block: (suspend RunScope.() -> Unit)? = null) {
        activate {
            activeReference.set(this)
            renderOnce()
            if (block != null) {
                val job = CoroutineScope(Dispatchers.Default).launch {
                    val scope = RunScope(
                        rerenderRequested = {
                            renderOnce()
                        }
                    )
                    scope.block()
                }

                runBlocking { job.join() }
            }
        }
    }
}

fun KonsoleBlock.runUntilSignal(block: suspend KonsoleBlock.RunScope.() -> Unit) {
    run {
        block()
        waitForSignal()
    }
}

// TODO: fun KonsoleBlock.runUntilTextEntered(block: suspend RunUntilScope.() -> Unit) { ... }