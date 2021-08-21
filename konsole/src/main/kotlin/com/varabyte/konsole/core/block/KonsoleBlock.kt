package com.varabyte.konsole.core.block

import com.varabyte.konsole.core.KonsoleCommand
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea
import com.varabyte.konsole.terminal.TerminalIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService

class KonsoleBlock internal constructor(
    private val executor: ExecutorService,
    private val terminalIO: TerminalIO,
    private val block: KonsoleScope.() -> Unit) {
    private val textArea = MutableKonsoleTextArea()

    val userInput = terminalIO.read()

    internal fun applyCommand(command: KonsoleCommand) {
        command.applyTo(textArea)
    }

    fun runOnce() {
        executor.submit {
            textArea.clear()
            KonsoleScope(this).block()
            terminalIO.write(textArea.toString())
        }.get()
    }

    fun runUntilFinished(block: suspend BackgroundWorkScope.() -> Unit) {
        runOnce()
        val job = CoroutineScope(Dispatchers.Default).launch {
            val scope = BackgroundWorkScope(
                rerenderRequested = {
                    runOnce()
                }
            )
            scope.block()
            if (scope.rerenderOnFinished) {
                runOnce()
            }
        }

        runBlocking { job.join() }
    }
}