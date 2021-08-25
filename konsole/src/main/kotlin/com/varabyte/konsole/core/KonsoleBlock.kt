package com.varabyte.konsole.core

import com.varabyte.konsole.ansi.Ansi
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea
import com.varabyte.konsole.terminal.Terminal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


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

    /**
     * A collection of values that might be accessed both within a konsole block AND a run block at the same time, so
     * their access should be guarded behind a lock.
     */
    private class ThreadSafeData(private val terminal: Terminal, private val requestRerender: () -> Unit) {
        /** State needed to support the special `$input` property */
        class InputState {
            var text = StringBuilder()
            var index = 0
        }

        private val lock = ReentrantLock()

        private lateinit var terminalReaderJob: Job
        private lateinit var _inputState: InputState

        fun <T> inputState(withLock: InputState.() -> T): T {
            return lock.withLock {
                if (!::_inputState.isInitialized) {
                    _inputState = InputState()
                    startReading()
                }
                _inputState.withLock()
            }
        }

        private fun startReading() {
            lock.withLock {
                if (!::terminalReaderJob.isInitialized) {
                    terminalReaderJob = CoroutineScope(Dispatchers.IO).launch {
                        val escSeq = StringBuilder()
                        terminal.read().collect { byte ->
                            val c = byte.toChar()
                            when {
                                escSeq.isNotEmpty() -> {
                                    escSeq.append(c)
                                    val code = Ansi.EscSeq.toCode(escSeq)
                                    if (code != null) {
                                        when(code) {
                                            Ansi.Csi.Codes.Keys.LEFT -> {
                                                inputState { index = (index - 1).coerceAtLeast(0) }
                                                requestRerender()
                                            }
                                            Ansi.Csi.Codes.Keys.RIGHT -> {
                                                inputState { index = (index + 1).coerceAtMost(text.lastIndex) }
                                                requestRerender()
                                            }
                                            Ansi.Csi.Codes.Keys.HOME -> {
                                                inputState { index = 0 }
                                                requestRerender()
                                            }
                                            Ansi.Csi.Codes.Keys.END -> {
                                                inputState { index = text.lastIndex }
                                                requestRerender()
                                            }
                                        }
                                        escSeq.clear()
                                    }
                                }
                                else -> when (byte) {
                                    Ansi.CtrlChars.ESC.code -> escSeq.append(c)
                                    Ansi.CtrlChars.ENTER.code -> {
                                        inputState {
                                            text.clear()
                                            index = 0
                                        }
                                        requestRerender()
                                    }
                                    Ansi.CtrlChars.BACKSPACE.code -> {
                                        inputState {
                                            if (index > 0) {
                                                text.deleteAt(index - 1)
                                                index--
                                            }
                                        }
                                        requestRerender()
                                    }
                                    else -> {
                                        inputState {
                                            text.insert(index, c)
                                            index += 1
                                        }
                                        requestRerender()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val threadSafeData = ThreadSafeData(terminal, { requestRerender() })

    val input: String
        get() = threadSafeData.inputState { text.toString() }

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
                        append(Ansi.Csi.Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode())
                        if (i < textArea.numLines - 1) {
                            append(Ansi.Csi.Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode())
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