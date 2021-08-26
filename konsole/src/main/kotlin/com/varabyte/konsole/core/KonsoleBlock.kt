package com.varabyte.konsole.core

import com.varabyte.konsole.ansi.Ansi
import com.varabyte.konsole.core.input.CharKey
import com.varabyte.konsole.core.input.Key
import com.varabyte.konsole.core.input.Keys
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea
import com.varabyte.konsole.terminal.Terminal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import net.jcip.annotations.GuardedBy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class KonsoleBlock internal constructor(
    private val executor: ExecutorService,
    private val terminal: Terminal,
    internal val data: KonsoleData,
    private val block: KonsoleScope.() -> Unit) {
    object Lifecycle : KonsoleData.Lifecycle

    companion object {
        private val activeReference = AtomicReference<KonsoleBlock?>(null)
        internal val active get() = activeReference.get()
    }

    class RunScope(
        private val rerenderRequested: () -> Unit
    ) {
        private val waitLatch = CountDownLatch(1)
        fun rerender() = rerenderRequested()
        fun waitForSignal() {
            waitLatch.await()
        }

        fun signal() {
            waitLatch.countDown()
        }
    }

    private val textArea = MutableKonsoleTextArea()

    private val renderLock = ReentrantLock()
    @GuardedBy("renderLock")
    private var renderRequested = false

    internal val keyFlow: Flow<Key> by lazy {
        val escSeq = StringBuilder()
        terminal.read().mapNotNull { byte ->
            val c = byte.toChar()
            when {
                escSeq.isNotEmpty() -> {
                    escSeq.append(c)
                    val code = Ansi.EscSeq.toCode(escSeq)
                    if (code != null) {
                        escSeq.clear()
                        when(code) {
                            Ansi.Csi.Codes.Keys.LEFT -> Keys.LEFT
                            Ansi.Csi.Codes.Keys.RIGHT -> Keys.RIGHT
                            Ansi.Csi.Codes.Keys.HOME -> Keys.HOME
                            Ansi.Csi.Codes.Keys.END -> Keys.END
                            Ansi.Csi.Codes.Keys.DELETE -> Keys.DELETE
                            else -> null
                        }
                    }
                    else {
                        null
                    }
                }
                else -> {
                    when(c) {
                        Ansi.CtrlChars.ESC -> { escSeq.append(c); null }
                        Ansi.CtrlChars.ENTER -> Keys.ENTER
                        Ansi.CtrlChars.BACKSPACE -> Keys.BACKSPACE
                        else -> if (!c.isISOControl()) CharKey(c) else null
                    }
                }
            }
        }
    }

    internal fun applyCommand(command: KonsoleCommand) {
        command.applyTo(textArea)
    }

    internal fun requestRerender() {
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
        return CoroutineScope(executor.asCoroutineDispatcher()).launch {
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

            textArea.clear()
            KonsoleScope(self).block()
            // Send the whole set of instructions through "write" at once so the clear and updates are processed
            // in one pass.
            terminal.write(clearBlockCommand + textArea.toString())
        }
    }
    private fun renderOnce() = runBlocking {
        renderOnceAsync().join()
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
                    val scope = RunScope(rerenderRequested = { requestRerender() })
                    scope.block()
                }

                runBlocking { job.join() }
            }
            data.dispose(Lifecycle)
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