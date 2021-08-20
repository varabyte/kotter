package com.varabyte.konsole.core

import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

interface KonsoleCommand {
    fun applyTo(textArea: MutableKonsoleTextArea)
    fun applyTo(state: KonsoleState) {}
}

open class AnsiKonsoleCommand(private val ansiCode: String) : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(ansiCode)
    }
}