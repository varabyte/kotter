package com.varabyte.kotter.runtime.internal.text

import kotlin.math.max
import kotlin.math.min

/**
 * A class which points at a character index within a text string, where the index can be incremented and decremented.
 *
 * Note that it's valid to point at the index AFTER the last character, which in this case returns a null terminator
 * character to indicate it. That is, for a String of length one, e.g. "a", then textPtr[0] == 'a' and
 * textPtr[1] == '\0'
 */
class TextPtr(val text: CharSequence, charIndex: Int = 0) {
    var charIndex = 0
        set(value) {
            require(value >= 0 && value <= text.length) { "charIndex value is out of bounds. Expected 0 .. ${text.length}, got $value" }
            field = value
        }

    val currChar get() = text.elementAtOrNull(charIndex) ?: Char.MIN_VALUE
    val remainingLength get() = max(0, text.length - charIndex)

    init {
        this.charIndex = charIndex
    }

    /**
     * Increment or decrement the pointer first (based on [forward]), and then keep moving until
     * [keepMoving] stops returning true.
     */
    private fun movePtr(forward: Boolean, keepMoving: (Char) -> Boolean): Boolean {
        val delta = if (forward) 1 else -1

        var newIndex = charIndex
        do {
            newIndex += delta
            if (newIndex < 0) {
                newIndex = 0
                break
            } else if (newIndex >= text.length) {
                newIndex = text.length
                break
            }
        } while (keepMoving(text[newIndex]))

        if (newIndex != charIndex) {
            charIndex = newIndex
            return true
        }
        return false
    }

    fun increment(): Boolean {
        return movePtr(true) { false }
    }

    fun decrement(): Boolean {
        return movePtr(false) { false }
    }

    fun incrementWhile(whileCondition: (Char) -> Boolean) = movePtr(true, whileCondition)
    fun decrementWhile(whileCondition: (Char) -> Boolean) = movePtr(false, whileCondition)
    fun incrementUntil(whileCondition: (Char) -> Boolean): Boolean {
        return incrementWhile { !whileCondition(it) }
    }

    fun decrementUntil(whileCondition: (Char) -> Boolean): Boolean {
        return decrementWhile { !whileCondition(it) }
    }
}

fun TextPtr.substring(length: Int): String {
    return text.substring(charIndex, min(charIndex + length, text.length))
}

fun TextPtr.readInt(): Int? {
    if (!currChar.isDigit()) return null

    var intValue = 0
    while (true) {
        val digit = currChar.digitToIntOrNull() ?: break
        increment()
        intValue *= 10
        intValue += digit
    }
    return intValue
}