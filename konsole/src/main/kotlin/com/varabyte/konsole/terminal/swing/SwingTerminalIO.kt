package com.varabyte.konsole.terminal.swing

import com.varabyte.konsole.KonsoleSettings
import com.varabyte.konsole.terminal.TerminalIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Dimension
import java.awt.Font
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane

class SwingTerminalIO private constructor(private val pane: SwingTerminalPane) : TerminalIO {
    companion object {
        fun create(
            title: String = KonsoleSettings.VirtualTerminal.title ?: "Konsole Terminal",
            numCharacters: Dimension = KonsoleSettings.VirtualTerminal.size,
            fontSize: Int = KonsoleSettings.VirtualTerminal.fontSize,
        ): SwingTerminalIO {
            val pane = SwingTerminalPane(fontSize)
            pane.text = buildString {
                // Set initial text to a block of blank characters so pack will set it to the right size
                for (h in 0 until numCharacters.height) {
                    if (h > 0) appendLine()
                    for (w in 0 until numCharacters.width) {
                        append(' ')
                    }
                }
            }

            val terminal = SwingTerminalIO(pane)
            val framePacked = CountDownLatch(1)
            CoroutineScope((Dispatchers.Swing)).launch {
                val frame = JFrame(title)
                frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

                frame.contentPane.add(JScrollPane(terminal.pane))
                frame.pack()
                terminal.pane.text = ""
                frame.setLocationRelativeTo(null)

                framePacked.countDown()
                frame.isVisible = true
            }
            framePacked.await()

            return terminal
        }
    }

    override fun write(text: String) {
        pane.text += text
    }

    override fun read(): Flow<Int> = callbackFlow {
        TODO("Not yet implemented")
    }
}

class SwingTerminalPane(fontSize: Int) : JTextPane() {
    init {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.BOLD, fontSize)
    }
}
