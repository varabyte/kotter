package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.core.KonsoleBlockState

internal val RESET_COMMAND = object : AnsiCsiCommand(AnsiCodes.Csi.Codes.Sgr.RESET) {
    override fun updateState(state: KonsoleBlockState) {
        state.clear()
    }
}
