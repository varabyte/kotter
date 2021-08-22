package com.varabyte.konsole

import com.varabyte.konsole.terminal.SystemTerminalIO
import com.varabyte.konsole.terminal.TerminalIO
import java.awt.Color
import java.awt.Dimension

object KonsoleSettings {
    var provideTerminalIO: () -> TerminalIO = { SystemTerminalIO() }

    object VirtualTerminal {
        var title: String? = null
        /** Size in number of characters that can fit in the terminal */
        var size = Dimension(100, 40)
        var fontSize = 16
        var fgColor = Color.LIGHT_GRAY
        var bgColor = Color.DARK_GRAY
    }
}