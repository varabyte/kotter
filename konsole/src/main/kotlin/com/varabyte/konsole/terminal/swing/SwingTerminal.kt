package com.varabyte.konsole.terminal.swing

import com.varabyte.konsole.KonsoleSettings
import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.ansi.AnsiCodes.Csi
import com.varabyte.konsole.ansi.AnsiCodes.Csi.Sgr.Colors.Bg
import com.varabyte.konsole.ansi.AnsiCodes.Csi.Sgr.Colors.Fg
import com.varabyte.konsole.ansi.AnsiCodes.Csi.Sgr.Decorations
import com.varabyte.konsole.ansi.AnsiCodes.Csi.Sgr.RESET
import com.varabyte.konsole.terminal.Terminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.text.Document
import javax.swing.text.MutableAttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import kotlin.math.max
import kotlin.math.min

class SwingTerminal private constructor(private val pane: SwingTerminalPane) : Terminal {
    companion object {
        /**
         * @param terminalSize Number of characters, so 80x32 will be expanded to fit 80 characters horizontally and
         *   32 lines vertically (before scrolling is needed)
         */
        fun create(
            title: String = KonsoleSettings.VirtualTerminal.title ?: "Konsole Terminal",
            terminalSize: Dimension = KonsoleSettings.VirtualTerminal.size,
            fontSize: Int = KonsoleSettings.VirtualTerminal.fontSize,
            fgColor: Color = KonsoleSettings.VirtualTerminal.fgColor,
            bgColor: Color = KonsoleSettings.VirtualTerminal.bgColor,
        ): SwingTerminal {
            val pane = SwingTerminalPane(fontSize)
            pane.foreground = fgColor
            pane.background = bgColor
            pane.text = buildString {
                // Set initial text to a block of blank characters so pack will set it to the right size
                for (h in 0 until terminalSize.height) {
                    if (h > 0) appendLine()
                    for (w in 0 until terminalSize.width) {
                        append(' ')
                    }
                }
            }

            val terminal = SwingTerminal(pane)
            val framePacked = CountDownLatch(1)
            CoroutineScope((Dispatchers.Swing)).launch {
                val frame = JFrame(title)
                frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

                frame.contentPane.add(JScrollPane(terminal.pane).apply {
                    border = EmptyBorder(5, 5, 5, 5)
                    foreground = fgColor
                    background = bgColor
                })
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
        runBlocking {
            CoroutineScope(Dispatchers.Swing).launch {
                pane.processAnsiText(text)
            }.join()
        }
    }

    override fun read(): Flow<Int> = callbackFlow {
        TODO("Not yet implemented")
    }
}

/**
 * Point at to a position inside some text.
 *
 * Note that this supports the concept of the null string terminator - that is, for a String of length one, e.g. "a",
 * then textPtr[0] == 'a' and textPtr[1] == '\0'
 */
private class TextPtr(val text: String, charIndex: Int = 0) {

    var charIndex = 0
        set(value) {
            require(value >= 0 && value <= text.length) { "charIndex value is out of bounds. Expected 0 .. ${text.length}, got $value" }
            field = value
        }

    val currChar get() = text.elementAtOrNull(charIndex) ?: Char.MIN_VALUE
    val remainingLength get() = max(0, text.length - charIndex)

    init {
        this.charIndex = charIndex
    }

    /**
     * Increment or decrement the pointer first (based on [forward]), and then keep moving until
     * [keepMoving] stops returning true.
     */
    private fun movePtr(forward: Boolean, keepMoving: (Char) -> Boolean): Boolean {
        val delta = if (forward) 1 else -1

        var newIndex = charIndex
        do {
            newIndex += delta
            if (newIndex < 0) {
                newIndex = 0
                break
            } else if (newIndex >= text.length) {
                newIndex = text.length
                break
            }
        } while (keepMoving(text[newIndex]))

        if (newIndex != charIndex) {
            charIndex = newIndex
            return true
        }
        return false
    }

    fun increment(): Boolean {
        return movePtr(true) { false }
    }

    fun decrement(): Boolean {
        return movePtr(false) { false }
    }

    fun incrementWhile(whileCondition: (Char) -> Boolean) = movePtr(true, whileCondition)
    fun decrementWhile(whileCondition: (Char) -> Boolean) = movePtr(false, whileCondition)
    fun incrementUntil(whileCondition: (Char) -> Boolean): Boolean {
        return incrementWhile { !whileCondition(it) }
    }

    fun decrementUntil(whileCondition: (Char) -> Boolean): Boolean {
        return decrementWhile { !whileCondition(it) }
    }
}

private fun TextPtr.substring(length: Int): String {
    return text.substring(charIndex, min(charIndex + length, text.length))
}

private fun TextPtr.readInt(): Int? {
    if (!currChar.isDigit()) return null

    var intValue = 0
    while (true) {
        val digit = currChar.digitToIntOrNull() ?: break
        increment()
        intValue *= 10
        intValue += digit
    }
    return intValue
}

private val SGR_CODE_TO_ATTR_MODIFIER = mapOf<Csi.Code, MutableAttributeSet.() -> Unit>(
    RESET to { removeAttributes(this) },

    Fg.BLACK to { StyleConstants.setForeground(this, Color.BLACK) },
    Fg.RED to { StyleConstants.setForeground(this, Color.RED) },
    Fg.GREEN to { StyleConstants.setForeground(this, Color.GREEN) },
    Fg.YELLOW to { StyleConstants.setForeground(this, Color.YELLOW) },
    Fg.BLUE to { StyleConstants.setForeground(this, Color.BLUE) },
    Fg.MAGENTA to { StyleConstants.setForeground(this, Color.MAGENTA) },
    Fg.CYAN to { StyleConstants.setForeground(this, Color.CYAN) },
    Fg.WHITE to { StyleConstants.setForeground(this, Color.WHITE) },
    Fg.BLACK_BRIGHT to { StyleConstants.setForeground(this, Color.BLACK) },
    Fg.RED_BRIGHT to { StyleConstants.setForeground(this, Color.RED) },
    Fg.GREEN_BRIGHT to { StyleConstants.setForeground(this, Color.GREEN) },
    Fg.YELLOW_BRIGHT to { StyleConstants.setForeground(this, Color.YELLOW) },
    Fg.BLUE_BRIGHT to { StyleConstants.setForeground(this, Color.BLUE) },
    Fg.MAGENTA_BRIGHT to { StyleConstants.setForeground(this, Color.MAGENTA) },
    Fg.CYAN_BRIGHT to { StyleConstants.setForeground(this, Color.CYAN) },
    Fg.WHITE_BRIGHT to { StyleConstants.setForeground(this, Color.WHITE) },

    Bg.BLACK to { StyleConstants.setBackground(this, Color.BLACK) },
    Bg.RED to { StyleConstants.setBackground(this, Color.RED) },
    Bg.GREEN to { StyleConstants.setBackground(this, Color.GREEN) },
    Bg.YELLOW to { StyleConstants.setBackground(this, Color.YELLOW) },
    Bg.BLUE to { StyleConstants.setBackground(this, Color.BLUE) },
    Bg.MAGENTA to { StyleConstants.setBackground(this, Color.MAGENTA) },
    Bg.CYAN to { StyleConstants.setBackground(this, Color.CYAN) },
    Bg.WHITE to { StyleConstants.setBackground(this, Color.WHITE) },
    Bg.BLACK_BRIGHT to { StyleConstants.setBackground(this, Color.BLACK) },
    Bg.RED_BRIGHT to { StyleConstants.setBackground(this, Color.RED) },
    Bg.GREEN_BRIGHT to { StyleConstants.setBackground(this, Color.GREEN) },
    Bg.YELLOW_BRIGHT to { StyleConstants.setBackground(this, Color.YELLOW) },
    Bg.BLUE_BRIGHT to { StyleConstants.setBackground(this, Color.BLUE) },
    Bg.MAGENTA_BRIGHT to { StyleConstants.setBackground(this, Color.MAGENTA) },
    Bg.CYAN_BRIGHT to { StyleConstants.setBackground(this, Color.CYAN) },
    Bg.WHITE_BRIGHT to { StyleConstants.setBackground(this, Color.WHITE) },

    Decorations.BOLD to { StyleConstants.setBold(this, true) },
    Decorations.ITALIC to { StyleConstants.setItalic(this, true) },
    Decorations.UNDERLINE to { StyleConstants.setUnderline(this, true) },
    Decorations.STRIKETHROUGH to { StyleConstants.setStrikeThrough(this, true) },
)

private fun Document.getText() = getText(0, length)

class SwingTerminalPane(fontSize: Int) : JTextPane() {
    init {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, fontSize)
    }

    private fun processEscapeCode(textPtr: TextPtr, doc: Document, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.increment()) return false
        return when (textPtr.currChar) {
            AnsiCodes.EscSeq.CSI -> processCsiCode(textPtr, doc, attrs)
            else -> false
        }
    }

