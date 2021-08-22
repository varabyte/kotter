package com.varabyte.konsole.terminal.swing

import com.varabyte.konsole.KonsoleSettings
import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.Colors.Bg
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.Colors.Fg
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.Decorations
import com.varabyte.konsole.ansi.AnsiCodes.Sgr.RESET
import com.varabyte.konsole.terminal.TerminalIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.MutableAttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class SwingTerminalIO private constructor(private val pane: SwingTerminalPane) : TerminalIO {
    companion object {
        fun create(
            title: String = KonsoleSettings.VirtualTerminal.title ?: "Konsole Terminal",
            numCharacters: Dimension = KonsoleSettings.VirtualTerminal.size,
            fontSize: Int = KonsoleSettings.VirtualTerminal.fontSize,
            fgColor: Color = KonsoleSettings.VirtualTerminal.fgColor,
            bgColor: Color = KonsoleSettings.VirtualTerminal.bgColor,
        ): SwingTerminalIO {
            val pane = SwingTerminalPane(fontSize)
            pane.foreground = fgColor
            pane.background = bgColor
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
        pane.processAnsiText(text)
    }

    override fun read(): Flow<Int> = callbackFlow {
        TODO("Not yet implemented")
    }
}

private fun String.startsWithAt(charIndex: Int, prefix: String): Boolean {
    if (this.length < charIndex + prefix.length) return false

    for (i in prefix.indices) {
        if (this[charIndex + i] != prefix[i]) return false
    }

    return true
}

private val SGR_CODE_TO_ATTR_MODIFIER = mapOf<String, MutableAttributeSet.() -> Unit>(
    RESET to {
        removeAttributes(this)
    },

    Fg.BLACK to {
        StyleConstants.setForeground(this, Color.BLACK)
    },
    Fg.RED to {
        StyleConstants.setForeground(this, Color.RED)
    },
    Fg.GREEN to {
        StyleConstants.setForeground(this, Color.GREEN)
    },
    Fg.YELLOW to {
        StyleConstants.setForeground(this, Color.YELLOW)
    },
    Fg.BLUE to {
        StyleConstants.setForeground(this, Color.BLUE)
    },
    Fg.MAGENTA to {
        StyleConstants.setForeground(this, Color.MAGENTA)
    },
    Fg.CYAN to {
        StyleConstants.setForeground(this, Color.CYAN)
    },
    Fg.WHITE to {
        StyleConstants.setForeground(this, Color.WHITE)
    },
    Fg.BLACK_BRIGHT to {
        StyleConstants.setForeground(this, Color.BLACK)
    },
    Fg.RED_BRIGHT to {
        StyleConstants.setForeground(this, Color.RED)
    },
    Fg.GREEN_BRIGHT to {
        StyleConstants.setForeground(this, Color.GREEN)
    },
    Fg.YELLOW_BRIGHT to {
        StyleConstants.setForeground(this, Color.YELLOW)
    },
    Fg.BLUE_BRIGHT to {
        StyleConstants.setForeground(this, Color.BLUE)
    },
    Fg.MAGENTA_BRIGHT to {
        StyleConstants.setForeground(this, Color.MAGENTA)
    },
    Fg.CYAN_BRIGHT to {
        StyleConstants.setForeground(this, Color.CYAN)
    },
    Fg.WHITE_BRIGHT to {
        StyleConstants.setForeground(this, Color.WHITE)
    },

    Bg.BLACK to {
        StyleConstants.setBackground(this, Color.BLACK)
    },
    Bg.RED to {
        StyleConstants.setBackground(this, Color.RED)
    },
    Bg.GREEN to {
        StyleConstants.setBackground(this, Color.GREEN)
    },
    Bg.YELLOW to {
        StyleConstants.setBackground(this, Color.YELLOW)
    },
    Bg.BLUE to {
        StyleConstants.setBackground(this, Color.BLUE)
    },
    Bg.MAGENTA to {
        StyleConstants.setBackground(this, Color.MAGENTA)
    },
    Bg.CYAN to {
        StyleConstants.setBackground(this, Color.CYAN)
    },
    Bg.WHITE to {
        StyleConstants.setBackground(this, Color.WHITE)
    },
    Bg.BLACK_BRIGHT to {
        StyleConstants.setBackground(this, Color.BLACK)
    },
    Bg.RED_BRIGHT to {
        StyleConstants.setBackground(this, Color.RED)
    },
    Bg.GREEN_BRIGHT to {
        StyleConstants.setBackground(this, Color.GREEN)
    },
    Bg.YELLOW_BRIGHT to {
        StyleConstants.setBackground(this, Color.YELLOW)
    },
    Bg.BLUE_BRIGHT to {
        StyleConstants.setBackground(this, Color.BLUE)
    },
    Bg.MAGENTA_BRIGHT to {
        StyleConstants.setBackground(this, Color.MAGENTA)
    },
    Bg.CYAN_BRIGHT to {
        StyleConstants.setBackground(this, Color.CYAN)
    },
    Bg.WHITE_BRIGHT to {
        StyleConstants.setBackground(this, Color.WHITE)
    },

    Decorations.BOLD to {
        StyleConstants.setBold(this, true)
    },
    Decorations.ITALIC to {
        StyleConstants.setItalic(this, true)
    },
    Decorations.UNDERLINE to {
        StyleConstants.setUnderline(this, true)
    },
    Decorations.STRIKETHROUGH to {
        StyleConstants.setStrikeThrough(this, true)
    },
)


class SwingTerminalPane(fontSize: Int) : JTextPane() {
    init {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.BOLD, fontSize)
    }

    fun processAnsiText(text: String) {
        val doc = styledDocument
        val attributes = SimpleAttributeSet()
        val stringBuilder = StringBuilder()
        fun flush() {
            doc.insertString(doc.length, stringBuilder.toString(), attributes)
            stringBuilder.clear()
        }

        var charIndex = 0
        while (charIndex < text.length) {
            when (val c = text[charIndex]) {
                AnsiCodes.ControlCharacters.ESC -> {
                    flush()
                    var handled = false
                    when {
                        text.startsWithAt(charIndex, AnsiCodes.EscapeSequences.CSI) -> {
                            for (entry in SGR_CODE_TO_ATTR_MODIFIER) {
                                val code = entry.key
                                if (text.startsWithAt(charIndex, code)) {
                                    handled = true
                                    val modifyAttributes = entry.value
                                    modifyAttributes(attributes)
                                    charIndex += code.length - 1
                                    break
                                }
                            }
                        }
                    }
                    if (!handled) {
                        val peekLen = 7
                        throw IllegalArgumentException(
                            "Unknown escape sequence starting here (plus next $peekLen characters): ${
                                text.substring(
                                    charIndex + 1,
                                    charIndex + peekLen + 1
                                )
                            }..."
                        )
                    }
                }
                else -> stringBuilder.append(c)
            }
            ++charIndex
        }
        flush()
    }
}
