package com.varabyte.konsole.text

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.ansi.AnsiKonsoleCommand
import com.varabyte.konsole.core.KonsoleBlockState

internal val RESET_COMMAND = object : AnsiKonsoleCommand(AnsiCodes.RESET) {
    override fun updateState(state: KonsoleBlockState) {
        state.clear()
    }
}
