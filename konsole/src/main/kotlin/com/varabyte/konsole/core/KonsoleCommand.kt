package com.varabyte.konsole.core

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

interface KonsoleCommand {
    fun applyTo(textArea: MutableKonsoleTextArea)
    fun updateState(state: KonsoleState) {}
}

val NoOpCommand = object : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) = Unit
    override fun updateState(state: KonsoleState) = Unit
}

open class AnsiKonsoleCommand(private val ansiCode: String) : KonsoleCommand {
    final override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(ansiCode)
    }

    override fun toString() = ansiCode.removePrefix(AnsiCodes.ESC)
}