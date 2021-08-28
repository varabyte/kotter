package com.varabyte.konsole.internal.ansi.commands

import com.varabyte.konsole.core.KonsoleState
import com.varabyte.konsole.internal.ansi.Ansi

internal val RESET_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.RESET) {
    override fun updateState(state: KonsoleState) {
        state.clear()
    }
}