package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.text.MutableTextArea

internal class CharCommand(char: Char) : KonsoleCommand(char.toString()) {
    init {
        require(char != '\n') { "Newlines should be represented by the NEWLINE_COMMAND" }
    }
}
internal class TextCommand(text: CharSequence) : KonsoleCommand(text.toString()) {
    init {
        require(!text.contains("\n")) { "Newlines should be represented by the NEWLINE_COMMAND" }
    }
}
internal val NEWLINE_COMMAND = KonsoleCommand("\n")