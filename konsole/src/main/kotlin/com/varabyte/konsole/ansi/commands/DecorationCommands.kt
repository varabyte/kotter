package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.ansi.Ansi
import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.KonsoleState

private val BOLD_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.BOLD) {
    override fun updateState(state: KonsoleState) {
        state.bolded = this
    }
}
private val UNDERLINE_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.UNDERLINE) {
    override fun updateState(state: KonsoleState) {
        state.underlined = this
    }
}
private val STRIKETHROUGH_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.STRIKETHROUGH) {
    override fun updateState(state: KonsoleState) {
        state.struckThrough = this
    }
}

fun KonsoleScope.bold() {
    applyCommand(BOLD_COMMAND)
}

fun KonsoleScope.bold(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        bold()
        scopedBlock()
    }
}

fun KonsoleScope.underline() {
    applyCommand(UNDERLINE_COMMAND)
}

fun KonsoleScope.underline(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        underline()
        scopedBlock()
    }
}

fun KonsoleScope.strikethrough() {
    applyCommand(STRIKETHROUGH_COMMAND)
}

fun KonsoleScope.strikethrough(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        strikethrough()
        scopedBlock()
    }
}