package com.varabyte.kotterx.compose.terminal

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.substring
import kotlin.math.min

private data class CaretPosition(var lineIndex: Int, var charIndex: Int)

private val EMPTY_ANNOTATION_STRING = AnnotatedString("")

private val NO_BREAK_SPACE = Char(0xA0)

/**
 * Handle incoming text that may contain ANSI commands and convert it to something Compose understands.
 */
internal class AnsiProcessedText(fg: Color, bg: Color) {
    val lines = mutableStateListOf(EMPTY_ANNOTATION_STRING)
    private val lineStartAttrs =
        mutableListOf(MutableTextAttributes().apply { this.fg = fg; this.bg = bg })
    private var currAttr = lineStartAttrs.first()

    private val caretPosition = CaretPosition(0, 0)
    private val sgrCodeConverter = SgrCodeConverter(fg, bg)

    fun process(text: String) {
        var currLine = AnnotatedString.Builder(lines.last())
        val textPtr = TextPtr(text)
        var lastPushedAttr: MutableTextAttributes? = null

        fun finishCurrentLine() {
            lines[lines.lastIndex] = currLine.toAnnotatedString()
            lastPushedAttr = null
        }

        do {
            when (val c = textPtr.currChar) {
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
                    currLine = AnnotatedString.Builder()
                    lastPushedAttr = null
                    currAttr = lineStartAttrs.last()
                    caretPosition.charIndex = 0
                }
                '\n' -> {
                    finishCurrentLine()
                    lineStartAttrs.add(lineStartAttrs.last().copy())
                    lines.add(EMPTY_ANNOTATION_STRING)
                    currLine = AnnotatedString.Builder()
                    caretPosition.lineIndex++
                    caretPosition.charIndex = 0
                }
                Char.MIN_VALUE -> Unit // Ignore the null terminator
                else -> {
                    if (lastPushedAttr != currAttr) {
                        if (lastPushedAttr == null) {
                            // Take a snapshot of the very first style applied to this line; we may need to remember
                            // it later if we repaint it.
                            lineStartAttrs[lineStartAttrs.lastIndex] = currAttr
                        } else {
                            currLine.pop()
                        }
                        currLine.pushStyle(currAttr.toSpanStyle())
                        lastPushedAttr = currAttr.copy()
                    }
                    // Compose tries to be smart about blank space and skips rendering it... but in our case, we
                    // actually want it to render! That's just how the terminal works. So, trick compose by using a
                    // different space character.
                    currLine.append(c.let { if (it == ' ') NO_BREAK_SPACE else it }); caretPosition.charIndex++
                }
            }
        } while (textPtr.increment())

        finishCurrentLine()
    }

    private fun TextAttributes.toSpanStyle(): SpanStyle {
        return SpanStyle(
            color = this.fg ?: Color.Unspecified,
            background = this.bg ?: Color.Unspecified,
            textDecoration = when {
                isUnderlined && isStruckThrough -> TextDecoration.Underline + TextDecoration.LineThrough
                isUnderlined -> TextDecoration.Underline
                isStruckThrough -> TextDecoration.LineThrough
                else -> TextDecoration.None
            },
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Thin,
        )
    }

    private fun processEscapeCode(textPtr: TextPtr): Boolean {
        if (!textPtr.increment()) return false
        return when (textPtr.currChar) {
            Ansi.EscSeq.CSI -> processCsiCode(textPtr)
            else -> false
        }
    }

    private fun processCsiCode(textPtr: TextPtr): Boolean {
        if (!textPtr.increment()) return false

        val csiParts = Ansi.Csi.Code.parts(textPtr) ?: return false
        val csiCode = Ansi.Csi.Code(csiParts)

        val identifier = Ansi.Csi.Identifier.fromCode(csiCode) ?: return false
        return when (identifier) {
            Ansi.Csi.Identifiers.CURSOR_PREV_LINE -> {
                var numLines = min(csiCode.parts.numericCode ?: 1, lines.size)
                while (numLines > 0) {
                    caretPosition.lineIndex--
                    lines.removeLast()
                    lineStartAttrs.removeLast()
                    caretPosition.charIndex = lines.lastOrNull()?.length ?: 0
                    --numLines
                }
                true
            }

            Ansi.Csi.Identifiers.ERASE_LINE -> {
                when (csiCode) {
                    Ansi.Csi.Codes.Erase.CURSOR_TO_LINE_END -> {
                        val erased = lines[caretPosition.lineIndex].subSequence(0, caretPosition.charIndex)
                        lines[caretPosition.lineIndex] = erased
                        true
                    }
                    else -> false
                }
            }
            Ansi.Csi.Identifiers.SGR -> {
                sgrCodeConverter.convert(csiCode)?.let { modifyAttributes ->
                    modifyAttributes(currAttr)
                    true
                } ?: false
            }
            else -> return false
        }
    }
}
