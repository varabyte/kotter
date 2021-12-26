package com.varabyte.konsole.terminal.swing

import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.terminal.toSwingColor
import java.awt.Color
import javax.swing.text.MutableAttributeSet
import javax.swing.text.StyleConstants
import com.varabyte.konsole.foundation.text.Color as AnsiColor

private const val Inverted = "inverted"

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

        println("$i -> $r $g $b")
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
internal class SgrCodeConverter(val defaultForeground: Color, val defaultBackground: Color) {
    fun convert(code: Ansi.Csi.Code): (MutableAttributeSet.() -> Unit)? {
        return when(code) {
            Ansi.Csi.Codes.Sgr.RESET -> { { removeAttributes(this) } }

            Ansi.Csi.Codes.Sgr.Colors.Fg.CLEAR -> { { setInverseAwareForeground(defaultForeground) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.BLACK -> { { setInverseAwareForeground(AnsiColor.BLACK.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.RED -> { { setInverseAwareForeground(AnsiColor.RED.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.GREEN -> { { setInverseAwareForeground(AnsiColor.GREEN.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.YELLOW -> { { setInverseAwareForeground(AnsiColor.YELLOW.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.BLUE -> { { setInverseAwareForeground(AnsiColor.BLUE.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.MAGENTA -> { { setInverseAwareForeground(AnsiColor.MAGENTA.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.CYAN -> { { setInverseAwareForeground(AnsiColor.CYAN.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.WHITE -> { { setInverseAwareForeground(AnsiColor.WHITE.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.BLACK_BRIGHT -> { { setInverseAwareForeground(AnsiColor.BRIGHT_BLACK.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.RED_BRIGHT -> { { setInverseAwareForeground(AnsiColor.BRIGHT_RED.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.GREEN_BRIGHT -> { { setInverseAwareForeground(AnsiColor.BRIGHT_GREEN.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.YELLOW_BRIGHT -> { { setInverseAwareForeground(AnsiColor.BRIGHT_YELLOW.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.BLUE_BRIGHT -> { { setInverseAwareForeground(AnsiColor.BRIGHT_BLUE.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.MAGENTA_BRIGHT -> { { setInverseAwareForeground(AnsiColor.BRIGHT_MAGENTA.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.CYAN_BRIGHT -> { { setInverseAwareForeground(AnsiColor.BRIGHT_CYAN.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Fg.WHITE_BRIGHT -> { { setInverseAwareForeground(AnsiColor.BRIGHT_WHITE.toSwingColor()) } }

            Ansi.Csi.Codes.Sgr.Colors.Bg.CLEAR -> { { setInverseAwareBackground(defaultBackground) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.BLACK -> { { setInverseAwareBackground(AnsiColor.BLACK.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.RED -> { { setInverseAwareBackground(AnsiColor.RED.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.GREEN -> { { setInverseAwareBackground(AnsiColor.GREEN.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.YELLOW -> { { setInverseAwareBackground(AnsiColor.YELLOW.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.BLUE -> { { setInverseAwareBackground(AnsiColor.BLUE.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.MAGENTA -> { { setInverseAwareBackground(AnsiColor.MAGENTA.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.CYAN -> { { setInverseAwareBackground(AnsiColor.CYAN.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.WHITE -> { { setInverseAwareBackground(AnsiColor.WHITE.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.BLACK_BRIGHT -> { { setInverseAwareBackground(AnsiColor.BRIGHT_BLACK.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.RED_BRIGHT -> { { setInverseAwareBackground(AnsiColor.BRIGHT_RED.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.GREEN_BRIGHT -> { { setInverseAwareBackground(AnsiColor.BRIGHT_GREEN.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.YELLOW_BRIGHT -> { { setInverseAwareBackground(AnsiColor.BRIGHT_YELLOW.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.BLUE_BRIGHT -> { { setInverseAwareBackground(AnsiColor.BRIGHT_BLUE.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.MAGENTA_BRIGHT -> { { setInverseAwareBackground(AnsiColor.BRIGHT_MAGENTA.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.CYAN_BRIGHT -> { { setInverseAwareBackground(AnsiColor.BRIGHT_CYAN.toSwingColor()) } }
            Ansi.Csi.Codes.Sgr.Colors.Bg.WHITE_BRIGHT -> { { setInverseAwareBackground(AnsiColor.BRIGHT_WHITE.toSwingColor()) } }

            Ansi.Csi.Codes.Sgr.Colors.INVERT -> { {
                if (getAttribute(Inverted) == null) {
                    invertColors()
                    addAttribute(Inverted, true)
                }
            } }
            Ansi.Csi.Codes.Sgr.Colors.CLEAR_INVERT -> { {
                if (getAttribute(Inverted) != null) {
                    removeAttribute(Inverted)
                    invertColors()
                }
            } }

            Ansi.Csi.Codes.Sgr.Decorations.BOLD -> { { StyleConstants.setBold(this, true) } }
            Ansi.Csi.Codes.Sgr.Decorations.CLEAR_BOLD -> { { removeAttribute(StyleConstants.Bold) } }
            Ansi.Csi.Codes.Sgr.Decorations.UNDERLINE -> { { StyleConstants.setUnderline(this, true) } }
            Ansi.Csi.Codes.Sgr.Decorations.CLEAR_UNDERLINE -> { { removeAttribute(StyleConstants.Underline) } }
            Ansi.Csi.Codes.Sgr.Decorations.STRIKETHROUGH -> { { StyleConstants.setStrikeThrough(this, true) } }
            Ansi.Csi.Codes.Sgr.Decorations.CLEAR_STRIKETHROUGH -> { { removeAttribute(StyleConstants.StrikeThrough) } }

            else -> {
                val optionalCodes = code.parts.optionalCodes ?: return null
                var attrSetModifier: (MutableAttributeSet.() -> Unit)? = null
                if (code.parts.numericCode == Ansi.Csi.Codes.Sgr.Colors.FG_NUMERIC) {
                    val color = if (optionalCodes[0] == Ansi.Csi.Codes.Sgr.Colors.TRUECOLOR_SUBCODE) {
                        Color(optionalCodes[1], optionalCodes[2], optionalCodes[3])
                    } else if (optionalCodes[0] == Ansi.Csi.Codes.Sgr.Colors.LOOKUP_SUBCODE) {
                        IndexedColors[optionalCodes[1]]
                    } else {
                        null
                    }

                    if (color != null) {
                        attrSetModifier = { setInverseAwareForeground(color) }
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
                        attrSetModifier = { setInverseAwareBackground(color) }
                    }
                }

                attrSetModifier
            }
        }
    }

    private fun MutableAttributeSet.invertColors() {
        val prevFg = getInverseAwareForeground()
        val prevBg = getInverseAwareBackground()
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
    private fun MutableAttributeSet.getInverseAwareForeground(): Color {
        return if (getAttribute(Inverted) == true) {
            getAttribute(StyleConstants.Background) as? Color ?: defaultBackground
        } else {
            getAttribute(StyleConstants.Foreground) as? Color ?: defaultForeground
        }
    }
    private fun MutableAttributeSet.getInverseAwareBackground(): Color {
        return if (getAttribute(Inverted) == true) {
            getAttribute(StyleConstants.Foreground) as? Color ?: defaultForeground
        } else {
            getAttribute(StyleConstants.Background) as? Color ?: defaultBackground
        }
    }
}