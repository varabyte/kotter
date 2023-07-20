package com.varabyte.kotterx.test.runtime

import com.varabyte.kotter.runtime.internal.ansi.*

fun String.replaceControlCharacters(): String {
    return this.replace(Ansi.CtrlChars.ESC.toString(), "\\e").replace(" ", "·")
}

fun Iterable<String>.replaceControlCharacters(): List<String> {
    return this.map { it.replaceControlCharacters() }
}
