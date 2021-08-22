package com.varabyte.konsole.core

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

    private val textArea = MutableKonsoleTextArea()

    val userInput = terminal.read()

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
            textArea.clear()
            KonsoleScope(this).block()
            terminal.write(textArea.toString())
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

    fun runOnce() {
        activate {
            renderOnce()
        }
    }

    fun runUntilFinished(block: suspend RunUntilScope.() -> Unit) {
        activate {
            activeReference.set(this)
            renderOnce()
            val job = CoroutineScope(Dispatchers.Default).launch {
                val scope = RunUntilScope(
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

class RunUntilScope(
    private val rerenderRequested: () -> Unit
) {
    private val waitLatch = CountDownLatch(1)
    fun rerender() = rerenderRequested()
    fun waitForSignal() { waitLatch.await() }
    fun signal() { waitLatch.countDown() }
}

fun KonsoleBlock.runUntilSignal(block: suspend RunUntilScope.() -> Unit) {
    runUntilFinished {
        block()
        waitForSignal()
    }
}

// TODO: fun KonsoleBlock.runUntilTextEntered(block: suspend RunUntilScope.() -> Unit) { ... }
