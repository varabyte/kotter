package com.varabyte.konsole.internal.ansi.commands

import com.varabyte.konsole.core.KonsoleState
import com.varabyte.konsole.internal.ansi.Ansi

internal val BOLD_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.BOLD) {
    override fun updateState(state: KonsoleState) {
        state.bolded = this
    }
}
internal val UNDERLINE_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.UNDERLINE) {
    override fun updateState(state: KonsoleState) {
        state.underlined = this
    }
}
internal val STRIKETHROUGH_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.STRIKETHROUGH) {
    override fun updateState(state: KonsoleState) {
        state.struckThrough = this
    }
}