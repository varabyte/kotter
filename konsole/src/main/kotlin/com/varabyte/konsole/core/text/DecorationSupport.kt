package com.varabyte.konsole.core.text

import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.RenderScope
import com.varabyte.konsole.internal.ansi.commands.BOLD_COMMAND
import com.varabyte.konsole.internal.ansi.commands.STRIKETHROUGH_COMMAND
import com.varabyte.konsole.internal.ansi.commands.UNDERLINE_COMMAND

fun RenderScope.bold() {
    applyCommand(BOLD_COMMAND)
}

fun RenderScope.bold(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        bold()
        scopedBlock()
    }
}

fun RenderScope.underline() {
    applyCommand(UNDERLINE_COMMAND)
}

fun RenderScope.underline(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        underline()
        scopedBlock()
    }
}

fun RenderScope.strikethrough() {
    applyCommand(STRIKETHROUGH_COMMAND)
}

fun RenderScope.strikethrough(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        strikethrough()
        scopedBlock()
    }
}