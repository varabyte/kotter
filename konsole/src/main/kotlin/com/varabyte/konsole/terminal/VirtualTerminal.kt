package com.varabyte.konsole.terminal

import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.runtime.internal.ansi.Ansi.Csi.Codes.Sgr.Colors
import com.varabyte.konsole.runtime.internal.ansi.Ansi.Csi.Codes.Sgr.Decorations
import com.varabyte.konsole.runtime.internal.ansi.Ansi.Csi.Codes.Sgr.RESET
import com.varabyte.konsole.runtime.internal.text.TextPtr
import com.varabyte.konsole.runtime.internal.text.substring
import com.varabyte.konsole.runtime.terminal.Terminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowEvent.WINDOW_CLOSING
import java.util.concurrent.CountDownLatch
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.text.Document
import javax.swing.text.MutableAttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class VirtualTerminal private constructor(private val pane: SwingTerminalPane) : Terminal {
    companion object {
        /**
         * @param terminalSize Number of characters, so 80x32 will be expanded to fit 80 characters horizontally and
         *   32 lines vertically (before scrolling is needed)
         * @param handleInterrupt If true, handle CTRL-C by closing the window.
         */
        fun create(
            title: String = "Virtual Terminal",
            terminalSize: Dimension = Dimension(100, 40),
            fontSize: Int = 16,
            fgColor: Color = Color.LIGHT_GRAY,
            bgColor: Color = Color.DARK_GRAY,
            handleInterrupt: Boolean = true
        ): VirtualTerminal {
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

            val terminal = VirtualTerminal(pane)
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
                frame.setLocationRelativeTo(null)

                terminal.pane.text = ""
                if (handleInterrupt) {
                    terminal.pane.addKeyListener(object : KeyAdapter() {
                        override fun keyPressed(e: KeyEvent) {
                            if (e.isControlDown && e.keyCode == KeyEvent.VK_C) {
                                frame.dispatchEvent(WindowEvent(frame, WINDOW_CLOSING))
                                e.consume()
                            }
                        }
                    })
                }

                framePacked.countDown()
                frame.isVisible = true
            }
            framePacked.await()

            return terminal
        }
    }

    private inline fun <reified T> Component.findAncestor(): T? {
        var c: Component? = this
        while (c != null) {
            if (c is T) return c
            c = c.parent
        }
        return null
    }

    private val Component.window get() = findAncestor<Window>()
    private val Component.scrollPane get() = findAncestor<JScrollPane>()
    private fun BoundedRangeModel.isAtEnd() = value + extent >= maximum

    override fun write(text: String) {
        runBlocking {
            CoroutineScope(Dispatchers.Swing).launch {
                // Here, we update our text pane causing it to repaint, but as a side effect, this screws with the
                // vscroll and hscroll positions. If the user has intentionally set either of those values themselves,
                // we should fight to keep them.
                val scrollPane = pane.scrollPane!!

                val vScrollPos = scrollPane.verticalScrollBar.model.value
                val vAtEnd = scrollPane.verticalScrollBar.model.isAtEnd()
                val hScrollPos = scrollPane.horizontalScrollBar.model.value
                val hAtEnd = scrollPane.horizontalScrollBar.model.isAtEnd()

                pane.processAnsiText(text)

                if (!vAtEnd) {
                    launch { scrollPane.verticalScrollBar.model.value = vScrollPos }
                }
                if (!hAtEnd) {
                    launch { scrollPane.horizontalScrollBar.model.value = hScrollPos }
                }
            }
        }
    }

    override fun read(): Flow<Int> = callbackFlow {
        pane.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                val chars: CharSequence = when(e.keyCode) {
                    KeyEvent.VK_UP -> Ansi.Csi.Codes.Keys.UP.toFullEscapeCode()
                    KeyEvent.VK_DOWN -> Ansi.Csi.Codes.Keys.DOWN.toFullEscapeCode()
                    KeyEvent.VK_LEFT -> Ansi.Csi.Codes.Keys.LEFT.toFullEscapeCode()
                    KeyEvent.VK_RIGHT -> Ansi.Csi.Codes.Keys.RIGHT.toFullEscapeCode()
                    KeyEvent.VK_HOME -> Ansi.Csi.Codes.Keys.HOME.toFullEscapeCode()
                    KeyEvent.VK_END -> Ansi.Csi.Codes.Keys.END.toFullEscapeCode()
                    KeyEvent.VK_DELETE -> Ansi.Csi.Codes.Keys.DELETE.toFullEscapeCode()
                    KeyEvent.VK_ENTER -> Ansi.CtrlChars.ENTER.toString()
                    KeyEvent.VK_BACK_SPACE -> Ansi.CtrlChars.BACKSPACE.toString()
                    else -> e.keyChar.takeIf { it.isDefined() && it.category != CharCategory.CONTROL }?.toString() ?: ""
                }
                chars.forEach { c -> trySend(c.code) }
                if (chars.isNotEmpty()) e.consume()
            }
        })

        pane.window!!.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                channel.close()
            }
        })

        awaitClose()
    }

    override fun close() {
        write("(This terminal session has ended. Press any key to continue.)")
        pane.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                with(pane.window!!) {
                    dispatchEvent(WindowEvent(this, WINDOW_CLOSING))
                }
            }
        })
    }
}

