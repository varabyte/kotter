package com.varabyte.konsole.terminal.swing

import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.terminal.toSwingColor
import java.awt.Color
import javax.swing.text.MutableAttributeSet
import javax.swing.text.StyleConstants
import com.varabyte.konsole.foundation.text.Color as AnsiColor

private const val Inverted = "inverted"

internal class SgrCodeToAttrModifiers(val defaultForeground: Color, val defaultBackground: Color) {
    private val SGR_CODE_TO_ATTR_MODIFIER = mapOf<Ansi.Csi.Code, MutableAttributeSet.() -> Unit>(
        Ansi.Csi.Codes.Sgr.RESET to { removeAttributes(this) },

        Ansi.Csi.Codes.Sgr.Colors.Fg.CLEAR to { setInverseAwareForeground(defaultForeground) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.BLACK to { setInverseAwareForeground(AnsiColor.BLACK.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.RED to { setInverseAwareForeground(AnsiColor.RED.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.GREEN to { setInverseAwareForeground(AnsiColor.GREEN.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.YELLOW to { setInverseAwareForeground(AnsiColor.YELLOW.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.BLUE to { setInverseAwareForeground(AnsiColor.BLUE.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.MAGENTA to { setInverseAwareForeground(AnsiColor.MAGENTA.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.CYAN to { setInverseAwareForeground(AnsiColor.CYAN.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.WHITE to { setInverseAwareForeground(AnsiColor.WHITE.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.BLACK_BRIGHT to { setInverseAwareForeground(AnsiColor.BRIGHT_BLACK.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.RED_BRIGHT to { setInverseAwareForeground(AnsiColor.BRIGHT_RED.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.GREEN_BRIGHT to { setInverseAwareForeground(AnsiColor.BRIGHT_GREEN.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.YELLOW_BRIGHT to { setInverseAwareForeground(AnsiColor.BRIGHT_YELLOW.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.BLUE_BRIGHT to { setInverseAwareForeground(AnsiColor.BRIGHT_BLUE.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.MAGENTA_BRIGHT to { setInverseAwareForeground(AnsiColor.BRIGHT_MAGENTA.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.CYAN_BRIGHT to { setInverseAwareForeground(AnsiColor.BRIGHT_CYAN.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Fg.WHITE_BRIGHT to { setInverseAwareForeground(AnsiColor.BRIGHT_WHITE.toSwingColor()) },

        Ansi.Csi.Codes.Sgr.Colors.Bg.CLEAR to { setInverseAwareBackground(defaultBackground) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.BLACK to { setInverseAwareBackground(AnsiColor.BLACK.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.RED to { setInverseAwareBackground(AnsiColor.RED.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.GREEN to { setInverseAwareBackground(AnsiColor.GREEN.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.YELLOW to { setInverseAwareBackground(AnsiColor.YELLOW.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.BLUE to { setInverseAwareBackground(AnsiColor.BLUE.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.MAGENTA to { setInverseAwareBackground(AnsiColor.MAGENTA.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.CYAN to { setInverseAwareBackground(AnsiColor.CYAN.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.WHITE to { setInverseAwareBackground(AnsiColor.WHITE.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.BLACK_BRIGHT to { setInverseAwareBackground(AnsiColor.BRIGHT_BLACK.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.RED_BRIGHT to { setInverseAwareBackground(AnsiColor.BRIGHT_RED.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.GREEN_BRIGHT to { setInverseAwareBackground(AnsiColor.BRIGHT_GREEN.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.YELLOW_BRIGHT to { setInverseAwareBackground(AnsiColor.BRIGHT_YELLOW.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.BLUE_BRIGHT to { setInverseAwareBackground(AnsiColor.BRIGHT_BLUE.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.MAGENTA_BRIGHT to { setInverseAwareBackground(AnsiColor.BRIGHT_MAGENTA.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.CYAN_BRIGHT to { setInverseAwareBackground(AnsiColor.BRIGHT_CYAN.toSwingColor()) },
        Ansi.Csi.Codes.Sgr.Colors.Bg.WHITE_BRIGHT to { setInverseAwareBackground(AnsiColor.BRIGHT_WHITE.toSwingColor()) },

        Ansi.Csi.Codes.Sgr.Colors.INVERT to {
            if (getAttribute(Inverted) == null) {
                invertColors()
                addAttribute(Inverted, true)
            }
        },
        Ansi.Csi.Codes.Sgr.Colors.CLEAR_INVERT to {
            if (getAttribute(Inverted) != null) {
                removeAttribute(Inverted)
                invertColors()
            }
        },

        Ansi.Csi.Codes.Sgr.Decorations.BOLD to { StyleConstants.setBold(this, true) },
        Ansi.Csi.Codes.Sgr.Decorations.CLEAR_BOLD to { removeAttribute(StyleConstants.Bold) },
        Ansi.Csi.Codes.Sgr.Decorations.UNDERLINE to { StyleConstants.setUnderline(this, true) },
        Ansi.Csi.Codes.Sgr.Decorations.CLEAR_UNDERLINE to { removeAttribute(StyleConstants.Underline) },
        Ansi.Csi.Codes.Sgr.Decorations.STRIKETHROUGH to { StyleConstants.setStrikeThrough(this, true) },
        Ansi.Csi.Codes.Sgr.Decorations.CLEAR_STRIKETHROUGH to { removeAttribute(StyleConstants.StrikeThrough) },
    )

    operator fun get(sgrCode: Ansi.Csi.Code) = SGR_CODE_TO_ATTR_MODIFIER[sgrCode]

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