package com.varabyte.konsole.core.text

import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.internal.ansi.commands.RESET_COMMAND

/** Clear all ANSI text effects commands, like colors, underlines, etc. */
fun KonsoleScope.reset() {
    applyCommand(RESET_COMMAND)
}