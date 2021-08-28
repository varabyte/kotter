package com.varabyte.konsole.core.text

import com.varabyte.konsole.core.RenderScope
import com.varabyte.konsole.internal.ansi.commands.RESET_COMMAND

/** Clear all ANSI text effects commands, like colors, underlines, etc. */
fun RenderScope.reset() {
    applyCommand(RESET_COMMAND)
}