@file:OptIn(ExperimentalComposeUiApi::class)

package com.varabyte.kotterx.compose.terminal

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.substring
import com.varabyte.kotter.runtime.terminal.Terminal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.Cursor
import java.nio.file.Path
import com.varabyte.kotter.foundation.text.Color as AnsiColor
import java.awt.event.KeyEvent as AwtKeyEvent

class TerminalSize(val width: Int, val height: Int) {
    init {
        require(width >= 1 && height >= 1) { "TerminalSize values must both be positive. Got: $width, $height" }
    }
}

private fun Color.darker() = Color(red * 0.7f, green * 0.7f, blue * 0.7f)
private fun Color.invert() = Color(1.0f - red, 1.0f - green, 1.0f - blue)

private val ANSI_TO_COMPOSE_COLORS = mapOf(
    AnsiColor.BLACK to Color.Black,
    AnsiColor.RED to Color.Red.darker(),
    AnsiColor.GREEN to Color.Green.darker(),
    AnsiColor.YELLOW to Color.Yellow.darker(),
    AnsiColor.BLUE to Color.Blue.darker(),
    AnsiColor.MAGENTA to Color.Magenta.darker(),
    AnsiColor.CYAN to Color.Cyan.darker(),
    AnsiColor.WHITE to Color.LightGray,
    AnsiColor.BRIGHT_BLACK to Color.DarkGray,
    AnsiColor.BRIGHT_RED to Color.Red,
    AnsiColor.BRIGHT_GREEN to Color.Green,
    AnsiColor.BRIGHT_YELLOW to Color.Yellow,
    AnsiColor.BRIGHT_BLUE to Color.Blue,
    AnsiColor.BRIGHT_MAGENTA to Color.Magenta,
    AnsiColor.BRIGHT_CYAN to Color.Cyan,
    AnsiColor.BRIGHT_WHITE to Color.White,
)

fun AnsiColor.toComposeColor(): Color = ANSI_TO_COMPOSE_COLORS.getValue(this)

private fun KeyEvent.isOnlyCtrlPressed() =
    this.isCtrlPressed && !this.isShiftPressed && !this.isAltPressed && !this.isMetaPressed

private fun KeyEvent.isOnlyShiftPressed() =
    !this.isCtrlPressed && this.isShiftPressed && !this.isAltPressed && !this.isMetaPressed

private fun KeyEvent.noControlKeysPressed() =
    !this.isCtrlPressed && !this.isShiftPressed && !this.isAltPressed && !this.isMetaPressed

/**
 * @param windowState Pass in the window state, which may have a size we can use to bypass the measuring step.
 *
 * In this way, we get an initial measurement when the app first renders, and then the window takes over once it knows
 * about its children sizes in the future. This also handles window resize events, when the user manually changes
 * things.
 */
