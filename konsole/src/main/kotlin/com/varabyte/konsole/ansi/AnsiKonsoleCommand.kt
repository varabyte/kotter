package com.varabyte.konsole.ansi

import com.varabyte.konsole.core.KonsoleCommand
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

open class AnsiKonsoleCommand(private val ansiCode: String) : KonsoleCommand {
    final override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(ansiCode)
    }

    override fun toString() = ansiCode.removePrefix(AnsiCodes.ESC)
}