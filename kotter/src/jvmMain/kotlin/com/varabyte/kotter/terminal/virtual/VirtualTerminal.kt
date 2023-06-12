package com.varabyte.kotter.terminal.virtual

import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.substring
import com.varabyte.kotter.runtime.terminal.Terminal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.awt.*
import java.awt.Cursor.HAND_CURSOR
import java.awt.datatransfer.DataFlavor
import java.awt.event.*
import java.awt.event.WindowEvent.WINDOW_CLOSING
import java.awt.geom.Point2D
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.text.*
import kotlin.io.path.exists
import com.varabyte.kotter.foundation.text.Color as AnsiColor

/**
 * The size of the virtual terminal text area, where [width] and [height] represent the number of characters that can
 * fit within it.
 *
 * In other words, 80x32 means 80 characters wide by 32 lines tall, as opposed to 80 pixels by 32 pixels.
 */
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

internal fun AnsiColor.toSwingColor(): Color = ANSI_TO_SWING_COLORS.getValue(this)

/**
 * A [Terminal] implementation backed by Swing.
 *
 * This allows us to provide a cross-platform UI window that can always run a Kotter program, which can be especially
 * useful backup if, for some reason, a normal ANSI-featured terminal cannot be created.
 *
 * An instance cannot be created manually. See [VirtualTerminal.create] instead.
 */
class VirtualTerminal private constructor(private val pane: SwingTerminalPane) : Terminal {
    companion object {
        /**
         * Factory method for constructing a [VirtualTerminal].
         *
         * @param title The text to use for the terminal window's title bar.
         * @param terminalSize Number of characters, so 80x32 will be expanded to fit 80 characters horizontally and
         *   32 lines vertically (before scrolling is needed)
         * @param fontSize The size to use for the font used by this virtual terminal.
         * @param fontOverride A path to a font file resource (e.g. ttf) for an alternate text look and feel.
         * @param fgColor The color to use for the font text.
         * @param bgColor The color to use for the virtual terminal background color.
         * @param linkColor The color to use for links.
         * @param maxNumLines The number of text lines to keep before truncating the oldest ones. This can be useful to
         *   ensure that this program won't eventually run out of memory if you keep appending text in a loop forever.
         *   This value Will be clamped to at least [TerminalSize.height]. Set to [Int.MAX_VALUE] if you don't want
         *   truncation to happen.
         * @param handleInterrupt If true, handle CTRL-C by closing the window.
         */
        fun create(
            title: String = "Virtual Terminal",
            terminalSize: TerminalSize = TerminalSize(100, 40),
            fontSize: Int = 16,
            fontOverride: Path? = null,
            fgColor: AnsiColor = AnsiColor.WHITE,
            bgColor: AnsiColor = AnsiColor.BLACK,
            linkColor: AnsiColor = AnsiColor.CYAN,
            maxNumLines: Int = 1000,
            handleInterrupt: Boolean = true
        ): VirtualTerminal {
            val font = fontOverride?.takeIf { it.exists() }?.let { Font.createFont(Font.TRUETYPE_FONT, it.toFile()).deriveFont(Font.PLAIN, fontSize.toFloat()) } ?: Font(Font.MONOSPACED, Font.PLAIN, fontSize)
            val pane = SwingTerminalPane(
                font,
                fgColor.toSwingColor(),
                bgColor.toSwingColor(),
                linkColor.toSwingColor(),
                maxNumLines.coerceAtLeast(terminalSize.height)
            )
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
            SwingUtilities.invokeAndWait {
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

                terminal.pane.addKeyListener(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if (e.isControlDown && e.keyCode == KeyEvent.VK_V) {
                            val data = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
                            with (terminal.pane) {
                                if (data != null && this.hasFocus()) {
                                    data.trim().forEach { c ->
                                        dispatchEvent(
                                            KeyEvent(
                                                this,
                                                KeyEvent.KEY_PRESSED,
                                                0,
                                                0,
                                                KeyEvent.getExtendedKeyCodeForChar(c.code),
                                                c,
                                                KeyEvent.KEY_LOCATION_STANDARD
                                            )
                                        )
                                    }
                                }
                            }
                            e.consume()
                        }
                    }
                })

                // No tooltip delay looks way better when hovering over URLs
                ToolTipManager.sharedInstance().initialDelay = 0

                frame.isVisible = true
            }

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

    override val width = Int.MAX_VALUE // Virtual terminals can scroll horizontally forever
    override val height = Int.MAX_VALUE // Virtual terminals can scroll vertically forever

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

    // No need to do anything; the virtual terminal starts up empty
    override fun clear() = Unit
}


private fun Document.getText() = getText(0, length)

private class SwingTerminalPane(font: Font, fgColor: Color, bgColor: Color, linkColor: Color, maxNumLines: Int) : JTextPane() {
    private class UriState(private val linkColor: Color, private val bgColor: Color) {
        private var currUri: Pair<Int, URI>? = null
        private var prevFgColor: Color? = null
        private var prevBgColor: Color? = null
        private var prevIsUnderlined: Boolean = false
        private val uris = mutableMapOf<Pair<Int, Int>, URI>()

        fun startDefiningUri(offset: Int, uri: URI, attrs: MutableAttributeSet) {
            check(currUri == null) { "Attempt to define a new URI without closing an old one." }
            currUri = offset to uri

            prevFgColor = StyleConstants.getForeground(attrs)
            prevBgColor = StyleConstants.getBackground(attrs)
            prevIsUnderlined = StyleConstants.isUnderline(attrs)

            StyleConstants.setForeground(attrs, linkColor)
            StyleConstants.setBackground(attrs, bgColor)
            StyleConstants.setUnderline(attrs, true)
        }

