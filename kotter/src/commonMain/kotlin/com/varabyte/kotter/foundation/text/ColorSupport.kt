package com.varabyte.kotter.foundation.text

import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.render.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Terminal text layers whose colors can be independently modified.
 */
enum class ColorLayer {
    FG,
    BG
}

/**
 * A list of standard ANSI colors.
 *
 * See also: https://en.wikipedia.org/wiki/ANSI_escape_code#3-bit_and_4-bit
 */
enum class Color {
    BLACK,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE,
    BRIGHT_BLACK,
    BRIGHT_RED,
    BRIGHT_GREEN,
    BRIGHT_YELLOW,
    BRIGHT_BLUE,
    BRIGHT_MAGENTA,
    BRIGHT_CYAN,
    BRIGHT_WHITE;
}

/** Simple data class representing RGB values for some 24-bit color */
data class RGB(val r: Int, val g: Int, val b: Int) {
    constructor(r: Float, g: Float, b: Float)
            : this((r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())

    init {
        require(r in (0..255) && g in (0..255) && b in (0..255)) { "Color values must be between 0 and 255 (inclusive). Got: ($r, $g, $b)" }
    }

    companion object {
        /**
         * Convert a 24-bit hex value into an [RGB] instance.
         *
         * For example, `from(0xFF00FF)` will create `RGB(255, 0, 255)`.
         */
        fun from(value: Int) = RGB(
            value.and(0xFF0000).shr(16),
            value.and(0x00FF00).shr(8),
            value.and(0x0000FF),
        )
    }
}

/** Simple data class representing HSV values. */
data class HSV(val h: Int, val s: Float, val v: Float) {
    init {
        require(h in (0..360) && s in (0.0..1.0) && v in (0.0..1.0)) {
            "Hue must be between 0 and 360 (inclusive). Saturation and value must be between 0.0 and 1.0 (inclusive) Got: ($h, $s, $v)"
        }
    }

