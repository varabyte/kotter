package com.varabyte.kotter.terminal

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.truthish.assertThat
import kotlinx.coroutines.flow.Flow
import org.junit.Test

/**
 * A fake terminal, built for tests, which stores data written to it in memory that can then be queried.
 */
class TestTerminal : Terminal {
    var closed = false
        private set

    private val _buffer = StringBuffer()

    val buffer get() = _buffer.toString()

    override fun write(text: String) {
        assertNotClosed()
        _buffer.append(text)
    }

    override fun read(): Flow<Int> {
        assertNotClosed()
        TODO("Not yet implemented")
    }

    override fun clear() {
        assertNotClosed()
        _buffer.delete(0, _buffer.length)
    }

    override fun close() {
        assertNotClosed()
        closed = true
    }

    private fun assertNotClosed() {
        check(!closed) { "Tried to modify this terminal after it was closed" }
    }
}

class TerminalTests {
    @Test
    fun `terminal always ends with a newline and reset code`() = testSession { terminal ->
        assertThat(terminal.buffer).isEmpty()
        section {}.run()

        assertThat(terminal.buffer).isEqualTo("\n" + Ansi.Csi.Codes.Sgr.RESET.toFullEscapeCode())
    }
}
