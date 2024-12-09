package com.varabyte.kotter.runtime.terminal.inmemory

import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotter.runtime.internal.text.*
import com.varabyte.kotter.runtime.terminal.*
import com.varabyte.kotter.runtime.terminal.inmemory.InMemoryTerminal.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.min

/**
 * An in-memory terminal implementation.
 *
 * This implementation is useful both for testing and also for generating ANSI commands directly into a [Buffer] (which
 * you can then feed to a real console directly).
 *
 * Using it looks like this:
 *
 * ```kotlin
 * val terminal = InMemoryTerminal()
 *
 * session(terminal) {
 *   /* ... do your Kotter stuff here ... */
 * }
 *
 * println(terminal.buffer)
 * ```
 *
 * You can use the buffer's advanced `toString` method to replace escape characters and spaces with more readable
 * representations:
 *
 * ```kotlin
 * println(terminal.buffer.toString(highlightEscapeChars = true, highlightSpaces = true))
 * // Output: "Hello·\e[31mWorld\e[0m"
 * ```
 */
class InMemoryTerminal : Terminal {
    class Buffer(private val builder: StringBuilder): CharSequence by builder {
        override fun toString(): String {
            return builder.toString()
        }

        fun toString(
            highlightEscapeChars: Boolean = false,
            highlightSpaces: Boolean = false,
        ): String {
            return this.toString()
                .let { if (highlightSpaces) it.replace(" ", "·") else it }
                .let { if (highlightEscapeChars) it.replace(Ansi.CtrlChars.ESC.toString(), "\\e") else it }
        }
    }

    private var closed = false

    private val builder = StringBuilder()
    val buffer = Buffer(builder)

    private val keysFlow = MutableSharedFlow<Int>()

    suspend fun sendKeys(vararg keys: Int) {
        assertNotClosed()
        keys.forEach { keysFlow.emit(it) }
    }

    override val width = TerminalSize.Unbounded.width
    override val height = TerminalSize.Unbounded.height

    override fun write(text: String) {
        assertNotClosed()
        builder.append(text)
    }

    override fun read(): SharedFlow<Int> {
        assertNotClosed()
        return keysFlow.asSharedFlow()
    }

    override fun clear() {
        assertNotClosed()
        builder.clear()
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
suspend fun InMemoryTerminal.sendKey(key: Int) = sendKeys(key)

/** Convenience function for the common case of sending an ANSI code. */
suspend fun InMemoryTerminal.sendCode(code: Ansi.Csi.Code) = type(*code.toFullEscapeCode().toCharArray())

/** Convenience functions for typing characters (instead of sending their underlying codes) */
suspend fun InMemoryTerminal.type(vararg chars: Char) = sendKeys(*chars.map { it.code }.toIntArray())
suspend fun InMemoryTerminal.type(text: String) = type(*text.toCharArray())

/**
 * Press one or more [Key]s.
 *
 * Technically, a Kotter [Key] represents the final transformation of input passed into a terminal, so using them here
 * as the initial input is technically a bit backwards. But for practicality, it's much easier to use this method than
 * understanding the nuances of what sort of input a terminal needs to result in the expected [Key] to get created.
 *
 * This method works by converting the passed in keys into the actual terminal input that will just result in those keys
 * getting created again on the other end.
 */
suspend fun InMemoryTerminal.press(vararg keys: Key) {
    keys.forEach { key ->
        when (key) {
            is CharKey -> sendKey(key.code.code)

            Keys.ENTER -> type(Ansi.CtrlChars.ENTER)
            Keys.ESC -> type(Ansi.CtrlChars.ESC)
            Keys.BACKSPACE -> type(Ansi.CtrlChars.BACKSPACE)
            Keys.DELETE -> type(Ansi.CtrlChars.DELETE)
            Keys.EOF -> type(Ansi.CtrlChars.EOF)
            Keys.TAB -> type(Ansi.CtrlChars.TAB)

            Keys.UP -> sendCode(Ansi.Csi.Codes.Keys.UP)
            Keys.DOWN -> sendCode(Ansi.Csi.Codes.Keys.DOWN)
            Keys.LEFT -> sendCode(Ansi.Csi.Codes.Keys.LEFT)
            Keys.RIGHT -> sendCode(Ansi.Csi.Codes.Keys.RIGHT)

            Keys.HOME -> sendCode(Ansi.Csi.Codes.Keys.HOME)
            Keys.END -> sendCode(Ansi.Csi.Codes.Keys.END)
            Keys.INSERT -> sendCode(Ansi.Csi.Codes.Keys.INSERT)
            Keys.PAGE_UP -> sendCode(Ansi.Csi.Codes.Keys.PG_UP)
            Keys.PAGE_DOWN -> sendCode(Ansi.Csi.Codes.Keys.PG_DOWN)

            else -> error("Unsupported key: $key")
        }
    }
}

/**
 * Convenience method that returns this test terminal's [InMemoryTerminal.buffer] as separate lines.
 */
fun InMemoryTerminal.lines(): List<String> {
    return buffer.split("\n")
}

/**
 * Return the state of the terminal AFTER lines have been erased and repainted.
 */
fun InMemoryTerminal.resolveRerenders(): List<String> {
    val codeEraseToLineEnd = Ansi.Csi.Codes.Erase.CURSOR_TO_LINE_END.toFullEscapeCode()
    val codeMoveToPrevLine = Ansi.Csi.Codes.Cursor.MOVE_TO_PREV_LINE.toFullEscapeCode()

    val resolved = mutableListOf<String>()
    val currLine = StringBuilder()
    var currLineIndex = 0
    val textPtr = TextPtr(buffer.toString())
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
                currLine.deleteRange(currLineIndex, currLineIndex + currLine.length)
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
