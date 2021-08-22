package com.varabyte.konsole.core

import com.varabyte.konsole.core.internal.MutableKonsoleTextArea
import com.varabyte.konsole.terminal.TerminalIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference


class KonsoleBlock internal constructor(
    private val executor: ExecutorService,
    private val terminalIO: TerminalIO,
    private val block: KonsoleScope.() -> Unit) {

    companion object {
        private val activeReference = AtomicReference<KonsoleBlock?>(null)
        internal val active get() = activeReference.get()
    }

    private val textArea = MutableKonsoleTextArea()

    val userInput = terminalIO.read()

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
            terminalIO.write(textArea.toString())
        }
    }
    private fun renderOnce() {
        renderOnceAsync().get()
    }

    fun runOnce() {
        activeReference.set(this)
        renderOnce()
        activeReference.set(null)
    }

    fun runUntilFinished(block: suspend RunUntilScope.() -> Unit) {
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
        activeReference.set(null)
    }
}

class RunUntilScope(
    private val rerenderRequested: () -> Unit
) {
    fun rerender() = rerenderRequested()
}