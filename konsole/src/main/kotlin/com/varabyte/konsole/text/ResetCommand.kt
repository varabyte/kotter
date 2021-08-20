package com.varabyte.konsole.text

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.core.AnsiKonsoleCommand
import com.varabyte.konsole.core.KonsoleState
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

internal val RESET_COMMAND = object : AnsiKonsoleCommand(AnsiCodes.RESET) {
    override fun updateState(state: KonsoleState) {
        state.clear()
    }
}