    /**
     * Convert this HSV value to RGB.
     */
    // See also: https://en.wikipedia.org/wiki/HSL_and_HSV#HSV_to_RGB
    fun toRgb(): RGB {
        if (s == 0.0f) {
            val vInt = (v * 255).roundToInt()
            return RGB(vInt, vInt, vInt)
        }

        val region = (h % 360) / 60
        val chroma = v * s
        val remainder = (h / 60f) % 2
        val secondary = chroma * (1 - abs(remainder - 1f))
        val (r1, g1, b1) = when (region) {
            0 -> arrayOf(chroma, secondary, 0f)
            1 -> arrayOf(secondary, chroma, 0f)
            2 -> arrayOf(0f, chroma, secondary)
            3 -> arrayOf(0f, secondary, chroma)
            4 -> arrayOf(secondary, 0f, chroma)
            else -> arrayOf(chroma, 0f, secondary)
        }

        val delta = v - chroma
        return RGB(r1 + delta, g1 + delta, b1 + delta)
    }
}

private fun toBlackCommand(layer: ColorLayer, isBright: Boolean) = when (layer) {
    ColorLayer.FG -> if (isBright) ColorCommands.Fg.BrightBlack else ColorCommands.Fg.Black
    ColorLayer.BG -> if (isBright) ColorCommands.Bg.BrightBlack else ColorCommands.Bg.Black
}

private fun toRedCommand(layer: ColorLayer, isBright: Boolean) = when (layer) {
    ColorLayer.FG -> if (isBright) ColorCommands.Fg.BrightRed else ColorCommands.Fg.Red
    ColorLayer.BG -> if (isBright) ColorCommands.Bg.BrightRed else ColorCommands.Bg.Red
}

private fun toGreenCommand(layer: ColorLayer, isBright: Boolean) = when (layer) {
    ColorLayer.FG -> if (isBright) ColorCommands.Fg.BrightGreen else ColorCommands.Fg.Green
    ColorLayer.BG -> if (isBright) ColorCommands.Bg.BrightGreen else ColorCommands.Bg.Green
}

private fun toYellowCommand(layer: ColorLayer, isBright: Boolean) = when (layer) {
    ColorLayer.FG -> if (isBright) ColorCommands.Fg.BrightYellow else ColorCommands.Fg.Yellow
    ColorLayer.BG -> if (isBright) ColorCommands.Bg.BrightYellow else ColorCommands.Bg.Yellow
}

private fun toBlueCommand(layer: ColorLayer, isBright: Boolean) = when (layer) {
    ColorLayer.FG -> if (isBright) ColorCommands.Fg.BrightBlue else ColorCommands.Fg.Blue
    ColorLayer.BG -> if (isBright) ColorCommands.Bg.BrightBlue else ColorCommands.Bg.Blue
}

private fun toMagentaCommand(layer: ColorLayer, isBright: Boolean) = when (layer) {
    ColorLayer.FG -> if (isBright) ColorCommands.Fg.BrightMagenta else ColorCommands.Fg.Magenta
    ColorLayer.BG -> if (isBright) ColorCommands.Bg.BrightMagenta else ColorCommands.Bg.Magenta
}

private fun toCyanCommand(layer: ColorLayer, isBright: Boolean) = when (layer) {
    ColorLayer.FG -> if (isBright) ColorCommands.Fg.BrightCyan else ColorCommands.Fg.Cyan
    ColorLayer.BG -> if (isBright) ColorCommands.Bg.BrightCyan else ColorCommands.Bg.Cyan
}

private fun toWhiteCommand(layer: ColorLayer, isBright: Boolean) = when (layer) {
    ColorLayer.FG -> if (isBright) ColorCommands.Fg.BrightWhite else ColorCommands.Fg.White
    ColorLayer.BG -> if (isBright) ColorCommands.Bg.BrightWhite else ColorCommands.Bg.White
}

private fun toLookupCommand(layer: ColorLayer, index: Int) = when (layer) {
    ColorLayer.FG -> ColorCommands.Fg.Color.lookup(index)
    ColorLayer.BG -> ColorCommands.Bg.Color.lookup(index)
}

private fun toTruecolorCommand(layer: ColorLayer, r: Int, g: Int, b: Int) = when (layer) {
    ColorLayer.FG -> ColorCommands.Fg.Color.truecolor(r, g, b)
    ColorLayer.BG -> ColorCommands.Bg.Color.truecolor(r, g, b)
}

/**
 * Marks the current scope so that any text after this point will be colored black.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.black(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toBlackCommand(layer, isBright))
}

/**
 * Marks the current scope so that any text after this point will be colored red.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.red(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toRedCommand(layer, isBright))
}

/**
 * Marks the current scope so that any text after this point will be colored green.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.green(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toGreenCommand(layer, isBright))
}

/**
 * Marks the current scope so that any text after this point will be colored yellow.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.yellow(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toYellowCommand(layer, isBright))
}

/**
 * Marks the current scope so that any text after this point will be colored blue.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.blue(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toBlueCommand(layer, isBright))
}

/**
 * Marks the current scope so that any text after this point will be colored magenta.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.magenta(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toMagentaCommand(layer, isBright))
}

/**
 * Marks the current scope so that any text after this point will be colored cyan.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.cyan(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toCyanCommand(layer, isBright))
}

/**
 * Marks the current scope so that any text after this point will be colored white.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.white(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toWhiteCommand(layer, isBright))
}

/**
 * Marks the current scope so that any text after this point will be colored with the specified [Color] value.
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.color(color: Color, layer: ColorLayer = ColorLayer.FG) {
    applyCommand(
        when (color) {
            Color.BLACK -> toBlackCommand(layer, false)
            Color.RED -> toRedCommand(layer, false)
            Color.GREEN -> toGreenCommand(layer, false)
            Color.YELLOW -> toYellowCommand(layer, false)
            Color.BLUE -> toBlueCommand(layer, false)
            Color.MAGENTA -> toMagentaCommand(layer, false)
            Color.CYAN -> toCyanCommand(layer, false)
            Color.WHITE -> toWhiteCommand(layer, false)
            Color.BRIGHT_BLACK -> toBlackCommand(layer, true)
            Color.BRIGHT_RED -> toRedCommand(layer, true)
            Color.BRIGHT_GREEN -> toGreenCommand(layer, true)
            Color.BRIGHT_YELLOW -> toYellowCommand(layer, true)
            Color.BRIGHT_BLUE -> toBlueCommand(layer, true)
            Color.BRIGHT_MAGENTA -> toMagentaCommand(layer, true)
            Color.BRIGHT_CYAN -> toCyanCommand(layer, true)
            Color.BRIGHT_WHITE -> toWhiteCommand(layer, true)
        }
    )
}

/**
 * Marks the current scope so that any text after this point will be colored by the color associated with the ANSI
 * [color index][index].
 *
 * See also: https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.color(index: Int, layer: ColorLayer = ColorLayer.FG) {
    applyCommand(toLookupCommand(layer, index))
}

/**
 * Marks the current scope so that any text after this point will be colored by the input RGB value.
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.rgb(r: Int, g: Int, b: Int, layer: ColorLayer = ColorLayer.FG) {
    applyCommand(toTruecolorCommand(layer, r, g, b))
}

/**
 * Marks the current scope so that any text after this point will be colored by the input HSV value.
 */
fun RenderScope.hsv(h: Int, s: Float, v: Float, layer: ColorLayer = ColorLayer.FG) {
    val rgb = HSV(h, s, v).toRgb()
    applyCommand(toTruecolorCommand(layer, rgb.r, rgb.g, rgb.b))
}

/**
 * Marks the current scope so that any text after this point will be colored by the RGB hex value.
 *
 * For example, `rgb(0xFF00FF)` will be set the color to magenta.
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.rgb(value: Int, layer: ColorLayer = ColorLayer.FG) {
    val (r, g, b) = RGB.from(value)
    applyCommand(toTruecolorCommand(layer, r, g, b))
}

fun RenderScope.rgb(color: RGB, layer: ColorLayer = ColorLayer.FG) {
    rgb(color.r, color.g, color.b, layer)
}

fun RenderScope.hsv(color: HSV, layer: ColorLayer = ColorLayer.FG) {
    hsv(color.h, color.s, color.v, layer)
}

/**
 * Clears any color set earlier in this scope on a particular [ColorLayer].
 */
fun RenderScope.clearColor(layer: ColorLayer = ColorLayer.FG) {
    applyCommand(
        when (layer) {
            ColorLayer.FG -> ColorCommands.Fg.Clear
            ColorLayer.BG -> ColorCommands.Bg.Clear
        }
    )
}

/**
 * Clears any colors set earlier in this scope.
 */
fun RenderScope.clearColors() {
    applyCommand(ColorCommands.Fg.Clear)
    applyCommand(ColorCommands.Bg.Clear)
}

/**
 * Marks the current scope as inverted, so that any text after this point will be rendered with its background and
 * foreground color layers switched.
 *
 * Note that calling invert twice in a row does *not* undo the original invert. Instead, call [clearInvert].
 */
fun RenderScope.invert() {
    applyCommand(ColorCommands.Invert)
}

/**
 * Clears a previous call to [invert].
 */
fun RenderScope.clearInvert() {
    applyCommand(ColorCommands.ClearInvert)
}

/**
 * Create a new scope within which any text will be colored black.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.black(
    layer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        black(layer, isBright)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored red.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.red(
    layer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        red(layer, isBright)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored green.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.green(
    layer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        green(layer, isBright)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored yellow.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.yellow(
    layer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        yellow(layer, isBright)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored blue.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.blue(
    layer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        blue(layer, isBright)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored cyan.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.cyan(
    layer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        cyan(layer, isBright)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored magenta.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.magenta(
    layer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        magenta(layer, isBright)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored white.
 *
 * @param layer A color can be applied either to the text itself or its background.
 * @param isBright If true, use a brighter variation of this color.
 */
fun RenderScope.white(
    layer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        white(layer, isBright)
        scopedBlock()
    }
}


