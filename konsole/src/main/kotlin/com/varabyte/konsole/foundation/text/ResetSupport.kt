package com.varabyte.konsole.foundation.text

import com.varabyte.konsole.runtime.RenderScope
import com.varabyte.konsole.runtime.internal.ansi.commands.RESET_COMMAND

/** Clear all ANSI text effects commands, like colors, underlines, etc. */
fun RenderScope.reset() {
    applyCommand(RESET_COMMAND)
}