    private fun processCsiCode(textPtr: TextPtr, doc: Document, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.increment()) return false

        val numericCode = textPtr.readInt() ?: return false
        val optionalCode = if (textPtr.currChar == ';') {
            textPtr.increment()
            textPtr.readInt() ?: 0
        } else {
            null
        }
        val finalCode = textPtr.currChar
        val csiCode = Csi.Code("$numericCode${if (optionalCode != null) ";$optionalCode" else ""}$finalCode")

        val identifier = Csi.Identifier.fromCode(finalCode) ?: return false
        return when (identifier) {
            Csi.Identifiers.ERASE_LINE -> {
                when (csiCode) {
                    Csi.EraseLine.CURSOR_TO_END -> {
                        with(TextPtr(doc.getText(), caretPosition)) {
                            incrementUntil { it == '\n' }
                            doc.remove(caretPosition, charIndex - caretPosition)
                        }
                        true
                    }
                    Csi.EraseLine.ENTIRE_LINE -> {
                        with(TextPtr(doc.getText(), caretPosition)) {
                            incrementUntil { it == '\n' }
                            val to = charIndex
                            val toChar = currChar
                            decrementUntil { it == '\n' }
                            if (currChar == '\n' && toChar == '\n') {
                                // Only delete one of the two \n's
                                increment()
                            }
                            caretPosition = charIndex
                            doc.remove(caretPosition, to - caretPosition)
                        }
                        true
                    }
                    else -> false
                }
            }
            Csi.Identifiers.SGR -> {
                SGR_CODE_TO_ATTR_MODIFIER[csiCode]?.let { modifyAttributes ->
                    modifyAttributes(attrs)
                    true
                } ?: false
            }
        }
    }

    fun processAnsiText(text: String) {
        require(SwingUtilities.isEventDispatchThread())
        if (text.isEmpty()) return

        val doc = styledDocument
        val attrs = SimpleAttributeSet()
        val stringBuilder = StringBuilder()
        fun flush() {
            val stringToInsert = stringBuilder.toString()
            if (stringToInsert.isNotEmpty()) {
                doc.insertString(caretPosition, stringToInsert, attrs)
                stringBuilder.clear()
            }
        }

        val textPtr = TextPtr(text)
        do {
            when (textPtr.currChar) {
                AnsiCodes.Ctrl.ESC -> {
                    flush()
                    val prevCharIndex = textPtr.charIndex
                    if (!processEscapeCode(textPtr, doc, attrs)) {
                        // Skip over escape byte or else error message will be interpreted as an ANSI command!
                        textPtr.charIndex = prevCharIndex + 1
                        val peek = textPtr.substring(7)
                        val truncated = peek.length < textPtr.remainingLength
                        throw IllegalArgumentException(
                            "Unknown escape sequence: \"${peek}${if (truncated) "..." else ""}\""
                        )
                    }
                }
                '\r' -> {
                    with(TextPtr(doc.getText(), caretPosition)) {
                        decrementWhile { it != '\n' }
                        // Assuming we didn't hit the beginning of the string, we went too far by one
                        if (charIndex > 0) increment()

                        caretPosition = charIndex
                    }
                }
                Char.MIN_VALUE -> {
                } // Ignore the null terminator, it's only a TextPtr/Document concept
                else -> stringBuilder.append(textPtr.currChar)
            }
        } while (textPtr.increment())
        flush()
    }
}
