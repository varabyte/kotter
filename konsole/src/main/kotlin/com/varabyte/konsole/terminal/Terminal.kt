package com.varabyte.konsole.terminal

import com.varabyte.konsole.KonsoleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.jline.terminal.TerminalBuilder
import java.io.IOException

val DefaultTerminalIO by lazy { KonsoleSettings.provideTerminal() }

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface Terminal {
    fun write(text: String)
    fun read(): Flow<Int>
}

/**
 * A class which interacts directly with the underlying system terminal, e.g. println
 */
class SystemTerminal : Terminal {
    val terminal = TerminalBuilder.builder()
        .system(true)
        .dumb(false)
        .build().apply {
            enterRawMode()
        }

    override fun write(text: String) {
        terminal.writer().print(text)
        terminal.writer().flush()
    }

    override fun read(): Flow<Int> {
        return callbackFlow {
            while (true) {
                try {
                    trySend(terminal.reader().read())
                } catch (ex: IOException) {
                    break
                }
            }
        }
    }
}
