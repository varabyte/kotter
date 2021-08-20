package com.varabyte.konsole.terminal.swing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.Font
import javax.swing.JFrame
import javax.swing.JTextPane

object SwingTerminal {
    suspend fun show(title: String = "Konsole Terminal") {
        withContext(Dispatchers.Swing) {
            val frame = JFrame(title)
            frame.add(SwingTerminalPane())
            frame.pack()
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
        }
    }
}

class SwingTerminalPane : JTextPane() {
    init {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        preferredSize = Dimension(640, 480)
    }
}