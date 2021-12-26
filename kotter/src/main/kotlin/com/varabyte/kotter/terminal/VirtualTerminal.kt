package com.varabyte.kotter.terminal

import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.substring
import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.kotter.terminal.swing.SgrCodeConverter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
import com.varabyte.kotter.foundation.text.Color as AnsiColor

class TerminalSize(val width: Int, val height: Int) {
    init {
        require(width >= 1 && height >= 1) { "TerminalSize values must both be positive. Got: $width, $height"}
    }
}

private val ANSI_TO_SWING_COLORS = mapOf(
    AnsiColor.BLACK to Color.BLACK,
    AnsiColor.RED to Color.RED.darker(),
    AnsiColor.GREEN to Color.GREEN.darker(),
    AnsiColor.YELLOW to Color.YELLOW.darker(),
    AnsiColor.BLUE to Color.BLUE.darker(),
    AnsiColor.MAGENTA to Color.MAGENTA.darker(),
    AnsiColor.CYAN to Color.CYAN.darker(),
    AnsiColor.WHITE to Color.LIGHT_GRAY,
    AnsiColor.BRIGHT_BLACK to Color.DARK_GRAY,
    AnsiColor.BRIGHT_RED to Color.RED,
    AnsiColor.BRIGHT_GREEN to Color.GREEN,
    AnsiColor.BRIGHT_YELLOW to Color.YELLOW,
    AnsiColor.BRIGHT_BLUE to Color.BLUE,
    AnsiColor.BRIGHT_MAGENTA to Color.MAGENTA,
    AnsiColor.BRIGHT_CYAN to Color.CYAN,
    AnsiColor.BRIGHT_WHITE to Color.WHITE,
)
fun AnsiColor.toSwingColor(): Color = ANSI_TO_SWING_COLORS.getValue(this)

