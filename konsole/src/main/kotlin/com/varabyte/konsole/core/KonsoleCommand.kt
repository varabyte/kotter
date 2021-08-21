package com.varabyte.konsole.core

import com.varabyte.konsole.core.block.KonsoleBlockState
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

interface KonsoleCommand {
    fun applyTo(textArea: MutableKonsoleTextArea)
    fun updateState(state: KonsoleBlockState) {}
}

val NoOpCommand = object : KonsoleCommand {
    override fun applyTo(textArea: MutableKonsoleTextArea) = Unit
    override fun updateState(state: KonsoleBlockState) = Unit
}