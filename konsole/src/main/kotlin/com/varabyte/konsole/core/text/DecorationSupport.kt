package com.varabyte.konsole.core.text

import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.internal.ansi.commands.BOLD_COMMAND
import com.varabyte.konsole.internal.ansi.commands.STRIKETHROUGH_COMMAND
import com.varabyte.konsole.internal.ansi.commands.UNDERLINE_COMMAND

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