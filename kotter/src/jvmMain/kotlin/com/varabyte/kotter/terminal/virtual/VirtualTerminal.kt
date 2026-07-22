package com.varabyte.kotter.terminal.virtual

import com.varabyte.kotter.runtime.coroutines.KotterDispatchers
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.substring
import com.varabyte.kotter.runtime.terminal.*
import com.varabyte.kotter.terminal.virtual.internal.*
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
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicScrollBarUI
import kotlin.io.path.exists
import kotlin.math.roundToInt
import com.varabyte.kotter.foundation.text.Color as AnsiColor

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
    AnsiColor.BRIGHT_BLACK to Color(0x666666),
    AnsiColor.BRIGHT_RED to Color(0xEC5A3A),
    AnsiColor.BRIGHT_GREEN to Color(0x77EA51),
    AnsiColor.BRIGHT_YELLOW to Color(0xEFEF53),
    AnsiColor.BRIGHT_BLUE to Color(0x5EA4E6),
    AnsiColor.BRIGHT_MAGENTA to Color(0xEC5AF7),
    AnsiColor.BRIGHT_CYAN to Color(0x78E2EF),
    AnsiColor.BRIGHT_WHITE to Color.WHITE,
)

internal fun AnsiColor.toSwingColor(): Color = ANSI_TO_SWING_COLORS.getValue(this)

private inline fun <reified T> Component.findAncestor(): T? {
    var c: Component? = this
    while (c != null) {
        if (c is T) return c
        c = c.parent
    }
    return null
}

private val Component.window get() = findAncestor<Window>()

private fun Dimension.withInsets(insets: Insets) =
    Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom)

// Data that doesn't require doing a String substring memory allocation
private class LineSection(
    val range: IntRange,
    val renderWidth: Int,
)
private val LineSection.length get() = range.last - range.first + 1

private fun CharSequence.sectionsForWidth(textMetrics: TextMetrics, width: Int): Sequence<LineSection> {
    // Widest grapheme is width 2, so as long as we don't go less than that, we can simplify logic a little
    check(width >= 2)
    val str = this
    return sequence {
        var currIndex = 0
        var startIndex = 0
        var lineRenderWidth = 0
        fun createMetadata() = LineSection(startIndex until currIndex, lineRenderWidth)
        while (currIndex < str.length) {
            val graphemeLen = textMetrics.graphemeClusterLengthAt(str, currIndex)
            val graphemeRenderWidth = textMetrics.renderWidthOf(str, currIndex, currIndex + graphemeLen)
            if (lineRenderWidth + graphemeRenderWidth > width) {
                yield(createMetadata())
                startIndex = currIndex
                lineRenderWidth = 0
            }
            currIndex += graphemeLen
            lineRenderWidth += graphemeRenderWidth
        }
        yield(createMetadata())
    }
}

/**
 * A [Terminal] implementation backed by Swing.
 *
 * This allows us to provide a cross-platform UI window that can always run a Kotter program, which can be especially
 * useful backup if, for some reason, a normal ANSI-featured terminal cannot be created.
 *
 * An instance cannot be created manually. See [VirtualTerminal.create] instead.
 */
