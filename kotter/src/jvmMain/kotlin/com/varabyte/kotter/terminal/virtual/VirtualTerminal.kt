package com.varabyte.kotter.terminal.virtual

import com.varabyte.kotter.runtime.coroutines.KotterDispatchers
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.substring
import com.varabyte.kotter.runtime.terminal.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import java.awt.*
import java.awt.Cursor.HAND_CURSOR
import java.awt.datatransfer.DataFlavor
import java.awt.event.*
import java.awt.event.WindowEvent.WINDOW_CLOSING
import java.awt.geom.Point2D
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.ServiceLoader
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicScrollBarUI
import javax.swing.text.*
import kotlin.io.path.exists
import kotlin.math.roundToInt
import com.varabyte.kotter.foundation.text.Color as AnsiColor

@Deprecated("Use com.varabyte.kotter.runtime.terminal.TerminalSize instead")
typealias TerminalSize = com.varabyte.kotter.runtime.terminal.TerminalSize

// Slightly creamy, less harsh colors.
private val ANSI_TO_SWING_COLORS = mapOf(
    AnsiColor.BLACK to Color.BLACK,
    AnsiColor.RED to Color(0xC65339),
    AnsiColor.GREEN to Color(0x4FAB30),
    AnsiColor.YELLOW to Color(0x9E9C2F),
    AnsiColor.BLUE to Color(0x457AE6),
    AnsiColor.MAGENTA to Color(0xC678DD),
    AnsiColor.CYAN to Color(0x399BA8),
    AnsiColor.WHITE to Color(0xAAAAAA),
    AnsiColor.BRIGHT_BLACK to Color(0x3E4757),
    AnsiColor.BRIGHT_RED to Color(0xEC5A3A),
    AnsiColor.BRIGHT_GREEN to Color(0x77EA51),
    AnsiColor.BRIGHT_YELLOW to Color(0xEFEF53),
    AnsiColor.BRIGHT_BLUE to Color(0x5EA4E6),
    AnsiColor.BRIGHT_MAGENTA to Color(0xEC5AF7),
    AnsiColor.BRIGHT_CYAN to Color(0x78E2EF),
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
class VirtualTerminal private constructor(
    private val pane: SwingTerminalPane, override val width: Int, override val height: Int
) : Terminal {
    private class SleekScrollBarUI(
        private val _trackColor: Color,
        private val _thumbColor: Color
    ) : BasicScrollBarUI() {
        private fun createNonButton() = JButton().apply { preferredSize = Dimension(0, 0) }
        override fun createDecreaseButton(orientation: Int): JButton = createNonButton()
        override fun createIncreaseButton(orientation: Int): JButton = createNonButton()

        override fun configureScrollBarColors() {
            this.trackColor = _trackColor
            this.thumbColor = _thumbColor
        }

        override fun paintThumb(g: Graphics, c: JComponent, thumbBounds: Rectangle) {
            if (thumbBounds.isEmpty || !scrollbar.isEnabled) return

            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                )
                g2.color = thumbColor

                val xMargin: Int
                val yMargin: Int
                if (scrollbar.orientation == JScrollBar.VERTICAL) {
                    xMargin = 4
                    yMargin = 2
                } else {
                    check(scrollbar.orientation == JScrollBar.HORIZONTAL)
                    xMargin = 2
                    yMargin = 4
                }

                val x = thumbBounds.x + xMargin
                val y = thumbBounds.y + yMargin
                val width = thumbBounds.width - (xMargin * 2)
                val height = thumbBounds.height - (yMargin * 2)

                // Use an arc width/height equal to the width of the thumb for a perfect oval
                val arcSize = if (scrollbar.orientation == JScrollBar.VERTICAL) width else height

                g2.fillRoundRect(x, y, width, height, arcSize, arcSize)
            } finally {
                g2.dispose()
            }
        }
    }

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
         * @param hideVerticalScrollbar If true, hide the vertical scrollbar. This is useful if you are explicitly
         *   designing an app that you are sure won't ever go over the height of the terminal (or you really don't care
         *   if there's no thumb indicator to tell you to scroll). When hidden, a small amount of space on the right
         *   side of the terminal will be collapsed, resulting in a slightly tighter fit.
         * @param handleInterrupt If true, handle CTRL-C by closing the window.
         */
        @Suppress("DEPRECATION")
        fun create(
            title: String = "Virtual Terminal",
            terminalSize: TerminalSize = TerminalSize.Default,
            fontSize: Int = 16,
            fontOverride: Path? = null,
            fgColor: AnsiColor = AnsiColor.WHITE,
            bgColor: AnsiColor = AnsiColor.BLACK,
            linkColor: AnsiColor = AnsiColor.CYAN,
            maxNumLines: Int = 1000,
            hideVerticalScrollbar: Boolean = false,
            handleInterrupt: Boolean = true
        ): VirtualTerminal {
            require(terminalSize.width < TerminalSize.Unbounded.width && terminalSize.height < TerminalSize.Unbounded.height) {
                "Neither width nor height in the virtual terminal size can be unbounded. Both must be set explicitly."
            }

            val font = fontOverride?.takeIf { it.exists() }
                ?.let { Font.createFont(Font.TRUETYPE_FONT, it.toFile()).deriveFont(Font.PLAIN, fontSize.toFloat()) }
                ?: Font(Font.MONOSPACED, Font.PLAIN, fontSize)
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

            val terminal = VirtualTerminal(pane, terminalSize.width, terminalSize.height)
            SwingUtilities.invokeAndWait {
                val frame = JFrame(title)
                frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

                frame.contentPane.add(JScrollPane(terminal.pane).apply {
                    border = EmptyBorder(5, 5, 5, 5)
                    foreground = fgColor.toSwingColor()
                    background = bgColor.toSwingColor()
                    viewport.background = background
                    viewportBorder = null
                    border = EmptyBorder(5, 5, 5, 5)

                    val trackColor = bgColor.toSwingColor()
                    val thumbColor = fgColor.toSwingColor().let {
                        Color(it.red, it.green, it.blue, 100)
                    }
                    horizontalScrollBar.setUI(SleekScrollBarUI(trackColor, thumbColor))
                    verticalScrollBar.setUI(SleekScrollBarUI(trackColor, thumbColor))

                    // Our text will autowrap and never go past the right side of the initial terminal window size, so
                    // by default we don't need to show a scrollbar. However, the user can resize the window themselves
                    // and shrink it.
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                    // The default "as needed" scrollbar eats into existing space after it appears, causing lines that
                    // previously perfectly fit to suddenly end up interrupted and wrapped. Instead, we design a
                    // scrollbar UI that essentially is always there but is invisible (since it shares the same bg
                    // color as the regular pane) until the thumb appears.
                    if (!hideVerticalScrollbar) {
                        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                    } else {
                        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
                    }
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
                            val data =
                                Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
                            with(terminal.pane) {
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

            pane.processAnsiText(text, width)

            userVScrollPos?.let {
                SwingUtilities.invokeLater { scrollPane.verticalScrollBar.model.value = it }
            }
            userHScrollPos?.let {
                SwingUtilities.invokeLater { scrollPane.horizontalScrollBar.model.value = it }
            }
        }
    }

    private val charFlow: SharedFlow<Int> by lazy {
        callbackFlow {
            pane.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    val chars: CharSequence = when (e.keyCode) {
                        KeyEvent.VK_UP -> Ansi.Csi.Codes.Keys.Up.toFullEscapeCode()
                        KeyEvent.VK_DOWN -> Ansi.Csi.Codes.Keys.Down.toFullEscapeCode()
                        KeyEvent.VK_LEFT -> Ansi.Csi.Codes.Keys.Left.toFullEscapeCode()
                        KeyEvent.VK_RIGHT -> Ansi.Csi.Codes.Keys.Right.toFullEscapeCode()
                        KeyEvent.VK_HOME -> Ansi.Csi.Codes.Keys.Home.toFullEscapeCode()
                        KeyEvent.VK_INSERT -> Ansi.Csi.Codes.Keys.Insert.toFullEscapeCode()
                        KeyEvent.VK_DELETE -> Ansi.Csi.Codes.Keys.Delete.toFullEscapeCode()
                        KeyEvent.VK_END -> Ansi.Csi.Codes.Keys.End.toFullEscapeCode()
                        KeyEvent.VK_PAGE_UP -> Ansi.Csi.Codes.Keys.PgUp.toFullEscapeCode()
                        KeyEvent.VK_PAGE_DOWN -> Ansi.Csi.Codes.Keys.PgDown.toFullEscapeCode()
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
                            } else {
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
        }.shareIn(CoroutineScope(KotterDispatchers.IO), SharingStarted.Lazily)
    }

    override fun read() = charFlow

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

private class SwingTerminalPane(
    font: Font,
    fgColor: Color,
    bgColor: Color,
    linkColor: Color,
    maxNumLines: Int
) : JTextPane() {

    // Our virtual terminal acts like there is always infinite horizontal space and text elements should never be
    // wrapped. In fact, we handle wrapping outselves (in processAnsiText) so disable Swing's own attempt to do the same
    // thing. (Swing apparently isn't aware of Unicode graphemes, and they force newlines in unexpected places for lines
    // with emoji.)
    // If this class isn't configured correctly, you get very weird horizontal scrollbar behavior, as the way I render
    // elements doesn't match the model Swing has for those same elements.
    private class NoWrapParagraphView(elem: Element) : ParagraphView(elem) {
        override fun layout(width: Int, height: Int) {
            super.layout(Short.MAX_VALUE.toInt(), height)
        }

        override fun getFlowSpan(index: Int): Int {
            return Int.MAX_VALUE
        }

        override fun getMinimumSpan(axis: Int): Float {
            return if (axis == X_AXIS) super.getPreferredSpan(axis) else super.getMinimumSpan(axis)
        }
    }

    override fun getScrollableTracksViewportWidth() = false

    /**
     * A custom component that enforces all text, regardless of font, to conform to a grid.
     *
     * This lets us mix emoji, which come from non-monospace fonts, with the rest of our monospace text.
     */
    private class FixedGridLabelView(
        private val textMetrics: TextMetrics,
        private val emojiRenderers: List<EmojiRenderer>,
        elem: Element,
        private val cellBounds: Point,
    ) : LabelView(elem) {

        private val lineStroke = BasicStroke(1f) // Used for underline / strikethrough

        override fun paint(g: Graphics, allocation: Shape) {
            val g2d = g as Graphics2D
            val doc = document
            val p0 = startOffset
            val p1 = endOffset
            val text = doc.getText(p0, p1 - p0)

            val bounds = allocation.bounds
            var currentX = bounds.x.toFloat()
            val lineMetrics = font.getLineMetrics(text, g2d.fontRenderContext)
            val yBaseline = bounds.y + lineMetrics.ascent

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            g2d.font = font
            background?.let { bgColor ->
                g2d.color = bgColor
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
            }
            g2d.color = foreground

            var currIndex = 0
            while (currIndex < text.length) {
                val graphemeLen = textMetrics.graphemeClusterLengthAt(text, currIndex)
                val grapheme = text.substring(currIndex, currIndex + graphemeLen)

                val numCells = textMetrics.renderWidthOf(text, currIndex, currIndex + graphemeLen)
                val pixelWidth = (numCells * cellBounds.x).toFloat()

                var graphemeRenderHandled = false
                if (emojiRenderers.isNotEmpty() && textMetrics.isEmoji(grapheme)) {
                    for (emojiRenderer in emojiRenderers) {
                        if (emojiRenderer.render(
                            g2d,
                            container as JComponent,
                            grapheme,
                            Rectangle(currentX.roundToInt(), bounds.y, pixelWidth.roundToInt(), bounds.height)
                        )) {
                            graphemeRenderHandled = true
                            break
                        }
                    }
                }

                if (!graphemeRenderHandled) {
                    g2d.drawString(grapheme, currentX, yBaseline)
                }

                if (isUnderline || isStrikeThrough) {
                    g2d.stroke = lineStroke

                    if (isUnderline) {
                        val underlineY = yBaseline + lineMetrics.underlineOffset + lineMetrics.underlineThickness
                        g2d.drawLine(
                            currentX.toInt(),
                            underlineY.toInt(),
                            (currentX + pixelWidth).toInt(),
                            underlineY.toInt()
                        )
                    }

                    if (isStrikeThrough) {
                        val strikeY = yBaseline + lineMetrics.strikethroughOffset
                        g2d.drawLine(
                            currentX.toInt(),
                            strikeY.toInt(),
                            (currentX + pixelWidth).toInt(),
                            strikeY.toInt()
                        )
                    }
                }

                currentX += numCells * cellBounds.x
                currIndex += graphemeLen
            }
        }

        override fun getPreferredSpan(axis: Int): Float {
            return when (axis) {
                X_AXIS -> {
                    val text = document.getText(startOffset, endOffset - startOffset)
                    (textMetrics.renderWidthOf(text) * cellBounds.x).toFloat()
                }
                Y_AXIS -> cellBounds.y.toFloat()
                else -> super.getPreferredSpan(axis)
            }
        }

        // There is no resizing our virtual terminal content. The text area always set to a fixed width for the life of
        // the program. Also, overriding these prevents Swing from using its own non-Unicode/non-Emoji aware logic.
        override fun getMinimumSpan(axis: Int) = getPreferredSpan(axis)
        override fun getMaximumSpan(axis: Int) = getPreferredSpan(axis)
    }

    private class GridEditorKit(private val textMetrics: TextMetrics, private val cellBounds: Point) : StyledEditorKit() {
        private val emojiRenderers = ServiceLoader.load(EmojiRenderer::class.java).toList()
        override fun getViewFactory(): ViewFactory {
            return ViewFactory { elem ->
                when (elem.name) {
                    AbstractDocument.ContentElementName -> FixedGridLabelView(textMetrics, emojiRenderers, elem, cellBounds)
                    AbstractDocument.ParagraphElementName -> NoWrapParagraphView(elem)
                    else -> super@GridEditorKit.getViewFactory().create(elem)
                }
            }
        }
    }

    private class UriState(private val linkColor: Color, private val bgColor: Color) {
        private var currUri: Pair<Int, URI>? = null
        private var prevFgColor: Color? = null
        private var prevBgColor: Color? = null
        private var prevIsUnderlined: Boolean = false
        private val uris = mutableMapOf<Pair<Int, Int>, URI>()

        fun startDefiningUri(index: Int, uri: URI, attrs: MutableAttributeSet) {
            check(currUri == null) { "Attempt to define a new URI without closing an old one." }
            currUri = index to uri

            prevFgColor = StyleConstants.getForeground(attrs)
            prevBgColor = StyleConstants.getBackground(attrs)
            prevIsUnderlined = StyleConstants.isUnderline(attrs)

            StyleConstants.setForeground(attrs, linkColor)
            StyleConstants.setBackground(attrs, bgColor)
            StyleConstants.setUnderline(attrs, true)
        }

        fun finishDefiningUri(index: Int, attrs: MutableAttributeSet) {
            val currUri = currUri
            check(currUri != null) { "Attempt to finish a ULI that was never started" }
            check(currUri.first < index) { "Invalid offset when closing URI" }
            uris[currUri.first to index] = currUri.second

            StyleConstants.setForeground(attrs, prevFgColor)
            StyleConstants.setBackground(attrs, prevBgColor)
            StyleConstants.setUnderline(attrs, prevIsUnderlined)

            this.currUri = null
            prevFgColor = null
            prevBgColor = null
            prevIsUnderlined = false
        }

        fun findUriAt(textIndex: Int): URI? {
            assertValidState()
            uris.forEach { (indices, uri) ->
                val (start, end) = indices
                if (textIndex in start..end) return uri
            }
            return null
        }

        fun assertValidState() {
            check(currUri == null) { "A URI being defined was never finished." }
        }
    }

    private val sgrCodeConverter: SgrCodeConverter
    private val uriState = UriState(linkColor, bgColor)
    private val textMetrics = TextMetrics()
    private val cellBounds: Point

    init {
        with(getFontMetrics(font)) {
            cellBounds = Point(
                charWidth('W'),
                getLineMetrics("W", graphics).height.toInt()
            )
        }

        editorKit = GridEditorKit(textMetrics, cellBounds)
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

        initMouseListeners()
    }

    private fun getWordAtTextIndex(textIndex: Int): String? {
        val text = this.styledDocument.getText()
        if (textIndex < 0 || textIndex >= text.length) return null
        val textPtr = TextPtr(text, textIndex)

        fun Char.isBoundary() = isWhitespace() || isLowSurrogate() || isHighSurrogate()

        if (textPtr.currChar.isBoundary()) return null

        textPtr.incrementUntil { it.isBoundary() }
        val end = textPtr.charIndex

        textPtr.decrementUntil { it.isBoundary()  }
        if (textPtr.currChar.isBoundary()) textPtr.increment() // If not a boundary char, we hit string start; leave it
        val start = textPtr.charIndex
        return textPtr.substring(end - start)
    }

    private fun getUriAtTextIndex(textIndex: Int): URI? {
        return uriState.findUriAt(textIndex) ?: run {
            // If no embedded hyperlink is found, we can still search for raw URLs inside the text
            val wordAtOffset = getWordAtTextIndex(textIndex) ?: return null
            try {
                val uri = wordAtOffset.takeIf { it.isNotBlank() }
                    ?.let { URI(it) }
                    // Sometimes URI accepts strings I wouldn't expect it too; just check if there's a scheme as a way
                    // to make sure it is an actual URL
                    ?.takeIf { it.scheme != null }
                uri
            } catch (_: URISyntaxException) {
                null
            }
        }
    }

    private fun JTextComponent.textIndexAtPoint(pt: Point2D): Int? {
        val col = (pt.x / cellBounds.x).toInt()
        val row = (pt.y / cellBounds.y).toInt()

        val lines = text.lines()

        if (row >= lines.size) return null
        if (col >= textMetrics.renderWidthOf(lines[row])) return null

        var textIndex = 0
        val currLine = lines[row]
        var x = 0
        while (x < col) {
            val graphemeLen = textMetrics.graphemeClusterLengthAt(currLine, textIndex)
            x += textMetrics.renderWidthOf(currLine, textIndex, textIndex + graphemeLen)
            textIndex += graphemeLen
        }
        textIndex += lines.asSequence()
            .take(row) // Take previous rows above us
            .sumOf { it.length + 1 } // +1 since newline was stripped by lines() call

        return textIndex
    }

    override fun getToolTipText(event: MouseEvent): String? {
        val textIndex = textIndexAtPoint(event.point) ?: return null
        val uriUnderCursor = getUriAtTextIndex(textIndex)
        if (uriUnderCursor != null) {
            val word = getWordAtTextIndex(textIndex)
            val uriAsString = uriUnderCursor.toString()
            if (uriAsString != word) {
                return uriAsString
            }
        }

        return null
    }

    private fun initMouseListeners() {
        // The existing mouse handlers set the cursor behind our back which mess with the repainting of the area
        // Let's just disable them for now.
        mouseListeners.toList().forEach { removeMouseListener(it) }
        mouseMotionListeners.toList().forEach { removeMouseMotionListener(it) }
        ToolTipManager.sharedInstance().registerComponent(this@SwingTerminalPane)

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                var nextCursor = Cursor.getDefaultCursor()
                this@SwingTerminalPane.textIndexAtPoint(e.point)?.let { textIndex ->
                    if (getUriAtTextIndex(textIndex) != null) {
                        nextCursor = Cursor.getPredefinedCursor(HAND_CURSOR)
                    }
                }
                cursor = nextCursor
            }
        })

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                this@SwingTerminalPane.textIndexAtPoint(e.point)?.let { textIndex ->
                    getUriAtTextIndex(textIndex)?.let { uriUnderCursor ->
                        Desktop.getDesktop().browse(uriUnderCursor)
                    }
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
            Ansi.Csi.Identifiers.CursorPrevLine -> {
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

            Ansi.Csi.Identifiers.EraseLine -> {
                when (csiCode) {
                    Ansi.Csi.Codes.Erase.CursorToLineEnd -> {
                        with(TextPtr(doc.getText(), caretPosition)) {
                            incrementUntil { it == '\n' }
                            doc.remove(caretPosition, charIndex - caretPosition)
                        }
                        true
                    }

                    else -> false
                }
            }

            Ansi.Csi.Identifiers.Sgr -> {
                sgrCodeConverter.convert(csiCode)?.let { modifyAttributes ->
                    modifyAttributes(attrs)
                    true
                } ?: false
            }

            else -> false
        }
    }

    private fun processOscCode(textPtr: TextPtr, doc: Document, attrs: MutableAttributeSet): Boolean {
        if (!textPtr.increment()) return false

        val oscParts = Ansi.Osc.Code.parts(textPtr) ?: return false
        val oscCode = Ansi.Osc.Code(oscParts)

        val identifier = Ansi.Osc.Identifier.fromCode(oscCode) ?: return false
        return when (identifier) {
            Ansi.Osc.Identifiers.Anchor -> {
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

    fun processAnsiText(text: String, maxWidth: Int) {
        require(SwingUtilities.isEventDispatchThread())
        require(maxWidth > 0)
        if (text.isEmpty()) return

        val doc = styledDocument
        val attrs = SimpleAttributeSet()
        val stringBuilder = StringBuilder()
        fun flush() {
            if (stringBuilder.isEmpty()) return

            // The contents of the string builder may not fit in the current width, so we need to force wrapping in that
            // case. We do this here instead of while we are adding characters to the string builder, because due to
            // Unicode graphemes consisting of multile chars, this is easier to do AFTER all the individual chars have
            // been added.
            run {
                val docText = doc.getText()
                var currLineWidth = 0
                if (docText.isNotEmpty()) {
                    with(TextPtr(docText, docText.length)) {
                        decrementUntil { it == '\n' }
                        // If not true we are at the start of the first line, so no need to step forward
                        if (currChar == '\n') increment()

                        while (remainingLength > 0) {
                            val graphemeLen = textMetrics.graphemeClusterLengthAt(docText, charIndex)
                            currLineWidth += textMetrics.renderWidthOf(docText, charIndex, charIndex + graphemeLen)
                            repeat(graphemeLen) { increment() }
                        }
                    }
                }

                var currIndex = 0
                while (currIndex < stringBuilder.length) {
                    val currChar = stringBuilder[currIndex]
                    if (currChar == '\n') {
                        currLineWidth = 0
                        currIndex++
                    } else {
                        val graphemeLen = textMetrics.graphemeClusterLengthAt(stringBuilder, currIndex)
                        currLineWidth += textMetrics.renderWidthOf(stringBuilder, currIndex, currIndex + graphemeLen)
                        if (currLineWidth > maxWidth) {
                            stringBuilder.insert(currIndex, '\n')
                            currIndex++
                            currLineWidth = 0
                        }
                        currIndex += graphemeLen
                    }
                }
            }

            doc.insertString(caretPosition, stringBuilder.toString(), attrs)
            stringBuilder.clear()
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

                '\n' -> {
                    stringBuilder.append(textPtr.currChar)
                }

                Char.MIN_VALUE -> {
                } // Ignore the null terminator, it's only a TextPtr/Document concept
                else -> {
                    stringBuilder.append(textPtr.currChar)
                }
            }
        } while (textPtr.increment())

        flush()

        uriState.assertValidState()

        revalidate()
        repaint()
    }
}
