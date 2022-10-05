package com.varabyte.kotter.terminal

import com.varabyte.kotter.runtime.terminal.Terminal
import kotlinx.coroutines.flow.Flow

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

/**
 * Convenience method that returns this test terminal's [TestTerminal.buffer] as separate lines.
 */
fun TestTerminal.lines(): List<String> {
    return buffer.split("\n")
}

