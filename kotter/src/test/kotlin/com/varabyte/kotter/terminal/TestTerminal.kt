package com.varabyte.kotter.terminal

import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.startsWith
import com.varabyte.kotter.runtime.terminal.Terminal
import kotlinx.coroutines.flow.Flow
import kotlin.math.min

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

/**
 * Return the state of the terminal AFTER lines have been erased and repainted.
 */
fun TestTerminal.resolveRerenders(): List<String> {
    val codeEraseToLineEnd = Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode()
    val codeMoveToPrevLine = Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode()

    val resolved = mutableListOf<String>()
    val currLine = StringBuilder()
    var currLineIndex = 0
    val textPtr = TextPtr(buffer)
    while (textPtr.remainingLength > 0) {
        // Store the current char index so we can check it later. Some handlers might change it, and if so,
        // we should not attempt to modify it further.
        val textPtrCharIndex = textPtr.charIndex
        when {
            textPtr.currChar == '\r' -> {
                currLineIndex = 0
            }
            textPtr.currChar == '\n' -> {
                resolved.add(currLine.toString())
                currLine.clear()
                currLineIndex = 0
            }
            textPtr.startsWith(codeEraseToLineEnd) -> {
                textPtr.charIndex += codeEraseToLineEnd.length
                currLine.delete(currLineIndex, currLine.length)
            }
            textPtr.startsWith(codeMoveToPrevLine) -> {
                textPtr.charIndex += codeMoveToPrevLine.length
                resolved.removeLast()
                currLine.clear()
                resolved.firstOrNull()?.let { currLine.append(it) }
                currLineIndex = min(currLineIndex, currLine.lastIndex)
            }
            else -> {
                currLine.insert(currLineIndex++, textPtr.currChar)
            }
        }

        if (textPtrCharIndex == textPtr.charIndex) {
            textPtr.increment()
        }
    }

    resolved.add(currLine.toString())
    return resolved
}

