package com.varabyte.kotter.terminal.virtual

import com.varabyte.kotter.runtime.internal.ansi.*
import java.awt.Color
import javax.swing.text.MutableAttributeSet
import javax.swing.text.StyleConstants
import com.varabyte.kotter.foundation.text.Color as AnsiColor

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
 * Convert ANSI SGR codes to instructions that Swing can understand.
 */
internal class SgrCodeConverter(private val defaultForeground: Color, private val defaultBackground: Color) {
    fun convert(code: Ansi.Csi.Code): (MutableAttributeSet.() -> Unit)? {
        return when (code) {
            Ansi.Csi.Codes.Sgr.Reset -> {
                { removeAttributes(this) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.Clear -> {
                { setInverseAwareForeground(defaultForeground) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.Black -> {
                { setInverseAwareForeground(AnsiColor.BLACK.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.Red -> {
                { setInverseAwareForeground(AnsiColor.RED.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.Green -> {
                { setInverseAwareForeground(AnsiColor.GREEN.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.Yellow -> {
                { setInverseAwareForeground(AnsiColor.YELLOW.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.Blue -> {
                { setInverseAwareForeground(AnsiColor.BLUE.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.Magenta -> {
                { setInverseAwareForeground(AnsiColor.MAGENTA.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.Cyan -> {
                { setInverseAwareForeground(AnsiColor.CYAN.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.White -> {
                { setInverseAwareForeground(AnsiColor.WHITE.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightBlack -> {
                { setInverseAwareForeground(AnsiColor.BRIGHT_BLACK.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightRed -> {
                { setInverseAwareForeground(AnsiColor.BRIGHT_RED.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightGreen -> {
                { setInverseAwareForeground(AnsiColor.BRIGHT_GREEN.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightYellow -> {
                { setInverseAwareForeground(AnsiColor.BRIGHT_YELLOW.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightBlue -> {
                { setInverseAwareForeground(AnsiColor.BRIGHT_BLUE.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightMagenta -> {
                { setInverseAwareForeground(AnsiColor.BRIGHT_MAGENTA.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightCyan -> {
                { setInverseAwareForeground(AnsiColor.BRIGHT_CYAN.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Fg.BrightWhite -> {
                { setInverseAwareForeground(AnsiColor.BRIGHT_WHITE.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.Clear -> {
                { setInverseAwareBackground(defaultBackground) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.Black -> {
                { setInverseAwareBackground(AnsiColor.BLACK.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.Red -> {
                { setInverseAwareBackground(AnsiColor.RED.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.Green -> {
                { setInverseAwareBackground(AnsiColor.GREEN.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.Yellow -> {
                { setInverseAwareBackground(AnsiColor.YELLOW.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.Blue -> {
                { setInverseAwareBackground(AnsiColor.BLUE.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.Magenta -> {
                { setInverseAwareBackground(AnsiColor.MAGENTA.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.Cyan -> {
                { setInverseAwareBackground(AnsiColor.CYAN.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.White -> {
                { setInverseAwareBackground(AnsiColor.WHITE.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightBlack -> {
                { setInverseAwareBackground(AnsiColor.BRIGHT_BLACK.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightRed -> {
                { setInverseAwareBackground(AnsiColor.BRIGHT_RED.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightGreen -> {
                { setInverseAwareBackground(AnsiColor.BRIGHT_GREEN.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightYellow -> {
                { setInverseAwareBackground(AnsiColor.BRIGHT_YELLOW.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightBlue -> {
                { setInverseAwareBackground(AnsiColor.BRIGHT_BLUE.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightMagenta -> {
                { setInverseAwareBackground(AnsiColor.BRIGHT_MAGENTA.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightCyan -> {
                { setInverseAwareBackground(AnsiColor.BRIGHT_CYAN.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Bg.BrightWhite -> {
                { setInverseAwareBackground(AnsiColor.BRIGHT_WHITE.toSwingColor()) }
            }

            Ansi.Csi.Codes.Sgr.Colors.Invert -> {
                {
                    if (getAttribute(Inverted) == null) {
                        invertColors()
                        addAttribute(Inverted, true)
                    }
                }
            }

            Ansi.Csi.Codes.Sgr.Colors.ClearInvert -> {
                {
                    if (getAttribute(Inverted) != null) {
                        removeAttribute(Inverted)
                        invertColors()
                    }
                }
            }

            Ansi.Csi.Codes.Sgr.Decorations.Bold -> {
                { StyleConstants.setBold(this, true) }
            }

            Ansi.Csi.Codes.Sgr.Decorations.ClearBold -> {
                { removeAttribute(StyleConstants.Bold) }
            }

            Ansi.Csi.Codes.Sgr.Decorations.Underline -> {
                { StyleConstants.setUnderline(this, true) }
            }

            Ansi.Csi.Codes.Sgr.Decorations.ClearUnderline -> {
                { removeAttribute(StyleConstants.Underline) }
            }

            Ansi.Csi.Codes.Sgr.Decorations.Strikethrough -> {
                { StyleConstants.setStrikeThrough(this, true) }
            }

            Ansi.Csi.Codes.Sgr.Decorations.ClearStrikethrough -> {
                { removeAttribute(StyleConstants.StrikeThrough) }
            }

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
                } else if (code.parts.numericCode == Ansi.Csi.Codes.Sgr.Colors.BG_NUMERIC) {
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
        } else {
            StyleConstants.setForeground(this, color)
        }
    }

    private fun MutableAttributeSet.setInverseAwareBackground(color: Color) {
        if (getAttribute(Inverted) == true) {
            StyleConstants.setForeground(this, color)
        } else {
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
