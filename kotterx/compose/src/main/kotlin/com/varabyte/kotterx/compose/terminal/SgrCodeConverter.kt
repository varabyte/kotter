package com.varabyte.kotterx.compose.terminal

import androidx.compose.ui.graphics.Color
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.foundation.text.Color as AnsiColor

// Taken from https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit and https://stackoverflow.com/a/27165165
private val IndexedColors by lazy {
    // Range 0 - 15 (legacy colors)
    val legacyColors = listOf(
        AnsiColor.BLACK.toComposeColor(),
        AnsiColor.RED.toComposeColor(),
        AnsiColor.GREEN.toComposeColor(),
        AnsiColor.YELLOW.toComposeColor(),
        AnsiColor.BLUE.toComposeColor(),
        AnsiColor.MAGENTA.toComposeColor(),
        AnsiColor.CYAN.toComposeColor(),
        AnsiColor.WHITE.toComposeColor(),
        AnsiColor.BRIGHT_BLACK.toComposeColor(),
        AnsiColor.BRIGHT_RED.toComposeColor(),
        AnsiColor.BRIGHT_GREEN.toComposeColor(),
        AnsiColor.BRIGHT_YELLOW.toComposeColor(),
        AnsiColor.BRIGHT_BLUE.toComposeColor(),
        AnsiColor.BRIGHT_MAGENTA.toComposeColor(),
        AnsiColor.BRIGHT_CYAN.toComposeColor(),
        AnsiColor.BRIGHT_WHITE.toComposeColor(),
    ).mapIndexed { i, color -> i to color }.toMap()

    // Range 16-231: Representative RGB values
    val coreColors = (16..231).associateWith { i ->
        val zeroOffset = i - 16
        val rIndex = zeroOffset / 36
        val gIndex = (zeroOffset % 36) / 6
        val bIndex = zeroOffset % 6

        val r = if (rIndex > 0) 55 + rIndex * 40 else 0
        val g = if (gIndex > 0) 55 + gIndex * 40 else 0
        val b = if (bIndex > 0) 55 + bIndex * 40 else 0

        Color(r, g, b)
    }

    // Range 232 - 255: Grayscale
    val grayscaleColors = (232 .. 255).associateWith { i ->
        val zeroOffset = i - 232
        val gray = zeroOffset * 10 + 8

        Color(gray, gray, gray)
    }

    legacyColors + coreColors + grayscaleColors
}

/**
 * Convert ANSI SGR codes to instructions that Swing can understand.
 */
