package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.ansi.Ansi
import com.varabyte.konsole.core.KonsoleState
import com.varabyte.konsole.core.KonsoleScope

internal val RESET_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.RESET) {
    override fun updateState(state: KonsoleState) {
        state.clear()
    }
}

/** Clear all ANSI text effects commands, like colors, underlines, etc. */
fun KonsoleScope.reset() {
    applyCommand(RESET_COMMAND)
}