package com.varabyte.konsole

import java.awt.Dimension

object KonsoleSettings {
    object VirtualTerminal {
        var alwaysShow = false
        var title: String? = null
        /** Size in number of characters that can fit in the terminal */
        var size = Dimension(100, 40)
        var fontSize = 16
    }
}