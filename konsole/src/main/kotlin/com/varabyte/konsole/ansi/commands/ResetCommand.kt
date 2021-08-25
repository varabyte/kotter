package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.ansi.Ansi
import com.varabyte.konsole.core.KonsoleBlockState

internal val RESET_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.RESET) {
    override fun updateState(state: KonsoleBlockState) {
        state.clear()
    }
}