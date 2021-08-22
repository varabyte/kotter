package com.varabyte.konsole.text

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.ansi.AnsiKonsoleCommand
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleBlockState
import com.varabyte.konsole.core.scopedState

private val UNDERLINE_COMMAND = object : AnsiKonsoleCommand(AnsiCodes.Decorations.UNDERLINE) {
    override fun updateState(state: KonsoleBlockState) {
        state.underlined = this
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