internal class SgrCodeConverter(private val defaultForeground: Color, private val defaultBackground: Color) {
    fun convert(code: Ansi.Csi.Code): (MutableTextAttributes.() -> Unit)? {
        return when(code) {
            Ansi.Csi.Codes.Sgr.RESET -> { {
                this.clear()
                this._bg = defaultBackground
                this._fg = defaultForeground
            } }

            Ansi.Csi.Codes.Sgr.Colors.Fg.CLEAR -> { { fg = defaultForeground } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.BLACK -> { { fg = AnsiColor.BLACK.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.RED -> { { fg = AnsiColor.RED.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.GREEN -> { { fg = AnsiColor.GREEN.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.YELLOW -> { { fg = AnsiColor.YELLOW.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.BLUE -> { { fg = AnsiColor.BLUE.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.MAGENTA -> { { fg = AnsiColor.MAGENTA.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.CYAN -> { { fg = AnsiColor.CYAN.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.WHITE -> { { fg = AnsiColor.WHITE.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.BLACK_BRIGHT -> { { fg = AnsiColor.BRIGHT_BLACK.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.RED_BRIGHT -> { { fg = AnsiColor.BRIGHT_RED.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.GREEN_BRIGHT -> { { fg = AnsiColor.BRIGHT_GREEN.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.YELLOW_BRIGHT -> { { fg = AnsiColor.BRIGHT_YELLOW.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.BLUE_BRIGHT -> { { fg = AnsiColor.BRIGHT_BLUE.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.MAGENTA_BRIGHT -> { { fg = AnsiColor.BRIGHT_MAGENTA.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.CYAN_BRIGHT -> { { fg = AnsiColor.BRIGHT_CYAN.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.WHITE_BRIGHT -> { { fg = AnsiColor.BRIGHT_WHITE.toComposeColor() } }

            Ansi.Csi.Codes.Sgr.Colors.Bg.CLEAR -> { { bg = defaultBackground } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.BLACK -> { { bg = AnsiColor.BLACK.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.RED -> { { bg = AnsiColor.RED.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.GREEN -> { { bg = AnsiColor.GREEN.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.YELLOW -> { { bg = AnsiColor.YELLOW.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.BLUE -> { { bg = AnsiColor.BLUE.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.MAGENTA -> { { bg = AnsiColor.MAGENTA.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.CYAN -> { { bg = AnsiColor.CYAN.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.WHITE -> { { bg = AnsiColor.WHITE.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.BLACK_BRIGHT -> { { bg = AnsiColor.BRIGHT_BLACK.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.RED_BRIGHT -> { { bg = AnsiColor.BRIGHT_RED.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.GREEN_BRIGHT -> { { bg = AnsiColor.BRIGHT_GREEN.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.YELLOW_BRIGHT -> { { bg = AnsiColor.BRIGHT_YELLOW.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.BLUE_BRIGHT -> { { bg = AnsiColor.BRIGHT_BLUE.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.MAGENTA_BRIGHT -> { { bg = AnsiColor.BRIGHT_MAGENTA.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.CYAN_BRIGHT -> { { bg = AnsiColor.BRIGHT_CYAN.toComposeColor() } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.WHITE_BRIGHT -> { { bg = AnsiColor.BRIGHT_WHITE.toComposeColor() } }

            Ansi.Csi.Codes.Sgr.Colors.INVERT -> { { isInverted = true } }
            Ansi.Csi.Codes.Sgr.Colors.CLEAR_INVERT -> { { isInverted = false } }
            Ansi.Csi.Codes.Sgr.Decorations.BOLD -> { { isBold = true } }
            Ansi.Csi.Codes.Sgr.Decorations.CLEAR_BOLD -> { { isBold = false } }
            Ansi.Csi.Codes.Sgr.Decorations.UNDERLINE -> { { isUnderlined = true } }
            Ansi.Csi.Codes.Sgr.Decorations.CLEAR_UNDERLINE -> { { isUnderlined = false } }
            Ansi.Csi.Codes.Sgr.Decorations.STRIKETHROUGH -> { { isStruckThrough = true } }
            Ansi.Csi.Codes.Sgr.Decorations.CLEAR_STRIKETHROUGH -> { { isStruckThrough = false } }

            else -> {
                val optionalCodes = code.parts.optionalCodes ?: return null
                var textAttrModifier: (MutableTextAttributes.() -> Unit)? = null
                if (code.parts.numericCode == Ansi.Csi.Codes.Sgr.Colors.FG_NUMERIC) {
                    val color = if (optionalCodes[0] == Ansi.Csi.Codes.Sgr.Colors.TRUECOLOR_SUBCODE) {
                        Color(optionalCodes[1], optionalCodes[2], optionalCodes[3])
                    } else if (optionalCodes[0] == Ansi.Csi.Codes.Sgr.Colors.LOOKUP_SUBCODE) {
                        IndexedColors[optionalCodes[1]]
                    } else {
                        null
                    }

                    if (color != null) {
                        textAttrModifier = { fg = color }
                    }
                }
                else if (code.parts.numericCode == Ansi.Csi.Codes.Sgr.Colors.BG_NUMERIC) {
                    val color = if (optionalCodes[0] == Ansi.Csi.Codes.Sgr.Colors.TRUECOLOR_SUBCODE) {
                        Color(optionalCodes[1], optionalCodes[2], optionalCodes[3])
                    } else if (optionalCodes[0] == Ansi.Csi.Codes.Sgr.Colors.LOOKUP_SUBCODE) {
                        IndexedColors[optionalCodes[1]]
                    } else {
                        null
                    }

                    if (color != null) {
                        textAttrModifier = { bg = color }
                    }
                }

                textAttrModifier
            }
        }
    }
}