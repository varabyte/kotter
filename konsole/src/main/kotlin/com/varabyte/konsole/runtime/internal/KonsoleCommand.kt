package com.varabyte.konsole.runtime.internal

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.internal.text.MutableTextArea

internal open class KonsoleCommand(val text: String) {
    open fun updateState(state: KonsoleState) {}
}