private const val Inverted = "inverted"
private fun MutableAttributeSet.invertColors() {
    val prevFg = StyleConstants.getForeground(this)
    val prevBg = StyleConstants.getBackground(this)
    StyleConstants.setForeground(this, prevBg)
    StyleConstants.setBackground(this, prevFg)
}
private fun MutableAttributeSet.setInverseAwareForeground(color: Color) {
    if (getAttribute(Inverted) == true) {
        StyleConstants.setBackground(this, color)
    }
    else {
        StyleConstants.setForeground(this, color)
    }
}
private fun MutableAttributeSet.setInverseAwareBackground(color: Color) {
    if (getAttribute(Inverted) == true) {
        StyleConstants.setForeground(this, color)
    }
    else {
        StyleConstants.setBackground(this, color)
    }
}

private val SGR_CODE_TO_ATTR_MODIFIER = mapOf<Ansi.Csi.Code, MutableAttributeSet.() -> Unit>(
    RESET to { removeAttributes(this) },

    Colors.Fg.CLEAR to { removeAttribute(StyleConstants.Foreground) },
    Colors.Fg.BLACK to { setInverseAwareForeground(Color.BLACK) },
    Colors.Fg.RED to { setInverseAwareForeground(Color.RED) },
    Colors.Fg.GREEN to { setInverseAwareForeground(Color.GREEN) },
    Colors.Fg.YELLOW to { setInverseAwareForeground(Color.YELLOW) },
    Colors.Fg.BLUE to { setInverseAwareForeground(Color.BLUE) },
    Colors.Fg.MAGENTA to { setInverseAwareForeground(Color.MAGENTA) },
    Colors.Fg.CYAN to { setInverseAwareForeground(Color.CYAN) },
    Colors.Fg.WHITE to { setInverseAwareForeground(Color.WHITE) },
    Colors.Fg.BLACK_BRIGHT to { setInverseAwareForeground(Color.BLACK) },
    Colors.Fg.RED_BRIGHT to { setInverseAwareForeground(Color.RED) },
    Colors.Fg.GREEN_BRIGHT to { setInverseAwareForeground(Color.GREEN) },
    Colors.Fg.YELLOW_BRIGHT to { setInverseAwareForeground(Color.YELLOW) },
    Colors.Fg.BLUE_BRIGHT to { setInverseAwareForeground(Color.BLUE) },
    Colors.Fg.MAGENTA_BRIGHT to { setInverseAwareForeground(Color.MAGENTA) },
    Colors.Fg.CYAN_BRIGHT to { setInverseAwareForeground(Color.CYAN) },
    Colors.Fg.WHITE_BRIGHT to { setInverseAwareForeground(Color.WHITE) },

    Colors.Bg.CLEAR to { removeAttribute(StyleConstants.Background) },
    Colors.Bg.BLACK to { setInverseAwareBackground(Color.BLACK) },
    Colors.Bg.RED to { setInverseAwareBackground(Color.RED) },
    Colors.Bg.GREEN to { setInverseAwareBackground(Color.GREEN) },
    Colors.Bg.YELLOW to { setInverseAwareBackground(Color.YELLOW) },
    Colors.Bg.BLUE to { setInverseAwareBackground(Color.BLUE) },
    Colors.Bg.MAGENTA to { setInverseAwareBackground(Color.MAGENTA) },
    Colors.Bg.CYAN to { setInverseAwareBackground(Color.CYAN) },
    Colors.Bg.WHITE to { setInverseAwareBackground(Color.WHITE) },
    Colors.Bg.BLACK_BRIGHT to { setInverseAwareBackground(Color.BLACK) },
    Colors.Bg.RED_BRIGHT to { setInverseAwareBackground(Color.RED) },
    Colors.Bg.GREEN_BRIGHT to { setInverseAwareBackground(Color.GREEN) },
    Colors.Bg.YELLOW_BRIGHT to { setInverseAwareBackground(Color.YELLOW) },
    Colors.Bg.BLUE_BRIGHT to { setInverseAwareBackground(Color.BLUE) },
    Colors.Bg.MAGENTA_BRIGHT to { setInverseAwareBackground(Color.MAGENTA) },
    Colors.Bg.CYAN_BRIGHT to { setInverseAwareBackground(Color.CYAN) },
    Colors.Bg.WHITE_BRIGHT to { setInverseAwareBackground(Color.WHITE) },

    Colors.INVERT to {
        if (getAttribute(Inverted) == null) {
            invertColors()
            addAttribute(Inverted, true)
        }
     },
    Colors.CLEAR_INVERT to {
        if (getAttribute(Inverted) != null) {

            invertColors()
            removeAttribute(Inverted)
        }
   },

    Decorations.BOLD to { StyleConstants.setBold(this, true) },
    Decorations.CLEAR_BOLD to { removeAttribute(StyleConstants.Bold) },
    Decorations.UNDERLINE to { StyleConstants.setUnderline(this, true) },
    Decorations.CLEAR_UNDERLINE to { removeAttribute(StyleConstants.Underline) },
    Decorations.STRIKETHROUGH to { StyleConstants.setStrikeThrough(this, true) },
    Decorations.CLEAR_STRIKETHROUGH to { removeAttribute(StyleConstants.StrikeThrough) },
)

