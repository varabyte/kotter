package com.varabyte.konsole.internal

import com.varabyte.konsole.core.KonsoleState

internal interface KonsoleCommand {
    fun applyTo(textArea: MutableKonsoleTextArea)
    fun updateState(state: KonsoleState) {}
}