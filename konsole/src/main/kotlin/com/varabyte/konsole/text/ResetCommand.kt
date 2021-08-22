package com.varabyte.konsole.text

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.ansi.AnsiCsiKonsoleCommand
import com.varabyte.konsole.core.KonsoleBlockState

internal val RESET_COMMAND = object : AnsiCsiKonsoleCommand(AnsiCodes.Csi.Sgr.RESET) {
    override fun updateState(state: KonsoleBlockState) {
        state.clear()
    }
}
