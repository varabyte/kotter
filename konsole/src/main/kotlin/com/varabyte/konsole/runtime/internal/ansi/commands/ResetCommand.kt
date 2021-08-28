package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.internal.ansi.Ansi

internal val RESET_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.RESET) {
    override fun updateState(state: KonsoleState) {
        state.clear()
    }
}