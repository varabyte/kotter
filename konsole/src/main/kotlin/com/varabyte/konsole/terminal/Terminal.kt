package com.varabyte.konsole.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.jline.terminal.TerminalBuilder
import java.io.IOException

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
        // Don't use JLine's virtual terminal - use ours! Because this is false, this builder will throw an exception
        // if the current terminal environment doesn't support standards we expect to run Konsole on top of. We can
        // listen for that exception to either halt the problem or start up a virtual terminal instead.
        .dumb(false)
        .build().apply {
            // Swallow keypresses - instead, Konsole will handle them
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