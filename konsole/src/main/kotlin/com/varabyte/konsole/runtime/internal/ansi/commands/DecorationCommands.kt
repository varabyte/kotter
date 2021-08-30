package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.internal.ansi.Ansi

internal val BOLD_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.BOLD) {
    override fun updateState(state: KonsoleState) {
        state.bolded = this
    }
}

internal val CLEAR_BOLD_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.CLEAR_BOLD) {
    override fun updateState(state: KonsoleState) {
        state.bolded = null
    }
}

internal val UNDERLINE_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.UNDERLINE) {
    override fun updateState(state: KonsoleState) {
        state.underlined = this
    }
}

internal val CLEAR_UNDERLINE_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.CLEAR_UNDERLINE) {
    override fun updateState(state: KonsoleState) {
        state.underlined = null
    }
}

internal val STRIKETHROUGH_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.STRIKETHROUGH) {
    override fun updateState(state: KonsoleState) {
        state.struckThrough = this
    }
}

internal val CLEAR_STRIKETHROUGH_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.CLEAR_STRIKETHROUGH) {
    override fun updateState(state: KonsoleState) {
        state.struckThrough = null
    }
}