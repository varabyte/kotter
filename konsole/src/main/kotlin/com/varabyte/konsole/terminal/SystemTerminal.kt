package com.varabyte.konsole.terminal

import com.varabyte.konsole.runtime.terminal.Terminal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.io.IOException

/**
 * A class which interacts directly with the underlying system terminal, e.g. println
 */
class SystemTerminal : Terminal {
    private var previousCursorSetting: InfoCmp.Capability
    private val terminal = TerminalBuilder.builder()
        .system(true)
        // Don't use JLine's virtual terminal - use ours! Because this is false, this builder will throw an exception
        // if the current terminal environment doesn't support standards we expect to run Konsole on top of. We can
        // listen for that exception to either halt the problem or start up a virtual terminal instead.
        .dumb(false)
        .build().apply {
            // Swallow keypresses - instead, Konsole will handle them
            enterRawMode()

            // Hide the cursor; we'll handle it ourselves
            val restoreCursorCapabilities = listOf(
                InfoCmp.Capability.cursor_normal,
                InfoCmp.Capability.cursor_visible,
            )
            previousCursorSetting = restoreCursorCapabilities
                .firstOrNull { c -> getBooleanCapability(c) } ?: InfoCmp.Capability.cursor_normal
            puts(InfoCmp.Capability.cursor_invisible)
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

    override fun close() {
        terminal.puts(previousCursorSetting)
        terminal.flush()

        terminal.close()
    }
}