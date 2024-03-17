package com.varabyte.kotterx.test.runtime

import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotter.runtime.internal.text.*

/**
 * Replace control characters with their escaped representation.
 *
 * This is useful for debugging purposes, as it allows you to see the actual control characters in a string. It can
 * help you understand why you're getting a `"test string" does not match "test string"` error, for example -- one of
 * those strings might have a control character that's not visible.
 */
fun String.replaceControlCharacters(): String {
    return this.replace(Ansi.CtrlChars.ESC.toString(), "\\e").replace(" ", "·")
}

fun Iterable<String>.replaceControlCharacters(): List<String> {
    return this.map { it.replaceControlCharacters() }
}


/**
 * Strip all ANSI formatting from a string.
 *
 * This can make it easy to assert the output of some Kotter rendering pass without worrying about marking up the
 * expected output.
 */
fun String.stripFormatting(): String {
    val textPtr = TextPtr(this)

    return buildString {
        while (textPtr.remainingLength > 0) {
            when (val c = textPtr.currChar) {
                Ansi.CtrlChars.ESC -> {
                    textPtr.increment()
                    // As a side effect, the `parts` parsing code consumes the text pointer
                    when (textPtr.currChar) {
                        Ansi.EscSeq.CSI -> { textPtr.increment(); Ansi.Csi.Code.parts(textPtr) }
                        Ansi.EscSeq.OSC -> { textPtr.increment(); Ansi.Osc.Code.parts(textPtr) }
                    }
                }
                else -> append(c)
            }
            textPtr.increment()
        }
    }
}

fun Iterable<String>.stripFormatting(): List<String> {
    return this.map { it.stripFormatting() }
}
