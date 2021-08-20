package com.varabyte.konsole.core

import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

class KonsoleBlock {
    private val textArea = MutableKonsoleTextArea()

    internal fun applyCommand(command: KonsoleCommand) {
        command.applyTo(textArea)
    }

    override fun toString(): String {
        return textArea.toString()
    }
}