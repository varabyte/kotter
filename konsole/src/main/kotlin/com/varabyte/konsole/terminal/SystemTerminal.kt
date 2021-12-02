package com.varabyte.konsole.terminal

import com.varabyte.konsole.runtime.terminal.Terminal
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream

/**
 * A class which interacts directly with the underlying system terminal, e.g. println
 */
class SystemTerminal : Terminal {
    private var previousCursorSetting: InfoCmp.Capability
    private val previousOut = System.out
    private val previousErr = System.err
    private val terminal = TerminalBuilder.builder()
        .system(true)
        // According to https://github.com/jline/jline3#jansi-vs-jna, both of these are equally valid libraries but
        // jansi is a bit smaller. Since we're not doing anything particularly complex with Konsole, we choose jansi.
        .jansi(true).jna(false)
        // Don't use JLine's virtual terminal - use ours! Because this is false, this builder will throw an exception
        // if the current terminal environment doesn't support standards we expect to run Konsole on top of. We can
        // listen for that exception to either halt the problem or start up a virtual terminal instead.
        .dumb(false)
        .build().apply {
            // Swallow keypresses - instead, Konsole will handle them
            enterRawMode()

            val disabledPrintStream = PrintStream(object : OutputStream() {
                override fun write(b: Int) = Unit
            })

            // Disable printlns, as they allow users to screw with assumptions that Konsole makes
            System.setOut(disabledPrintStream)
            System.setErr(disabledPrintStream)

            // Hide the cursor; we'll handle it ourselves
            val restoreCursorCapabilities = listOf(
                InfoCmp.Capability.cursor_normal,
                InfoCmp.Capability.cursor_visible,
            )
            previousCursorSetting = restoreCursorCapabilities
                .firstOrNull { c -> getBooleanCapability(c) } ?: InfoCmp.Capability.cursor_normal
            puts(InfoCmp.Capability.cursor_invisible)
        }

    override val width: Int
        get() = terminal.width

    override fun write(text: String) {
        terminal.writer().print(text)
        terminal.writer().flush()
    }

    private val charFlow: Flow<Int> by lazy {
        flow {
            var quit = false
            val context = currentCoroutineContext()
            while (!quit && context.isActive) {
                try {
                    val c = terminal.reader().read(16)
                    if (c >= 0) {
                        emit(c)
                    }
                    else {
                        quit = (c == -1)
                    }
                } catch (ex: IOException) {
                    break
                }
            }
        }
    }

    override fun read(): Flow<Int> = charFlow

    override fun close() {
        terminal.puts(previousCursorSetting)
        terminal.flush()
        terminal.close()

        System.setOut(previousOut)
        System.setErr(previousErr)
    }
}