        fun finishDefiningUri(offset: Int, attrs: MutableAttributeSet) {
            val currUri = currUri
            check(currUri != null) { "Attempt to finish a ULI that was never started" }
            check(currUri.first < offset) { "Invalid offset when closing URI" }
            uris[currUri.first to offset] = currUri.second

            StyleConstants.setForeground(attrs, prevFgColor)
            StyleConstants.setBackground(attrs, prevBgColor)
            StyleConstants.setUnderline(attrs, prevIsUnderlined)

            this.currUri = null
            prevFgColor = null
            prevBgColor = null
            prevIsUnderlined = false
        }

        fun findUriAt(offset: Int): URI? {
            assertValidState()
            uris.forEach { (offsets, uri) ->
                val (start, end) = offsets
                if (offset in start..end) return uri
            }
            return null
        }

        fun assertValidState() {
            check(currUri == null) { "A URI being defined was never finished." }
        }
    }

    private val sgrCodeConverter: SgrCodeConverter
    private val uriState = UriState(linkColor, bgColor)

    init {
        isEditable = false
        foreground = fgColor
        background = bgColor
        this.font = font
        sgrCodeConverter = SgrCodeConverter(foreground, background)

        (styledDocument as AbstractDocument).documentFilter = object : DocumentFilter() {
            override fun insertString(fb: FilterBypass, offset: Int, string: String, attr: AttributeSet) {
                super.insertString(fb, offset, string, attr)
                val rootElement = styledDocument.defaultRootElement
                val numLines = rootElement.elementCount
                if (numLines > maxNumLines) {
                    val lastLineIndex = numLines - maxNumLines - 1
                    val lastLineOffset = rootElement.getElement(lastLineIndex).startOffset
                    remove(fb, 0, lastLineOffset)
                }
            }
        }

        resetMouseListeners()
    }

    private fun getWordAtOffset(offset: Int): String {
        val textPtr = TextPtr(styledDocument.getText(), offset)

        textPtr.incrementUntil { it.isWhitespace() }
        val end = textPtr.charIndex

        textPtr.decrementUntil { it.isWhitespace() }
        val start = textPtr.charIndex
        return textPtr.substring(end - start)
    }

    private fun getUriAtOffset(offset: Int): URI? {
        return uriState.findUriAt(offset) ?: run {
            val wordAtOffset = getWordAtOffset(offset)
            try {
                wordAtOffset.takeIf { it.isNotBlank() }?.let { URI(it) }
            } catch (ignored: URISyntaxException) {
                null
            }
        }
    }

    // viewToModel2D is not available in JDK8, so backport it
    @Suppress("DEPRECATION") // viewToModel2d not available in JDK8
    private fun JTextComponent.viewToModel2D_jdk8(pt: Point2D): Int {
        // In our limited use of viewToModel2D, its behavior is identical to viewToModel
        return viewToModel(pt as Point)
    }

    override fun getToolTipText(event: MouseEvent): String? {
        val offset = viewToModel2D_jdk8(event.point)
        val uriUnderCursor = getUriAtOffset(offset)
        if (uriUnderCursor != null) {
            val word = getWordAtOffset(offset)
            val uriAsString = uriUnderCursor.toString()
            if (uriAsString != word) {
                return uriAsString
            }
        }

        return null
    }

    private fun resetMouseListeners() {
        // The existing mouse handlers set the cursor behind our back which mess with the repainting of the area
        // Let's just disable them for now.
        mouseListeners.toList().forEach { removeMouseListener(it) }
        mouseMotionListeners.toList().forEach { removeMouseMotionListener(it) }
        ToolTipManager.sharedInstance().registerComponent(this@SwingTerminalPane)

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val uriUnderCursor = getUriAtOffset(this@SwingTerminalPane.viewToModel2D_jdk8(e.point))
                cursor = if (uriUnderCursor != null) {
                    Cursor.getPredefinedCursor(HAND_CURSOR)
                } else {
                    Cursor.getDefaultCursor()
                }
            }
        })

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                getUriAtOffset(this@SwingTerminalPane.viewToModel2D_jdk8(e.point))?.let { uriUnderCursor ->
                    Desktop.getDesktop().browse(uriUnderCursor)
                }
            }
        })
    }

    private fun processEscapeCode(textPtr: TextPtr, doc: Document, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.increment()) return false
        return when (textPtr.currChar) {
            Ansi.EscSeq.CSI -> processCsiCode(textPtr, doc, attrs)
            Ansi.EscSeq.OSC -> processOscCode(textPtr, doc, attrs)
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

    private fun processOscCode(textPtr: TextPtr, doc: Document, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.increment()) return false

        val oscParts = Ansi.Osc.Code.parts(textPtr) ?: return false
        val oscCode = Ansi.Osc.Code(oscParts)

        val identifier = Ansi.Osc.Identifier.fromCode(oscCode) ?: return false
        return when (identifier) {
            Ansi.Osc.Identifiers.ANCHOR -> {
                // Anchor spec is `;(anchor-params);(uri)` if starting a URI block or `;;` if finishing one
                val uriPart = oscCode.parts.params[1].takeIf { it.isNotBlank() }
                if (uriPart != null) {
                    uriState.startDefiningUri(doc.length, URI(uriPart), attrs)
                } else {
                    uriState.finishDefiningUri(doc.length, attrs)
                }

                true
            }
            else -> false
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

        uriState.assertValidState()

        // Hack alert: I'm not sure why, but calling updateUI is the only consistent way I've been able to get the text
        // pane to refresh its text contents without stuttering. However, this sometimes affects the caret position? So
        // we reset it back.
        caretPosition.let { prevCaret ->
            updateUI()
            resetMouseListeners()
            caretPosition = prevCaret
        }
    }
}