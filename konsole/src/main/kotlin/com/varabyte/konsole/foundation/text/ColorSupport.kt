package com.varabyte.konsole.foundation.text

import com.varabyte.konsole.runtime.internal.ansi.commands.*
import com.varabyte.konsole.runtime.render.RenderScope

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