class VirtualTerminal private constructor(
    private val pane: TerminalPane, terminalSize: TerminalSize, private val showExitPrompt: Boolean
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
         * @param showExitPrompt If true, show a prompt before the terminal process finishes, telling the user they
         *   should press a key to continue. This also causes a scroll to the bottom of the window. If false, the window
         *   will just exist.
         */
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
            handleInterrupt: Boolean = true,
            showExitPrompt: Boolean = true,
        ): VirtualTerminal {
            require(terminalSize.width < TerminalSize.Unbounded.width && terminalSize.height < TerminalSize.Unbounded.height) {
                "Neither width nor height in the virtual terminal size can be unbounded. Both must be set explicitly."
            }

            val font = fontOverride?.takeIf { it.exists() }
                ?.let { Font.createFont(Font.TRUETYPE_FONT, it.toFile()).deriveFont(Font.PLAIN, fontSize.toFloat()) }
                ?: Font(Font.MONOSPACED, Font.PLAIN, fontSize)
            val pane = TerminalPane(
                terminalSize,
                font,
                fgColor.toSwingColor(),
                bgColor.toSwingColor(),
                linkColor.toSwingColor(),
                maxNumLines.coerceAtLeast(terminalSize.height)
            )
            pane.focusTraversalKeysEnabled = false // Don't handle TAB, we want to send it to the user

            val terminal = VirtualTerminal(pane, terminalSize, showExitPrompt)
            terminal.pane.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    terminal.size = pane.terminalSize
                }
            })

            val vscrollBar = JScrollBar(JScrollBar.VERTICAL).apply {
                val self = this

                val trackColor = bgColor.toSwingColor()
                val thumbColor = fgColor.toSwingColor().let {
                    Color(it.red, it.green, it.blue, 100)
                }
                setUI(SleekScrollBarUI(trackColor, thumbColor))

                fun updateScrollBounds() {
                    val newValue = pane.docViewport.topLineIndex
                    val newExtent = pane.terminalSize.height
                    val newMin = 0
                    val newMax = pane.docViewport.totalLineCount
                    self.setValues(newValue, newExtent, newMin, newMax)
                }

                pane.addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent) {
                        updateScrollBounds()
                    }
                })
                pane.addTextProcessedListener { updateScrollBounds() }
                pane.addMouseWheelListener { e ->
                    val unitsToScroll = e.unitsToScroll

                    val newValue = self.value + unitsToScroll
                    val clampedValue = newValue.coerceIn(self.minimum, self.maximum)
                    self.value = clampedValue
                }

                this.addAdjustmentListener { e ->
                    pane.topLineIndex = e.value
                }
            }

            val windowContent = object : JPanel(BorderLayout()) {
                init {
                    background = bgColor.toSwingColor()
                    border = EmptyBorder(10, 10, 10, 10)
                    add(terminal.pane, BorderLayout.CENTER)
                    if (!hideVerticalScrollbar) add(vscrollBar, BorderLayout.EAST)
                }

                override fun getMinimumSize(): Dimension {
                    val terminalMinSize = terminal.pane.minimumSize
                    val scrollerSize = vscrollBar.preferredSize
                    return Dimension(
                        terminalMinSize.width + scrollerSize.width,
                        terminalMinSize.height,
                    ).withInsets(insets)
                }
            }

            SwingUtilities.invokeAndWait {
                val frame = JFrame(title).apply {
                    background = bgColor.toSwingColor()
                    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                    add(windowContent)
                    pack()
                    setLocationRelativeTo(null)

                    minimumSize = windowContent.minimumSize.withInsets(insets)
                }

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

    override var size = terminalSize
        set(value) {
            if (field != value) {
                field = value
                mutableEvents.sizeChanged.tryEmit(value)
            }
        }

    private var mutableEvents = Terminal.MutableEvents()
    override val events = mutableEvents.asReadOnly()

    override fun write(text: String) {
        SwingUtilities.invokeLater {
            pane.processAnsiText(text, size.width)
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
                    if (chars.isNotEmpty()) {
                        e.consume()
                        // When the user presses a key, focus back to the bottom of the terminal (otherwise, it's weird
                        // if they've scrolled up to the top and can't see what they're typing!)
                        pane.stickToBottom()
                    }
                }
            })

            pane.window!!.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    ToolTipManager.sharedInstance().unregisterComponent(pane)
                    channel.close()
                }
            })

            awaitClose()
        }.shareIn(CoroutineScope(KotterDispatchers.IO), SharingStarted.Lazily)
    }

    override fun read() = charFlow

    override fun close() {
        fun dispatchWindowClosingEvent() {
            with(pane.window!!) {
                dispatchEvent(WindowEvent(this, WINDOW_CLOSING))
            }
        }

        if (showExitPrompt) {
            SwingUtilities.invokeLater {
                // There should always be a blank line before this final text so this looks good. Append newlines to make
                // this happen if they're not there.
                val prependNewlines = "\n".repeat(2 - pane.doc.lines.takeLast(2).count { it.isEmpty() })
                pane.processAnsiText(
                    "$prependNewlines(Application has ended. Press any key to continue.)",
                    size.width,
                    forceScrollToBottom = true
                )
            }
            pane.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    dispatchWindowClosingEvent()
                    e.consume()
                }
            })
        } else {
            dispatchWindowClosingEvent()
        }
    }

    // No need to do anything; the virtual terminal starts up empty
    override fun clear() = Unit
}