/**
 * Create a new scope within which any text will be colored using a color associated with the ANSI [color index][index].
 *
 * See also: https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.color(
    index: Int,
    layer: ColorLayer = ColorLayer.FG,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        color(index, layer)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored with the specified [Color] value.
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.color(
    color: Color,
    layer: ColorLayer = ColorLayer.FG,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        color(color, layer)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored by the input RGB value.
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.rgb(
    r: Int,
    g: Int,
    b: Int,
    layer: ColorLayer = ColorLayer.FG,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        rgb(r, g, b, layer)
        scopedBlock()
    }
}

/**
 * Marks the current scope so that any text after this point will be colored by the input hex value.
 *
 * For example, `rgb(0xFF00FF)` will be set the color to magenta.
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.rgb(
    value: Int,
    layer: ColorLayer = ColorLayer.FG,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        rgb(value, layer)
        scopedBlock()
    }
}

fun RenderScope.rgb(
    rgb: RGB,
    layer: ColorLayer = ColorLayer.FG,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        rgb(rgb, layer)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be colored by the input HSV value.
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.hsv(
    h: Int,
    s: Float,
    v: Float,
    layer: ColorLayer = ColorLayer.FG,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        hsv(h, s, v, layer)
        scopedBlock()
    }
}

fun RenderScope.hsv(
    hsv: HSV,
    layer: ColorLayer = ColorLayer.FG,
    scopedBlock: RenderScope.() -> Unit
) {
    scopedState {
        hsv(hsv, layer)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any color set earlier on a particular [ColorLayer] will be cleared.
 *
 * The color will be restored after the current scope exits.
 *
 * @param layer A color can be applied either to the text itself or its background.
 */
fun RenderScope.clearColor(layer: ColorLayer = ColorLayer.FG, scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        clearColor(layer)
        scopedBlock()
    }
}

/**
 * Create a new scope within which any colors set earlier will be cleared.
 *
 * Those colors will be restored after the current scope exits.
 */
fun RenderScope.clearColors(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        clearColors()
        scopedBlock()
    }
}

/**
 * Create a new scope within which any text will be rendered with its background and foreground color layers switched.
 */
fun RenderScope.invert(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        invert()
        scopedBlock()
    }
}