private fun Document.getText() = getText(0, length)

class SwingTerminalPane(fontSize: Int) : JTextPane() {
    init {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, fontSize)

        // The existing mouse handlers set the cursor behind our back which mess with the repainting of the area
        // Let's just disable them for now.
        mouseListeners.toList().forEach { removeMouseListener(it) }
        mouseMotionListeners.toList().forEach { removeMouseMotionListener(it) }
    }

    private fun processEscapeCode(textPtr: TextPtr, doc: Document, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.increment()) return false
        return when (textPtr.currChar) {
            Ansi.EscSeq.CSI -> processCsiCode(textPtr, doc, attrs)
            else -> false
        }
    }

    private fun processCsiCode(textPtr: TextPtr, doc: Document, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.increment()) return false

        val csiParts = Ansi.Csi.Code.parts(textPtr) ?: return false
        val csiCode = Ansi.Csi.Code(csiParts)

        val identifier = Ansi.Csi.Identifier.fromCode(csiCode) ?: return false
        return when (identifier) {
            Ansi.Csi.Identifiers.CURSOR_PREV_LINE -> {
                var numLines = csiCode.parts.numericCode ?: 1
                with(TextPtr(doc.getText(), caretPosition)) {
                    // First, move to beginning of this line
                    if (currChar != '\n') {
                        decrementUntil { it == '\n' }
                    }
                    while (numLines > 0) {
                        if (!decrementUntil { it == '\n' }) {
                            // We hit the beginning of the text area so just abort early
                            break
                        }
                        --numLines
                    }
                    if (currChar == '\n') {
                        // We're now at the beginning of the new line. Increment so we don't delete it too.
                        increment()
                    }
                    caretPosition = charIndex
                    doc.remove(caretPosition, doc.length - caretPosition)
                }
                true
            }
            Ansi.Csi.Identifiers.ERASE_LINE -> {
                when (csiCode) {
                    Ansi.Csi.Codes.Erase.CURSOR_TO_LINE_END -> {
                        with(TextPtr(doc.getText(), caretPosition)) {
                            incrementUntil { it == '\n' }
                            doc.remove(caretPosition, charIndex - caretPosition)
                        }
                        true
                    }
                    else -> false
                }
            }
            Ansi.Csi.Identifiers.SGR -> {
                SGR_CODE_TO_ATTR_MODIFIER[csiCode]?.let { modifyAttributes ->
                    modifyAttributes(attrs)
                    true
                } ?: false
            }
            else -> return false
        }
    }

    fun processAnsiText(text: String) {
        require(SwingUtilities.isEventDispatchThread())
        if (text.isEmpty()) return

        val doc = styledDocument
        val attrs = SimpleAttributeSet()
        // Set foreground and background explicitly so color inversion can work
        StyleConstants.setForeground(attrs, foreground)
        StyleConstants.setBackground(attrs, background)
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
                Ansi.CtrlChars.ESC -> {
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

        // Hack alert: I'm not sure why, but calling updateUI is the only consistent way I've been able to get the text
        // pane to refresh its text contents without stuttering. However, this sometimes affects the caret position? So
        // we reset it back.
        caretPosition.let { prevCaret ->
            updateUI()
            caretPosition = prevCaret
        }
    }
}