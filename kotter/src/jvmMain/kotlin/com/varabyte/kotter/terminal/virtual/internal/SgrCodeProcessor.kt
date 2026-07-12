package com.varabyte.kotter.terminal.virtual.internal

import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotter.terminal.virtual.toSwingColor
import java.awt.Color
import com.varabyte.kotter.foundation.text.Color as AnsiColor

// Taken from https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit and https://stackoverflow.com/a/27165165
private val IndexedColors by lazy {
    // Range 0 - 15 (legacy colors)
    val legacyColors = listOf(
        AnsiColor.BLACK.toSwingColor(),
        AnsiColor.RED.toSwingColor(),
        AnsiColor.GREEN.toSwingColor(),
        AnsiColor.YELLOW.toSwingColor(),
        AnsiColor.BLUE.toSwingColor(),
        AnsiColor.MAGENTA.toSwingColor(),
        AnsiColor.CYAN.toSwingColor(),
        AnsiColor.WHITE.toSwingColor(),
        AnsiColor.BRIGHT_BLACK.toSwingColor(),
        AnsiColor.BRIGHT_RED.toSwingColor(),
        AnsiColor.BRIGHT_GREEN.toSwingColor(),
        AnsiColor.BRIGHT_YELLOW.toSwingColor(),
        AnsiColor.BRIGHT_BLUE.toSwingColor(),
        AnsiColor.BRIGHT_MAGENTA.toSwingColor(),
        AnsiColor.BRIGHT_CYAN.toSwingColor(),
        AnsiColor.BRIGHT_WHITE.toSwingColor(),
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
    val grayscaleColors = (232..255).associateWith { i ->
        val zeroOffset = i - 232
        val gray = zeroOffset * 10 + 8

        Color(gray, gray, gray)
    }

    legacyColors + coreColors + grayscaleColors
}

/**
 * Handle ANSI SGR codes and convert their instructions into a target [TextStyle].
 */
internal class SgrCodeProcessor(private val textStyle: MutableTextStyle) {
    /**
     * Process the target SGR code and, if possible, convert them into modifications that get applied to the target
     * [textStyle] that this class was constructed with.
     *
     * Returns true if the code was processed, false if unknown to this processor.
     */
    fun process(code: Ansi.Csi.Code): Boolean {
        val attributes: List<TextAttribute>? = when (code) {
            Ansi.Csi.Codes.Sgr.Reset -> listOf(TextAttribute.Clear)

            Ansi.Csi.Codes.Sgr.Colors.Fg.Clear -> listOf(TextAttribute.FgColor(textStyle.defaultFgColor))
            Ansi.Csi.Codes.Sgr.Colors.Fg.Black -> listOf(TextAttribute.FgColor(AnsiColor.BLACK.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.Red -> listOf(TextAttribute.FgColor(AnsiColor.RED.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.Green -> listOf(TextAttribute.FgColor(AnsiColor.GREEN.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.Yellow -> listOf(TextAttribute.FgColor(AnsiColor.YELLOW.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.Blue -> listOf(TextAttribute.FgColor(AnsiColor.BLUE.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.Magenta -> listOf(TextAttribute.FgColor(AnsiColor.MAGENTA.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.Cyan -> listOf(TextAttribute.FgColor(AnsiColor.CYAN.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.White -> listOf(TextAttribute.FgColor(AnsiColor.WHITE.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightBlack -> listOf(TextAttribute.FgColor(AnsiColor.BRIGHT_BLACK.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightRed -> listOf(TextAttribute.FgColor(AnsiColor.BRIGHT_RED.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightGreen -> listOf(TextAttribute.FgColor(AnsiColor.BRIGHT_GREEN.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightYellow -> listOf(TextAttribute.FgColor(AnsiColor.BRIGHT_YELLOW.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightBlue -> listOf(TextAttribute.FgColor(AnsiColor.BRIGHT_BLUE.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightMagenta -> listOf(TextAttribute.FgColor(AnsiColor.BRIGHT_MAGENTA.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightCyan -> listOf(TextAttribute.FgColor(AnsiColor.BRIGHT_CYAN.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightWhite -> listOf(TextAttribute.FgColor(AnsiColor.BRIGHT_WHITE.toSwingColor()))

            Ansi.Csi.Codes.Sgr.Colors.Bg.Clear -> listOf(TextAttribute.BgColor(textStyle.defaultBgColor))
            Ansi.Csi.Codes.Sgr.Colors.Bg.Black -> listOf(TextAttribute.BgColor(AnsiColor.BLACK.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.Red -> listOf(TextAttribute.BgColor(AnsiColor.RED.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.Green -> listOf(TextAttribute.BgColor(AnsiColor.GREEN.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.Yellow -> listOf(TextAttribute.BgColor(AnsiColor.YELLOW.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.Blue -> listOf(TextAttribute.BgColor(AnsiColor.BLUE.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.Magenta -> listOf(TextAttribute.BgColor(AnsiColor.MAGENTA.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.Cyan -> listOf(TextAttribute.BgColor(AnsiColor.CYAN.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.White -> listOf(TextAttribute.BgColor(AnsiColor.WHITE.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightBlack -> listOf(TextAttribute.BgColor(AnsiColor.BRIGHT_BLACK.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightRed -> listOf(TextAttribute.BgColor(AnsiColor.BRIGHT_RED.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightGreen -> listOf(TextAttribute.BgColor(AnsiColor.BRIGHT_GREEN.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightYellow -> listOf(TextAttribute.BgColor(AnsiColor.BRIGHT_YELLOW.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightBlue -> listOf(TextAttribute.BgColor(AnsiColor.BRIGHT_BLUE.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightMagenta -> listOf(TextAttribute.BgColor(AnsiColor.BRIGHT_MAGENTA.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightCyan -> listOf(TextAttribute.BgColor(AnsiColor.BRIGHT_CYAN.toSwingColor()))
            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightWhite -> listOf(TextAttribute.BgColor(AnsiColor.BRIGHT_WHITE.toSwingColor()))

            Ansi.Csi.Codes.Sgr.Colors.Invert -> listOf(TextAttribute.Invert(true))
            Ansi.Csi.Codes.Sgr.Colors.ClearInvert -> listOf(TextAttribute.Invert(false))
            Ansi.Csi.Codes.Sgr.Decorations.Bold -> listOf(TextAttribute.Bold(true))
            Ansi.Csi.Codes.Sgr.Decorations.ClearBold -> listOf(TextAttribute.Bold(false))
            Ansi.Csi.Codes.Sgr.Decorations.Underline -> listOf(TextAttribute.Underline(true))
            Ansi.Csi.Codes.Sgr.Decorations.ClearUnderline -> listOf(TextAttribute.Underline(false))
            Ansi.Csi.Codes.Sgr.Decorations.Strikethrough -> listOf(TextAttribute.Strikethrough(true))
            Ansi.Csi.Codes.Sgr.Decorations.ClearStrikethrough -> listOf(TextAttribute.Strikethrough(false))

            else -> {
                fun Ansi.Csi.Code.optionalCodesToColorOrNull(): Color? {
                    val optionalCodes = parts.optionalCodes ?: return null
                    return if (optionalCodes[0] == Ansi.Csi.Codes.Sgr.Colors.TRUECOLOR_SUBCODE) {
                        Color(optionalCodes[1], optionalCodes[2], optionalCodes[3])
                    } else if (optionalCodes[0] == Ansi.Csi.Codes.Sgr.Colors.LOOKUP_SUBCODE) {
                        IndexedColors[optionalCodes[1]]
                    } else {
                        null
                    }
                }

                when (code.parts.numericCode) {
                    Ansi.Csi.Codes.Sgr.Colors.FG_NUMERIC -> {
                        code.optionalCodesToColorOrNull()?.let { color -> listOf(TextAttribute.FgColor(color)) }
                    }
                    Ansi.Csi.Codes.Sgr.Colors.BG_NUMERIC -> {
                        code.optionalCodesToColorOrNull()?.let { color -> listOf(TextAttribute.BgColor(color)) }
                    }
                    else -> null
                }
            }
        }

        attributes?.forEach { it.applyInto(textStyle) }
        return attributes != null
    }
}
