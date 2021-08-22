package com.varabyte.konsole.text

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.ansi.AnsiSgrKonsoleCommand
import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleBlockState
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.scopedState

private val BOLD_COMMAND = object : AnsiSgrKonsoleCommand(AnsiCodes.Sgr.Decorations.BOLD) {
    override fun updateState(state: KonsoleBlockState) {
        state.bolded = this
    }
}
private val ITALIC_COMMAND = object : AnsiSgrKonsoleCommand(AnsiCodes.Sgr.Decorations.ITALIC) {
    override fun updateState(state: KonsoleBlockState) {
        state.italicized = this
    }
}
private val UNDERLINE_COMMAND = object : AnsiSgrKonsoleCommand(AnsiCodes.Sgr.Decorations.UNDERLINE) {
    override fun updateState(state: KonsoleBlockState) {
        state.underlined = this
    }
}
private val STRIKETHROUGH_COMMAND = object : AnsiSgrKonsoleCommand(AnsiCodes.Sgr.Decorations.STRIKETHROUGH) {
    override fun updateState(state: KonsoleBlockState) {
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

fun KonsoleScope.italic() {
    applyCommand(ITALIC_COMMAND)
}

fun KonsoleScope.italic(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        italic()
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
