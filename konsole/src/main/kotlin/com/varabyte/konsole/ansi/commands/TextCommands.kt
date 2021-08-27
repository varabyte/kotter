package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.core.KonsoleCommand
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

private class CharCommand(private val char: Char) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(char)
    }
}

private class TextCommand(private val text: String) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(text)
    }
}

private val NewlineCommand = CharCommand('\n')

fun KonsoleScope.text(text: String) {
    applyCommand(TextCommand(text))
}

fun KonsoleScope.text(c: Char) {
    applyCommand(CharCommand(c))
}

fun KonsoleScope.textLine(text: String) {
    applyCommand(TextCommand(text))
    textLine()
}

fun KonsoleScope.textLine(c: Char) {
    applyCommand(CharCommand(c))
    textLine()
}

fun KonsoleScope.textLine() {
    applyCommand(NewlineCommand)
}

/**
 * Create a "paragraph" for text.
 *
 * This is a convenience function for wrapping a block with newlines above and below it, which is a common enough
 * pattern that it's nice to shorten it.
 */
fun KonsoleScope.p(block: KonsoleScope.() -> Unit) {
    textLine()
    textLine()
    block()
    textLine()
}