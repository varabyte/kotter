package com.varabyte.konsole.runtime.internal

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.internal.text.MutableTextArea

internal interface KonsoleCommand {
    fun applyTo(textArea: MutableTextArea)
    fun updateState(state: KonsoleState) {}
}