package com.varabyte.konsole.foundation.text

import com.varabyte.konsole.runtime.internal.ansi.commands.*
import com.varabyte.konsole.runtime.render.RenderScope
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ColorLayer {
    FG,
    BG
}

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

data class RGB(val r: Int, val g: Int, val b: Int) {
    constructor(r: Float, g: Float, b: Float)
            : this((r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())

    init {
        require(r in (0..255) && g in (0 .. 255) && b in (0 .. 255)) { "Color values must be between 0 and 255 (inclusive). Got: ($r, $g, $b)"}
    }

    companion object {
        fun from(value: Int) = RGB(
            value.and(0xFF0000).shr(16),
            value.and(0x00FF00).shr(8),
            value.and(0x0000FF),
        )
    }
}

data class HSV(val h: Int, val s: Float, val v: Float) {
    init {
        require(h in (0 .. 360) && s in (0.0 .. 1.0) && v in (0.0 .. 1.0)) {
            "Hue must be between 0 and 360 (inclusive). Saturation and value must be between 0.0 and 1.0 (inclusive) Got: ($h, $s, $v)"
        }
    }

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

private fun toBlackCommand(layer: ColorLayer, isBright: Boolean) = when(layer) {
    ColorLayer.FG -> if (isBright) FG_BLACK_BRIGHT_COMMAND else FG_BLACK_COMMAND
    ColorLayer.BG -> if (isBright) BG_BLACK_BRIGHT_COMMAND else BG_BLACK_COMMAND
}

private fun toRedCommand(layer: ColorLayer, isBright: Boolean) = when(layer) {
    ColorLayer.FG -> if (isBright) FG_RED_BRIGHT_COMMAND else FG_RED_COMMAND
    ColorLayer.BG -> if (isBright) BG_RED_BRIGHT_COMMAND else BG_RED_COMMAND
}

private fun toGreenCommand(layer: ColorLayer, isBright: Boolean) = when(layer) {
    ColorLayer.FG -> if (isBright) FG_GREEN_BRIGHT_COMMAND else FG_GREEN_COMMAND
    ColorLayer.BG -> if (isBright) BG_GREEN_BRIGHT_COMMAND else BG_GREEN_COMMAND
}

private fun toYellowCommand(layer: ColorLayer, isBright: Boolean) = when(layer) {
    ColorLayer.FG -> if (isBright) FG_YELLOW_BRIGHT_COMMAND else FG_YELLOW_COMMAND
    ColorLayer.BG -> if (isBright) BG_YELLOW_BRIGHT_COMMAND else BG_YELLOW_COMMAND
}

private fun toBlueCommand(layer: ColorLayer, isBright: Boolean) = when(layer) {
    ColorLayer.FG -> if (isBright) FG_BLUE_BRIGHT_COMMAND else FG_BLUE_COMMAND
    ColorLayer.BG -> if (isBright) BG_BLUE_BRIGHT_COMMAND else BG_BLUE_COMMAND
}

private fun toMagentaCommand(layer: ColorLayer, isBright: Boolean) = when(layer) {
    ColorLayer.FG -> if (isBright) FG_MAGENTA_BRIGHT_COMMAND else FG_MAGENTA_COMMAND
    ColorLayer.BG -> if (isBright) BG_MAGENTA_BRIGHT_COMMAND else BG_MAGENTA_COMMAND
}

private fun toCyanCommand(layer: ColorLayer, isBright: Boolean) = when(layer) {
    ColorLayer.FG -> if (isBright) FG_CYAN_BRIGHT_COMMAND else FG_CYAN_COMMAND
    ColorLayer.BG -> if (isBright) BG_CYAN_BRIGHT_COMMAND else BG_CYAN_COMMAND
}

private fun toWhiteCommand(layer: ColorLayer, isBright: Boolean) = when(layer) {
    ColorLayer.FG -> if (isBright) FG_WHITE_BRIGHT_COMMAND else FG_WHITE_COMMAND
    ColorLayer.BG -> if (isBright) BG_WHITE_BRIGHT_COMMAND else BG_WHITE_COMMAND
}

private fun toLookupCommand(layer: ColorLayer, index: Int) = when(layer) {
    ColorLayer.FG -> fgLookupCommand(index)
    ColorLayer.BG -> bgLookupCommand(index)
}

private fun toTruecolorCommand(layer: ColorLayer, r: Int, g: Int, b: Int) = when(layer) {
    ColorLayer.FG -> fgTruecolorCommand(r, g, b)
    ColorLayer.BG -> bgTruecolorCommand(r, g, b)
}

fun RenderScope.black(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toBlackCommand(layer, isBright))
}

fun RenderScope.red(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toRedCommand(layer, isBright))
}

fun RenderScope.green(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toGreenCommand(layer, isBright))
}

fun RenderScope.yellow(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toYellowCommand(layer, isBright))
}

fun RenderScope.blue(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toBlueCommand(layer, isBright))
}

fun RenderScope.magenta(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toMagentaCommand(layer, isBright))
}

fun RenderScope.cyan(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toCyanCommand(layer, isBright))
}

fun RenderScope.white(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toWhiteCommand(layer, isBright))
}

fun RenderScope.color(color: Color, layer: ColorLayer = ColorLayer.FG) {
    applyCommand(when (color) {
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
    })
}

fun RenderScope.color(index: Int, layer: ColorLayer = ColorLayer.FG) {
    applyCommand(toLookupCommand(layer, index))
}

fun RenderScope.rgb(r: Int, g: Int, b: Int, layer: ColorLayer = ColorLayer.FG) {
    applyCommand(toTruecolorCommand(layer, r, g, b))
}

fun RenderScope.hsv(h: Int, s: Float, v: Float, layer: ColorLayer = ColorLayer.FG) {
    val rgb = HSV(h, s, v).toRgb()
    applyCommand(toTruecolorCommand(layer, rgb.r, rgb.g, rgb.b))
}

fun RenderScope.rgb(value: Int, layer: ColorLayer = ColorLayer.FG) {
    val (r, g, b) = RGB.from(value)
    applyCommand(toTruecolorCommand(layer, r, g, b))
}

fun RenderScope.clearColor(layer: ColorLayer = ColorLayer.FG) {
    applyCommand(when(layer) {
        ColorLayer.FG -> FG_CLEAR_COMMAND
        ColorLayer.BG -> BG_CLEAR_COMMAND
    })
}

fun RenderScope.clearColors() {
    applyCommand(FG_CLEAR_COMMAND)
    applyCommand(BG_CLEAR_COMMAND)
}

fun RenderScope.invert() {
    applyCommand(INVERT_COMMAND)
}

fun RenderScope.clearInvert() {
    applyCommand(CLEAR_INVERT_COMMAND)
}

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
 * Use an index to lookup an 8-bit color.
 *
 * See also: https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
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

fun RenderScope.rgb(
    value: Int,
    layer: ColorLayer = ColorLayer.FG,
    scopedBlock: RenderScope.() -> Unit
) {
    val (r, g, b) = RGB.from(value)
    rgb(r, g, b, layer, scopedBlock)
}

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

fun RenderScope.clearColor(layer: ColorLayer = ColorLayer.FG, scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        clearColor(layer)
        scopedBlock()
    }
}

fun RenderScope.clearColors(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        clearColors()
        scopedBlock()
    }
}

fun RenderScope.invert(scopedBlock: RenderScope.() -> Unit) {
    scopedState {
        invert()
        scopedBlock()
    }
}