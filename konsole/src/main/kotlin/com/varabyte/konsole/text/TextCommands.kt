package com.varabyte.konsole.text

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
    block.applyCommand(TextCommand(text))
}

fun KonsoleScope.textLine(text: String) {
    block.applyCommand(TextCommand(text))
    newLine()
}

fun KonsoleScope.newLine() {
    block.applyCommand(NewlineCommand)
}