private class TerminalPane(
    terminalSize: TerminalSize,
    font: Font,
    fgColor: Color,
    bgColor: Color,
    linkColor: Color,
    maxNumLines: Int
) : JPanel() {

    private class UriState(private val linkColor: Color, private val bgColor: Color) {
        private var currUri: Pair<Int, URI>? = null
        private lateinit var prevFgColor: Color
        private lateinit var prevBgColor: Color
        private var prevIsUnderlined: Boolean = false
        private val uris = mutableMapOf<Pair<Int, Int>, URI>()

        fun startDefiningUri(index: Int, uri: URI, textStyle: MutableTextStyle) {
            check(currUri == null) { "Attempt to define a new URI without closing an old one." }
            currUri = index to uri

            prevFgColor = textStyle.fgColor
            prevBgColor = textStyle.bgColor
            prevIsUnderlined = textStyle.isUnderline

            textStyle.fgColor = linkColor
            textStyle.bgColor = bgColor
            textStyle.isUnderline = true
        }

        fun finishDefiningUri(index: Int, textStyle: MutableTextStyle) {
            val currUri = currUri
            check(currUri != null) { "Attempt to finish a ULI that was never started" }
            check(currUri.first < index) { "Invalid offset when closing URI" }
            uris[currUri.first to index] = currUri.second

            textStyle.fgColor = prevFgColor
            textStyle.bgColor = prevBgColor
            textStyle.isUnderline = prevIsUnderlined
            this.currUri = null
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

    interface Document {
        val lines: List<CharSequence>
        val lineStartIndices: List<Int>
        val styles: DocumentStyles
        val length: Int

        /**
         * Given a global text index into the document, return the line that it exists in, or -1 if not found.
         */
        fun lineContaining(textIndex: Int): Int
    }

    private class MutableDocument(defaultFgColor: Color, defaultBgColor: Color, private val maxNumLines: Int, private val textMetrics: TextMetrics): Document {
        // mutableLines and lineStartIndices kept in sync separately instead of in a map or wrapper object so that
        // override val lines will be efficient
        private val mutableLines = CircularList<StringBuilder>()
        private val mutableLineStartIndices = CircularList<Int>()
        override val lines = mutableLines
        override val lineStartIndices = mutableLineStartIndices
        override val styles = MutableDocumentStyles(defaultFgColor, defaultBgColor)

        // StringBuilder wrapper with caret index (needed by some ANSI commands), with some assumptions that we are
        // editing the very last line in a list of string builders.
        class LastLineEditor(
            private val sb: StringBuilder,
            private val lineStart: Int,
            private val styles: MutableDocumentStyles,
            private val textMetrics: TextMetrics
        ) {
            var cursorIndex = sb.length
                set(value) {
                    field = value.coerceIn(0, sb.length)
                }

            val length get() = sb.length
            fun removeRange(fromInclusive: Int = cursorIndex, toExclusive: Int = sb.length) {
                if (toExclusive > sb.length || fromInclusive !in 0 until toExclusive) return
                sb.delete(fromInclusive, toExclusive)
                if (cursorIndex > fromInclusive) {
                    cursorIndex -= (cursorIndex - fromInclusive)
                }
                styles.removeRange(lineStart + fromInclusive, lineStart + toExclusive)
            }

            /**
             * Add char [c] at [cursorIndex], appending if at the end of the line, and updating its associated style.
             */
            fun add(c: Char, style: TextStyle) {
                if (cursorIndex < sb.lastIndex) {
                    sb[cursorIndex++] = c
                } else {
                    sb.append(c)
                    cursorIndex = sb.length
                }
                styles.put(lineStart + cursorIndex - 1, style)
            }

            /**
             * Move the cursor back to the the start index of the final section AFTER splitting the string by width.
             *
             * In terminals, a user's original text will be auto-wrapped at terminal width boundaries. When an ANSI
             * command directs the user to go back to the beginning of the line, they mean the _visual_ line, not the
             * logical one.
             *
             * Ultimately, this method exists to make it easy to move the cursor to the start of the visual line,
             * rather than the logical one.
             */
            fun moveCursorToLastSectionStart(width: Int) {
                val lastSectionStartIndex = sb.sectionsForWidth(textMetrics, width).last().range.first
                cursorIndex = lastSectionStartIndex
            }
        }

        private var _lastLine: LastLineEditor? = null
        val lastLine
            get() = _lastLine ?: mutableLines.lastOrNull()
                ?.let { LastLineEditor(it, mutableLineStartIndices.last(), styles, textMetrics) }.also { _lastLine = it }
        override val length
            get() = if (mutableLines.isNotEmpty()) {
                mutableLineStartIndices.last() + mutableLines.last().length
            } else 0

        fun addLine(): LastLineEditor {
            fun calculateLineStartFor(i: Int): Int {
                return if (i == 0) 0 else (mutableLineStartIndices[i - 1] + mutableLines[i - 1].length)
            }

            val line = StringBuilder()
            mutableLines.add(line)
            mutableLineStartIndices.add(calculateLineStartFor(mutableLines.lastIndex))

            if (mutableLines.size > maxNumLines) {
                val firstLine = mutableLines.removeFirst()
                val removedLineLength = firstLine.length
                mutableLineStartIndices.removeFirst()
                styles.removeRange(0 until removedLineLength)
                for (i in mutableLineStartIndices.indices) {
                    mutableLineStartIndices[i] -= removedLineLength
                }
            }

            _lastLine = null
            return lastLine!!
        }

        fun removeLast(): Boolean {
            if (mutableLines.isEmpty()) return false
            mutableLines.removeLast()
            val lastLineStart = mutableLineStartIndices.removeLast()
            styles.removeFrom(lastLineStart)
            _lastLine = null
            return true
        }

        override fun lineContaining(textIndex: Int): Int {
            if (textIndex !in 0 until length) return -1
            mutableLineStartIndices.binarySearch(textIndex).let { index ->
                // binarySearch returns (-insertionpoint - 1) if not found, indicating where a value should go;
                // inverting that and subtracting 2 is essentially (index - 1), which gives us the floor. So if we had a
                // list like [0 80 160 240] and the user searched for 100, the method would indicate that we should
                // insert that at index 2, by returning -3. -(-3) - 2 is 1, the index of 80, which is what we wanted.
                return if (index >= 0) index else (-index - 2)
            }
        }
    }

    private fun MutableDocument.add(c: Char, style: TextStyle) {
        val lastLine = lastLine ?: addLine()
        if (c != '\n') {
            lastLine.add(c, style)
        } else {
            addLine()
        }
    }

    interface DocumentViewport {
        /**
         * Information about a line visible to the user.
         *
         * @property startIndex The _global_ index from the whole document that this line starts from. This value can be
         *   used as a base offset allowing the caller to fetch the current text style using [DocumentStyles.at].
         *
         * @property renderWidth The render width of the line is NOT the width in pixels but the result of calling
         *   [TextMetrics.renderWidthOf] on the entire string. To convert it into pixels, you would need to multiple
         *   this value by the width of each cell in your render area.
         */
        class LineInfo(val line: String, val startIndex: Int, val renderWidth: Int)

        val numLines: Int
        val topLineIndex: Int
        val totalLineCount: Int

        /**
         * Return a [LineInfo] associated with the visible line on screen.
         *
         * For example, if [lineIndex] is 2, that will be the third visible row on screen.
         *
         * This could be null if you are asking for a row past the final text in the document, e.g. a view that has
         * five lines of text in a terminal of height 20, and you ask for index 18.
         */
        fun lineInfoFor(lineIndex: Int): LineInfo?
        fun forEach(block: (LineInfo) -> Unit)
    }

    private class MutableDocumentViewport(
        private val doc: Document,
        private val textMetrics: TextMetrics,
        numLines: Int,
        width: Int,
        topLineIndex: Int = 0
    ) : DocumentViewport {
        /**
         * A mapping of a line's final visual index (which autowrap may affect) to its original logical document line
         * index.
         *
         * In other words, the key will always be greater than or equal to the value.
         */
        private val visualToLogicalIndices = TreeMap<Int, Int>()
        private val visualIndexToLineStart = TreeMap<Int, Int>()
        private val lineInfoCache = mutableMapOf<Int, DocumentViewport.LineInfo>()

        override var numLines = numLines
            set(value) {
                if (field != value) {
                    field = value
                    lineInfoCache.clear()
                }
            }

        var width = width
            set(value) {
                if (field != value) {
                    field = value
                    invalidate()
                }
            }

        override var topLineIndex = topLineIndex
            set(value) {
                if (field != value) {
                    field = value
                    lineInfoCache.clear()
                }
            }

        fun invalidate() {
            _totalLineCount = null
            visualToLogicalIndices.clear()
            visualIndexToLineStart.clear()
            lineInfoCache.clear()
        }

        private var _totalLineCount: Int? = null
        override val totalLineCount: Int get() = _totalLineCount ?: run {
            updateVisualIndices()
            _totalLineCount!!
        }

        private fun updateVisualIndices() {
            if (_totalLineCount != null) return
            check(visualToLogicalIndices.isEmpty() && visualIndexToLineStart.isEmpty())
            _totalLineCount = 0
            var visualLineIndex = 0
            visualIndexToLineStart[0] = 0
            doc.lines.forEachIndexed { logicalLineIndex, line ->
                visualToLogicalIndices[visualLineIndex] = logicalLineIndex
                val sections = line.sectionsForWidth(textMetrics, width)
                sections.forEach { section ->
                    // This line has information useful for the next line, so update it ahead of time. This will create
                    // one extra entry for a final extra line that doesn't exist; that's fine
                    visualIndexToLineStart[visualLineIndex + 1] = visualIndexToLineStart.getValue(visualLineIndex) + section.length
                    visualLineIndex++
                }
                // All visual sections correspond to a single logical line which has a newline at the end of it which we
                // account for here.
                visualIndexToLineStart[visualLineIndex] = visualIndexToLineStart.getValue(visualLineIndex)
                _totalLineCount = visualLineIndex
            }
        }

        /**
         * Fetch a [LineInfo] associated with the line index of _all_ text _after_ it was autowrapped.
         *
         */
        private fun lineInfoForGlobalIndex(visualLineIndex: Int): DocumentViewport.LineInfo? {
            check(_totalLineCount != null) // Only call this AFTER calling `updateVisualIndices` first!
            if (visualLineIndex !in 0 until totalLineCount) return null

            var lineInfo = lineInfoCache[visualLineIndex]
            if (lineInfo == null) {
                // if a logical line is so long it is broken up into 3 lines, and we request the 2nd line, and the cache
                // isn't populated yet, we should fill the cache will all 3 lines right now
                val floorVisualLineIndex = visualToLogicalIndices.floorKey(visualLineIndex) ?: return null
                val logicalLineIndex = visualToLogicalIndices.getValue(floorVisualLineIndex)
                val logicalLine = doc.lines[logicalLineIndex]
                val sections = logicalLine.sectionsForWidth(textMetrics, width)
                sections.forEachIndexed { i, section ->
                    val visualLine = logicalLine.substring(section.range)
                    lineInfoCache[floorVisualLineIndex + i] =
                        DocumentViewport.LineInfo(
                            visualLine,
                            // "+ logicalLineIndex" means capture all preceeding newlines from the logical text as well
                            visualIndexToLineStart.getValue(floorVisualLineIndex + i),
                            section.renderWidth,
                        )
                }
                lineInfo = lineInfoCache.getValue(visualLineIndex)
            }
            return lineInfo
        }

        /**
         * Return a [LineInfo] associated with the visible line on screen.
         *
         * For example, if [lineIndex] is 2, that will be the third visible row on screen.
         *
         * This could be null if you are asking for a row past the final text in the document, e.g. a view that has
         * five lines of text in a terminal of height 20, and you ask for index 18.
         */
        override fun lineInfoFor(lineIndex: Int): DocumentViewport.LineInfo? {
            if (lineIndex !in 0 until numLines) return null
            updateVisualIndices()
            return lineInfoForGlobalIndex(topLineIndex + lineIndex)
        }

        override fun forEach(block: (DocumentViewport.LineInfo) -> Unit) {
            updateVisualIndices()
            for (i in topLineIndex until topLineIndex + numLines) {
                val lineInfo = lineInfoForGlobalIndex(i) ?: break
                block(lineInfo)
            }
        }
    }

    private val textProcessedListeners = mutableListOf<() -> Unit>()
    fun addTextProcessedListener(block: () -> Unit) { textProcessedListeners.add(block) }

    var terminalSize = terminalSize
        set(value) {
            if (field != value) {
                field = value
                mutableDocViewport.width = value.width
                mutableDocViewport.numLines = value.height
                repaint()
            }
        }

    var topLineIndex: Int
        get() {
            return docViewport.topLineIndex
        }
        set(value) {
            val clampedValue = value.coerceIn(0, maxTopLineIndex)
            if (docViewport.topLineIndex != clampedValue) {
                mutableDocViewport.topLineIndex = clampedValue
                repaint()
            }
        }

    val maxTopLineIndex get() = (docViewport.totalLineCount - terminalSize.height).coerceAtLeast(0)

    private val textMetrics = TextMetrics()
    private val mutableDoc = MutableDocument(fgColor, bgColor, maxNumLines, textMetrics)
    val doc get(): Document = mutableDoc

    private val uriState = UriState(linkColor, bgColor)
    private val mutableDocViewport = MutableDocumentViewport(doc, textMetrics, numLines = terminalSize.height, width = terminalSize.width)
    val docViewport: DocumentViewport = mutableDocViewport

    /**
     * The cumulative application of styles as we've processed ANSI style commands.
     *
     * Think of it like us having an active pen that at all times has its own color and text decoration state. This
     * state is maintained even when the cursor is moved to a previous position.
     */
    private val activeTextStyle = mutableDoc.styles.createEmptyTextStyle()
    private val sgrCodeProcessor = SgrCodeProcessor(activeTextStyle)
    val cellBounds: Point
    private val boldFont by lazy { font.deriveFont(Font.BOLD) }

    init {
        FontRenderContext(AffineTransform(), true, true).let { frc ->
            val stringBounds = font.getStringBounds("W", frc)
            val lineMetrics = font.getLineMetrics("W", frc)
            cellBounds = Point(
                stringBounds.width.toInt(),
                lineMetrics.height.toInt()
            )
        }

        preferredSize = Dimension(
            terminalSize.width * cellBounds.x,
            terminalSize.height * cellBounds.y
        )

        isFocusable = true
        isOpaque = true
        foreground = fgColor
        background = bgColor
        this.font = font

        initMouseListeners()

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val pane = this@TerminalPane
                val newWidth = (width / cellBounds.x).coerceAtLeast(1)
                val newHeight = (height / cellBounds.y).coerceAtLeast(1)
                if (newWidth != pane.terminalSize.width || newHeight != pane.terminalSize.height) {
                    val wasAtBottom = (pane.isStuckToBottom())
                    pane.terminalSize = TerminalSize(newWidth, newHeight)

                    if (!wasAtBottom) {
                        // If the window got bigger, there is now more space for lines, so it's possible our maxTopLineIndex
                        // changed. Update topLineIndex to respect it.
                        topLineIndex = topLineIndex // Will get clamped if maxTopLineIndex changed; otherwise, no-op
                    } else {
                        stickToBottom()
                    }
                }
            }
        })
    }

    override fun getMinimumSize(): Dimension {
        return Dimension(10 * cellBounds.x, 2 * cellBounds.y)
    }

    private val emojiRenderers = ServiceLoader.load(EmojiRenderer::class.java).toList()
    private val lineStroke by lazy { BasicStroke(1f) } // Used for underline / strikethrough
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val lineMetrics = font.getLineMetrics("W", g2d.fontRenderContext)

        var drawY = visibleRect.y
        docViewport.forEach { lineInfo ->
            var drawX = visibleRect.x
            var charIndex = 0
            textMetrics.graphemesOf(lineInfo.line).forEach { grapheme ->
                val textStyle = doc.styles.at(lineInfo.startIndex + charIndex)
                g2d.font = if (textStyle.isBold) boldFont else font
                val graphemePixelWidth = textMetrics.renderWidthOf(grapheme) * cellBounds.x

                if (textStyle.activeBgColor != background) {
                    g2d.color = textStyle.activeBgColor
                    g2d.fillRect(drawX, drawY, graphemePixelWidth, cellBounds.y)
                }
                g2d.color = textStyle.activeFgColor

                var graphemeRenderHandled = false
                if (emojiRenderers.isNotEmpty() && textMetrics.isEmoji(grapheme)) {
                    for (emojiRenderer in emojiRenderers) {
                        if (emojiRenderer.render(
                            g2d,
                            this@TerminalPane,
                            grapheme,
                                Rectangle(drawX, drawY, graphemePixelWidth, cellBounds.y)
                        )) {
                            graphemeRenderHandled = true
                            break
                        }
                    }
                }
                if (!graphemeRenderHandled) {
                    g2d.drawString(grapheme, drawX, (drawY + lineMetrics.ascent).roundToInt())
                }

                fun drawHorizontalLine(deltaY: Float) {
                    g2d.stroke = lineStroke

                    val x0 = drawX
                    val x1 = drawX + graphemePixelWidth
                    val y = (drawY + lineMetrics.ascent + deltaY).roundToInt()
                    g2d.drawLine(x0, y, x1, y)
                }

                if (textStyle.isUnderline) {
                    drawHorizontalLine(lineMetrics.underlineOffset)
                }
                if (textStyle.isStrikethrough) {
                    drawHorizontalLine(lineMetrics.strikethroughOffset)
                }

                drawX += graphemePixelWidth
                charIndex += grapheme.length
            }
            drawY += cellBounds.y
        }
   }

    fun Point.toLocalCoords(): Point {
        return Point(
            x - visibleRect.x,
            y - visibleRect.y
        )
    }

    private fun getWordAtTextIndex(textIndex: Int): String? {
        val lineIndex = doc.lineContaining(textIndex).takeIf { it >= 0 } ?: return null
        val lineStart = doc.lineStartIndices[lineIndex]
        val line = doc.lines[lineIndex]
        val textPtr = TextPtr(line, textIndex - lineStart)

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

    private fun textIndexAtPoint(pt: Point2D): Int? {
        val row = (pt.y / cellBounds.y).toInt()
        val lineInfo = docViewport.lineInfoFor(row) ?: return null
        val col = (pt.x / cellBounds.x).toInt()

        if (col >= lineInfo.renderWidth) return null

        var textIndex = 0
        val currLine = lineInfo.line
        var x = 0
        while (x < col) {
            val graphemeLen = textMetrics.graphemeClusterLengthAt(currLine, textIndex)
            x += textMetrics.renderWidthOf(currLine, textIndex, textIndex + graphemeLen)
            textIndex += graphemeLen
        }
        textIndex += lineInfo.startIndex // Adjust the textIndex to its global position in the document
        return textIndex
    }

    override fun getToolTipText(event: MouseEvent): String? {
        val textIndex = textIndexAtPoint(event.point.toLocalCoords()) ?: return null
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
        ToolTipManager.sharedInstance().registerComponent(this@TerminalPane)

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                var nextCursor = Cursor.getDefaultCursor()
                this@TerminalPane.textIndexAtPoint(e.point.toLocalCoords())?.let { textIndex ->
                    if (getUriAtTextIndex(textIndex) != null) {
                        nextCursor = Cursor.getPredefinedCursor(HAND_CURSOR)
                    }
                }
                cursor = nextCursor
            }
        })

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                this@TerminalPane.textIndexAtPoint(e.point.toLocalCoords())?.let { textIndex ->
                    getUriAtTextIndex(textIndex)?.let { uriUnderCursor ->
                        Desktop.getDesktop().browse(uriUnderCursor)
                    }
                }
            }
        })
    }

    private fun processEscapeCode(textPtr: TextPtr): Boolean {
        if (!textPtr.increment()) return false
        return when (textPtr.currChar) {
            Ansi.EscSeq.CSI -> processCsiCode(textPtr)
            Ansi.EscSeq.OSC -> processOscCode(textPtr)
            else -> false
        }
    }

    private fun processCsiCode(textPtr: TextPtr): Boolean {
        if (!textPtr.increment()) return false

        val csiParts = Ansi.Csi.Code.parts(textPtr) ?: return false
        val csiCode = Ansi.Csi.Code(csiParts)

        val identifier = Ansi.Csi.Identifier.fromCode(csiCode) ?: return false
        return when (identifier) {
            Ansi.Csi.Identifiers.CursorPrevLine -> {
                // For this command, a value of N means go up N lines from the current line and set the cursor to the
                // start.
                //
                // AAAAAAA
                // BBBBBBB
                // CCCCCCC
                // DDDD|
                //
                // If we get "cursor prev line = 2" at the cursor position, that means we want the end state:
                //
                // AAAAAAA
                // |BBBBBBB
                //
                // which means delete C and D lines and clear B
                //
                // NOTE: Technically we shouldn't delete the lines following the lines we move up from, but we can
                // sidestep that for now because Kotter always follows this command with a line wipe. If we need to
                // revisit this later, we'll cross that bridge when we get to it.
                var numLinesToRemove = (csiCode.parts.numericCode ?: 1)
                while (numLinesToRemove > 0) {
                    val lastLine = mutableDoc.lastLine ?: break
                    if (lastLine.length == 0) {
                        mutableDoc.removeLast()
                    } else {
                        // If our cursor index is past the end of the line, that essentially means we are on the other
                        // side of a visual newline, and on that side is an empty string. This happens when we have just
                        // finished erasing a visual line. e.g. say we had "ABCDEFGH" with width 4, and we just wiped
                        // out "EFGH", so our cursor is still on index 5 with the text set to "ABCD". In that case,
                        // there's nothing to remove -- just move the cursor and we're done.
                        val removeRequired = lastLine.cursorIndex < lastLine.length
                        lastLine.moveCursorToLastSectionStart(terminalSize.width)
                        if (removeRequired) { lastLine.removeRange() }
                    }
                    --numLinesToRemove
                }
                true
            }

            Ansi.Csi.Identifiers.EraseLine -> {
                when (csiCode) {
                    Ansi.Csi.Codes.Erase.CursorToLineEnd -> {
                        mutableDoc.lastLine?.removeRange()
                        true
                    }

                    else -> false
                }
            }

            Ansi.Csi.Identifiers.Sgr -> {
                sgrCodeProcessor.process(csiCode)
            }

            else -> false
        }
    }

    private fun processOscCode(textPtr: TextPtr): Boolean {
        if (!textPtr.increment()) return false

        val oscParts = Ansi.Osc.Code.parts(textPtr) ?: return false
        val oscCode = Ansi.Osc.Code(oscParts)

        val identifier = Ansi.Osc.Identifier.fromCode(oscCode) ?: return false
        return when (identifier) {
            Ansi.Osc.Identifiers.Anchor -> {
                // Anchor spec is `;(anchor-params);(uri)` if starting a URI block or `;;` if finishing one
                val uriPart = oscCode.parts.params[1].takeIf { it.isNotBlank() }
                if (uriPart != null) {
                    uriState.startDefiningUri(doc.length, URI(uriPart), activeTextStyle)
                } else {
                    uriState.finishDefiningUri(doc.length, activeTextStyle)
                }

                true
            }

            else -> false
        }
    }

    fun processAnsiText(text: String, maxWidth: Int, forceScrollToBottom: Boolean = false) {
        require(SwingUtilities.isEventDispatchThread())
        require(maxWidth > 0)
        if (text.isEmpty()) return

        // The following logic will keep the window snapped at the bottom
        val wasAtBottom = isStuckToBottom()

        val textPtr = TextPtr(text)
        do {
            when (textPtr.currChar) {
                Ansi.CtrlChars.ESC -> {
                    val prevCharIndex = textPtr.charIndex
                    if (!processEscapeCode(textPtr)) {
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
                    mutableDoc.lastLine?.moveCursorToLastSectionStart(terminalSize.width)
                }

                Char.MIN_VALUE -> {
                    // Ignore the null terminator, it's only a TextPtr concept
                }

                else -> {
                    mutableDoc.add(textPtr.currChar, activeTextStyle)
                }
            }
        } while (textPtr.increment())

        uriState.assertValidState()
        mutableDocViewport.invalidate()

        revalidate()
        repaint()

        if (wasAtBottom || forceScrollToBottom) {
            stickToBottom()
        }

        textProcessedListeners.forEach { it() }
    }
}

private fun TerminalPane.stickToBottom() { topLineIndex = maxTopLineIndex }
private fun TerminalPane.isStuckToBottom() = topLineIndex == maxTopLineIndex