@Composable
private fun MeasureTerminalArea(
    terminalSize: TerminalSize,
    fontFamily: FontFamily,
    fontSize: Int,
    lineHeight: Int,
    paddingLeftRight: Int,
    paddingTopBottom: Int,
    windowState: WindowState,
    content: @Composable (width: Dp, height: Dp) -> Unit
) {
    SubcomposeLayout { constraints ->
        val contentPlaceable = if (!windowState.size.isSpecified) {
            val charArea = subcompose("dummyChar") {
                MonospaceLine(AnnotatedString("X"), fontFamily, fontSize, lineHeight)
            }.first().measure(constraints)

            subcompose("content") {
                content(
                    (charArea.width * terminalSize.width + 2 * paddingLeftRight).toDp(),
                    (charArea.height * terminalSize.height + 2 * paddingTopBottom).toDp()
                )
            }.first().measure(constraints)
        } else {
            subcompose("content") {
                content(windowState.size.width, windowState.size.height)
            }.first().measure(constraints)
        }

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}

class VirtualTerminal private constructor(
    private val writeChannel: SendChannel<String>,
    private val readChannel: ReceiveChannel<KeyEvent>,
) : Terminal {
    companion object {
        /**
         * @param terminalSize Number of characters, so 80x32 will be expanded to fit 80 characters horizontally and
         *   32 lines vertically (before scrolling is needed)
         * @param maxNumLines The number of text lines to keep before truncating oldest ones. Will be clamped to at
         *   least [TerminalSize.height]. Set to [Int.MAX_VALUE] if you don't want truncation to happen.
         * @param handleInterrupt If true, handle CTRL-C by closing the window.
         */
        fun create(
            title: String = "Virtual Terminal",
            terminalSize: TerminalSize = TerminalSize(100, 40),
            fgColor: AnsiColor = AnsiColor.WHITE,
            bgColor: AnsiColor = AnsiColor.BLACK,
            fontOverride: Path? = null,
            fontSize: Int = 16,
            lineHeight: Int = fontSize,
            paddingLeftRight: Int = 20,
            paddingTopBottom: Int = 10,
            maxNumLines: Int = 1000,
            handleInterrupt: Boolean = true
        ): VirtualTerminal {
            val fontFamily = fontOverride?.let { Font(it.toFile()).toFontFamily() } ?: FontFamily.Monospace

            @Suppress("NAME_SHADOWING")
            val lineHeight = lineHeight.coerceAtLeast(fontSize)

            val writeChannel = Channel<String>()
            val readChannel = Channel<KeyEvent>()

            // Create a thread that will keep the program alive until it exits, and we'll use that to host our Compose UI
            Thread({
                application {
                    var pressAnyKeyToExit by mutableStateOf(false)
                    val processedText =
                        remember { AnsiProcessedText(fgColor.toComposeColor(), bgColor.toComposeColor()) }
                    LaunchedEffect(Unit) {
                        try {
                            while (true) {
                                val text = writeChannel.receive()
                                Snapshot.withMutableSnapshot {
                                    processedText.process(text)
                                    if (processedText.lines.size > maxNumLines) {
                                        processedText.lines.removeRange(0, processedText.lines.size - maxNumLines)
                                    }
                                }
                            }
                        } catch (ignored: Exception) {
                            // Add a bit of padding if necessary. There should be two blank lines between the end text
                            // and our own (since our own text will be appended onto the last blank line)
                            val blankCount = processedText.lines.takeLast(2).count { it.isEmpty() }
                            repeat((2 - blankCount).coerceAtLeast(0)) { processedText.process("\n") }
                            processedText.process("(This terminal session has ended. Press any key to continue.)")
                            pressAnyKeyToExit = true
                        }
                    }

                    val windowState = rememberWindowState(
                        width = Dp.Unspecified,
                        height = Dp.Unspecified,
                        position = WindowPosition(Alignment.Center)
                    )

                    Window(
                        onCloseRequest = ::exitApplication,
                        state = windowState,
                        title = title,
                        onKeyEvent = {
                            if (it.type != KeyEventType.KeyDown) return@Window false
                            if (pressAnyKeyToExit || (handleInterrupt && it.isOnlyCtrlPressed() && it.key == Key.C)) {
                                exitApplication()
                                true
                            } else {
                                runBlocking { readChannel.trySend(it) }
                                true
                            }
                        }
                    ) {
                        MeasureTerminalArea(
                            terminalSize,
                            fontFamily,
                            fontSize,
                            lineHeight,
                            paddingLeftRight,
                            paddingTopBottom,
                            windowState
                        ) { width, height ->
                            TerminalPane(
                                width,
                                height,
                                bgColor.toComposeColor(),
                                fontFamily,
                                fontSize,
                                lineHeight,
                                paddingLeftRight,
                                paddingTopBottom,
                                processedText.lines,
                            )
                        }
                    }
                }
            }, "Compose Main").start()
            return VirtualTerminal(writeChannel, readChannel)
        }
    }

    override fun write(text: String) {
        runBlocking { writeChannel.send(text) }
    }

    private val charFlow: Flow<Int> by lazy {
        callbackFlow {
            try {
                while (true) {
                    val keyEvent = readChannel.receive()
                    val chars: CharSequence = when (keyEvent.key) {
                        Key.DirectionUp -> Ansi.Csi.Codes.Keys.UP.toFullEscapeCode()
                        Key.DirectionDown -> Ansi.Csi.Codes.Keys.DOWN.toFullEscapeCode()
                        Key.DirectionLeft -> Ansi.Csi.Codes.Keys.LEFT.toFullEscapeCode()
                        Key.DirectionRight -> Ansi.Csi.Codes.Keys.RIGHT.toFullEscapeCode()
                        Key.MoveHome -> Ansi.Csi.Codes.Keys.HOME.toFullEscapeCode()
                        Key.Insert -> Ansi.Csi.Codes.Keys.INSERT.toFullEscapeCode()
                        Key.Delete -> Ansi.Csi.Codes.Keys.DELETE.toFullEscapeCode()
                        Key.MoveEnd -> Ansi.Csi.Codes.Keys.END.toFullEscapeCode()
                        Key.PageUp -> Ansi.Csi.Codes.Keys.PG_UP.toFullEscapeCode()
                        Key.PageDown -> Ansi.Csi.Codes.Keys.PG_DOWN.toFullEscapeCode()
                        Key.Enter -> Ansi.CtrlChars.ENTER.toString()
                        Key.Backspace -> Ansi.CtrlChars.BACKSPACE.toString()
                        Key.Tab -> Ansi.CtrlChars.TAB.toString()
                        Key.Escape -> Ansi.CtrlChars.ESC.toString()

                        else -> {
                            if (keyEvent.isOnlyCtrlPressed()) {
                                when (keyEvent.key) {
                                    Key.D -> Ansi.CtrlChars.EOF.toString()
                                    else -> ""
                                }
                            } else {
                                val noControlKeysPressed = keyEvent.noControlKeysPressed()
                                val shiftPressed = keyEvent.isOnlyShiftPressed()
                                if (noControlKeysPressed || shiftPressed) {
                                    val awtKeyEvent = keyEvent.nativeKeyEvent as AwtKeyEvent
                                    awtKeyEvent.keyChar
                                        .takeIf { it.isDefined() && it.category != CharCategory.CONTROL }
                                        ?.toString() ?: ""
                                } else ""
                            }
                        }
                    }
                    chars.forEach { c -> trySend(c.code) }
                }
            } catch (ignored: Exception) {
                // This is expected, when the upstream channel closes
            }

            awaitClose()
        }
    }

    override fun read(): Flow<Int> = charFlow
    override fun close() {
        writeChannel.close()
    }
    // No need to do anything; the virtual terminal starts up empty
    override fun clear() = Unit
}

/**
 * Render a single line in monospace font.
 *
 * Also supports clicking on a URL if detected.
 */
@Composable
private fun MonospaceLine(annotatedText: AnnotatedString, fontFamily: FontFamily, fontSize: Int, lineHeight: Int) {
    val uriHandler = LocalUriHandler.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showPointerCursor by remember { mutableStateOf(false) }

    fun getUriAt(offset: Int): String? {
        val textPtr = TextPtr(annotatedText.text, offset)
        textPtr.incrementUntil { it.isWhitespace() }
        val end = textPtr.charIndex
        textPtr.decrementUntil { it.isWhitespace() }
        val clickedWord = textPtr.substring(end - textPtr.charIndex).trim()
        return if (listOf("http://", "https://").any { clickedWord.startsWith(it, ignoreCase = true) }) {
            clickedWord
        } else null
    }

    ClickableText(
        annotatedText,
        style = TextStyle(
            fontFamily = fontFamily,
            fontSize = fontSize.sp,
            lineHeight = lineHeight.sp,
        ),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .onPointerEvent(PointerEventType.Move) { event ->
                layoutResult?.let { layoutResult ->
                    val offset = layoutResult.getOffsetForPosition(event.changes.first().position)
                    showPointerCursor = getUriAt(offset) != null
                }
            }
            .pointerHoverIcon(PointerIcon(if (showPointerCursor) Cursor(Cursor.HAND_CURSOR) else Cursor.getDefaultCursor())),
        onTextLayout = { layoutResult = it }
    ) { offset -> getUriAt(offset)?.let { uri -> uriHandler.openUri(uri) } }
}

private val SCROLL_SIZE = 8.dp
private fun createScrollbarStyle(color: Color) = defaultScrollbarStyle().copy(
    hoverColor = color.copy(alpha = 0.5f),
    unhoverColor = color.copy(alpha = 0.12f)
)

@Composable
private fun TerminalPane(
    width: Dp,
    height: Dp,
    bg: Color,
    fontFamily: FontFamily,
    fontSize: Int,
    lineHeight: Int,
    paddingLeftRight: Int,
    paddingTopBottom: Int,
    lines: List<AnnotatedString>
) {
    Box(
        Modifier.background(bg)
            .padding(
                start = paddingLeftRight.dp,
                end = paddingLeftRight.dp,
                top = paddingTopBottom.dp,
                bottom = paddingTopBottom.dp
            )
            .width(width)
            .height(height),
    ) {
        val listState = rememberLazyListState()
        var stickToBottom by remember { mutableStateOf(true) }
        LaunchedEffect(listState.layoutInfo.totalItemsCount) {
            if (stickToBottom) listState.scrollToItem(lines.size)
        }

        fun checkStickToBottom() {
            stickToBottom = listState.layoutInfo.visibleItemsInfo.last().index == lines.lastIndex
        }

        val hscroll = rememberScrollState()
        val hscrollAdapter = rememberScrollbarAdapter(hscroll)
        val vscrollAdapter = rememberScrollbarAdapter(listState)
        val interactionSource = remember { MutableInteractionSource() }

        val coroutineScope = rememberCoroutineScope()
        coroutineScope.launch {
            interactionSource.interactions.collectLatest {
                if (it is DragInteraction) checkStickToBottom()
            }
        }

        // Make extra space between the scrollbar and content
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(end = SCROLL_SIZE * 2, bottom = SCROLL_SIZE * 2)
                .horizontalScroll(hscroll)
                .onPointerEvent(PointerEventType.Scroll) { checkStickToBottom() },
            listState
        ) {
            lines.forEach { text ->
                item {
                    MonospaceLine(text, fontFamily, fontSize, lineHeight)
                }
            }
        }
        VerticalScrollbar(
            vscrollAdapter,
            Modifier.fillMaxHeight().padding(bottom = SCROLL_SIZE * 2).align(Alignment.CenterEnd).width(SCROLL_SIZE),
            style = createScrollbarStyle(bg.invert()),
            interactionSource = interactionSource,
        )
        HorizontalScrollbar(
            hscrollAdapter,
            Modifier.fillMaxWidth().padding(end = SCROLL_SIZE * 2).align(Alignment.BottomCenter).height(SCROLL_SIZE),
            style = createScrollbarStyle(bg.invert())
        )
    }
}