class VirtualTerminal private constructor(private val pane: SwingTerminalPane) : Terminal {
    companion object {
        /**
         * @param terminalSize Number of characters, so 80x32 will be expanded to fit 80 characters horizontally and
         *   32 lines vertically (before scrolling is needed)
         * @param handleInterrupt If true, handle CTRL-C by closing the window.
         */
        fun create(
            title: String = "Virtual Terminal",
            terminalSize: TerminalSize = TerminalSize(100, 40),
            fontSize: Int = 16,
            fgColor: AnsiColor = AnsiColor.WHITE,
            bgColor: AnsiColor = AnsiColor.BLACK,
            handleInterrupt: Boolean = true
        ): VirtualTerminal {
            val pane = SwingTerminalPane(fontSize, fgColor.toSwingColor(), bgColor.toSwingColor())
            pane.focusTraversalKeysEnabled = false // Don't handle TAB, we want to send it to the user
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
            SwingUtilities.invokeLater {
                val frame = JFrame(title)
                frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

                frame.contentPane.add(JScrollPane(terminal.pane).apply {
                    border = EmptyBorder(5, 5, 5, 5)
                    foreground = fgColor.toSwingColor()
                    background = bgColor.toSwingColor()
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
    // Note: For some reason, sometimes the text pane doesn't scroll the bar all the way to the bottom
    private fun BoundedRangeModel.isAtEnd() = value + extent + pane.font.size >= maximum

    private var listenersAdded = false
    private var userVScrollPos: Int? = null
    private var userHScrollPos: Int? = null

    override fun write(text: String) {
        SwingUtilities.invokeLater {
            // Here, we update our text pane causing it to repaint, but as a side effect, this screws with the
            // vscroll and hscroll positions. If the user has intentionally set either of those values themselves,
            // we should fight to keep them.
            val scrollPane = pane.scrollPane!!
            fun updateVScrollPos() {
                userVScrollPos = null
                scrollPane.verticalScrollBar.model.let { model ->
                    if (!model.isAtEnd()) {
                        userVScrollPos = model.value
                    }
                }
            }

            fun updateHScrollPos() {
                userHScrollPos = null
                scrollPane.horizontalScrollBar.model.let { model ->
                    if (model.value > 0) {
                        userHScrollPos = model.value
                    }
                }
            }
            if (!listenersAdded) {
                scrollPane.verticalScrollBar.addAdjustmentListener { evt -> if (evt.valueIsAdjusting) updateVScrollPos() }
                scrollPane.horizontalScrollBar.addAdjustmentListener { evt -> if (evt.valueIsAdjusting) updateHScrollPos() }
                scrollPane.addMouseWheelListener { updateVScrollPos() }

                listenersAdded = true
            }

            pane.processAnsiText(text)

            userVScrollPos?.let {
                SwingUtilities.invokeLater { scrollPane.verticalScrollBar.model.value = it }
            }
            userHScrollPos?.let {
                SwingUtilities.invokeLater { scrollPane.horizontalScrollBar.model.value = it }
            }
        }
    }

    private val charFlow: Flow<Int> by lazy {
        callbackFlow {
            pane.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    val chars: CharSequence = when (e.keyCode) {
                        KeyEvent.VK_UP -> Ansi.Csi.Codes.Keys.UP.toFullEscapeCode()
                        KeyEvent.VK_DOWN -> Ansi.Csi.Codes.Keys.DOWN.toFullEscapeCode()
                        KeyEvent.VK_LEFT -> Ansi.Csi.Codes.Keys.LEFT.toFullEscapeCode()
                        KeyEvent.VK_RIGHT -> Ansi.Csi.Codes.Keys.RIGHT.toFullEscapeCode()
                        KeyEvent.VK_HOME -> Ansi.Csi.Codes.Keys.HOME.toFullEscapeCode()
                        KeyEvent.VK_INSERT -> Ansi.Csi.Codes.Keys.INSERT.toFullEscapeCode()
                        KeyEvent.VK_DELETE -> Ansi.Csi.Codes.Keys.DELETE.toFullEscapeCode()
                        KeyEvent.VK_END -> Ansi.Csi.Codes.Keys.END.toFullEscapeCode()
                        KeyEvent.VK_PAGE_UP -> Ansi.Csi.Codes.Keys.PG_UP.toFullEscapeCode()
                        KeyEvent.VK_PAGE_DOWN -> Ansi.Csi.Codes.Keys.PG_DOWN.toFullEscapeCode()
                        KeyEvent.VK_ENTER -> Ansi.CtrlChars.ENTER.toString()
                        KeyEvent.VK_BACK_SPACE -> Ansi.CtrlChars.BACKSPACE.toString()
                        KeyEvent.VK_TAB -> Ansi.CtrlChars.TAB.toString()
                        KeyEvent.VK_ESCAPE -> Ansi.CtrlChars.ESC.toString()

                        else -> {
                            if (e.isControlDown) {
                                when (e.keyCode) {
                                    KeyEvent.VK_D -> Ansi.CtrlChars.EOF.toString()
                                    else -> ""
                                }
                            }
                            else {
                                e.keyChar.takeIf { it.isDefined() && it.category != CharCategory.CONTROL }?.toString()
                                    ?: ""
                            }
                        }
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
    }
    override fun read(): Flow<Int> = charFlow

    override fun close() {
        SwingUtilities.invokeLater {
            // There should always be two newlines before this final text so this looks good. Append them
            // if they're not there!
            val prependNewlines = "\n".repeat(2 - pane.text.takeLast(2).count { it == '\n' })
            write("$prependNewlines(This terminal session has ended. Press any key to continue.)")
        }
        pane.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                with(pane.window!!) {
                    dispatchEvent(WindowEvent(this, WINDOW_CLOSING))
                }
            }
        })
    }
}


private fun Document.getText() = getText(0, length)

class SwingTerminalPane(fontSize: Int, fgColor: Color, bgColor: Color) : JTextPane() {
    private val sgrCodeConverter: SgrCodeConverter

    init {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, fontSize)
        foreground = fgColor
        background = bgColor
        sgrCodeConverter = SgrCodeConverter(foreground, background)

        clearMouseListeners()
    }

    private fun clearMouseListeners() {
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
                sgrCodeConverter.convert(csiCode)?.let { modifyAttributes ->
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
            clearMouseListeners()
            caretPosition = prevCaret
        }
    }
}