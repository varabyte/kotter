package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.core.KonsoleCommand
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

class CharCommand(private val char: Char) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(char)
    }
}

class TextCommand(private val text: String) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(text)
    }
}

val NewlineCommand = CharCommand('\n')