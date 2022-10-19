package com.varabyte.kotterx.test.terminal

import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.startsWith
import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.kotterx.test.foundation.testSession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.math.min

/**
 * A fake terminal, built for tests, which stores data written to it in memory that can then be queried.
 */
class TestTerminal : Terminal {
    var closed = false
        private set

    private val _buffer = StringBuffer()
    val buffer get() = _buffer.toString()

    private val keysChannel = Channel<Int>()

    suspend fun sendKeys(vararg keys: Int) {
        keys.forEach { keysChannel.send(it) }
    }

    override fun write(text: String) {
        assertNotClosed()
        _buffer.append(text)
    }

    override fun read(): Flow<Int> {
        assertNotClosed()
        return keysChannel.consumeAsFlow()
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

/** Convenience function for the common case of only sending a single key. */
suspend fun TestTerminal.sendKey(key: Int) = sendKeys(key)

/** Convenience function for the common case of sending an ANSI code. */
suspend fun TestTerminal.sendCode(code: Ansi.Csi.Code) = type(*code.toFullEscapeCode().toCharArray())

/** Convenience functions for typing characters (instead of sending their underlying codes) */
suspend fun TestTerminal.type(vararg chars: Char) = sendKeys(*chars.map { it.code }.toIntArray())
suspend fun TestTerminal.type(text: String) = type(*text.toCharArray())

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

fun TestTerminal.matches(expected: MainRenderScope.() -> Unit): Boolean {
    var matches = false
    val self = this
    testSession { otherTerminal ->
        section {
            this.expected()
        }.run()
        matches = self.resolveRerenders() == otherTerminal.resolveRerenders()
    }

    return matches
}