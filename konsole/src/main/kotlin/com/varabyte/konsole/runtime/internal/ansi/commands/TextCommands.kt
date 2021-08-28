package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.text.MutableTextArea

internal class CharCommand(private val char: Char) : KonsoleCommand {
    override fun applyTo(textArea: MutableTextArea) {
        textArea.append(char)
    }
}

internal class TextCommand(private val text: String) : KonsoleCommand {
    override fun applyTo(textArea: MutableTextArea) {
        textArea.append(text)
    }
}

internal val NewlineCommand = CharCommand('\n')