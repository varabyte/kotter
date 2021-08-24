package com.varabyte.konsole

import com.varabyte.konsole.terminal.SystemTerminal
import com.varabyte.konsole.terminal.Terminal
import com.varabyte.konsole.terminal.swing.SwingTerminal
import java.awt.Color
import java.awt.Dimension

object KonsoleSettings {
    var provideTerminal: () -> Terminal = {
        try {
            SystemTerminal()
        }
        catch (ex: Exception) {
            SwingTerminal.create()
        }
    }
}