package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.render.RenderScope

fun RenderScope.bold() {
    applyCommand(BOLD_COMMAND)
}

fun RenderScope.clearBold() {
    applyCommand(CLEAR_BOLD_COMMAND)
}

fun RenderScope.bold(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        bold()
        scopedBlock()
    }
}

fun RenderScope.underline() {
    applyCommand(UNDERLINE_COMMAND)
}

fun RenderScope.clearUnderline() {
    applyCommand(CLEAR_UNDERLINE_COMMAND)
}

fun RenderScope.underline(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        underline()
        scopedBlock()
    }
}

fun RenderScope.strikethrough() {
    applyCommand(STRIKETHROUGH_COMMAND)
}

fun RenderScope.clearStrikethrough() {
    applyCommand(CLEAR_STRIKETHROUGH_COMMAND)
}

fun RenderScope.strikethrough(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        strikethrough()
        scopedBlock()
    }
}