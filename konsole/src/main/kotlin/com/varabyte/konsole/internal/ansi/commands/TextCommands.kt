package com.varabyte.konsole.internal.ansi.commands

import com.varabyte.konsole.internal.KonsoleCommand
import com.varabyte.konsole.internal.MutableKonsoleTextArea

internal class CharCommand(private val char: Char) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(char)
    }
}

internal class TextCommand(private val text: String) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(text)
    }
}

internal val NewlineCommand = CharCommand('\n')