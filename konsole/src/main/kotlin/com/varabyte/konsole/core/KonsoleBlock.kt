package com.varabyte.konsole.core

import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

class KonsoleBlock {
    private val textArea = MutableKonsoleTextArea()

    fun applyCommand(command: KonsoleCommand) {
        command.applyTo(textArea)
    }
}