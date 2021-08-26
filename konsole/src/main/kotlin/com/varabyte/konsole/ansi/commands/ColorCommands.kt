package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.ansi.Ansi.Csi
import com.varabyte.konsole.ansi.Ansi.Csi.Codes.Sgr.Colors
import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.KonsoleState

enum class ColorLayer {
    FG,
    BG
}

private class ColorCommand(csiCode: Csi.Code, private val layer: ColorLayer)
    : AnsiCsiCommand(csiCode) {
    override fun updateState(state: KonsoleState) {
        when(layer) {
            ColorLayer.FG -> state.fgColor = this
            ColorLayer.BG -> state.bgColor = this
        }
    }
}

private val FG_BLACK_COMMAND = ColorCommand(Colors.Fg.BLACK, ColorLayer.FG)
private val FG_RED_COMMAND = ColorCommand(Colors.Fg.RED, ColorLayer.FG)
private val FG_GREEN_COMMAND = ColorCommand(Colors.Fg.GREEN, ColorLayer.FG)
private val FG_YELLOW_COMMAND = ColorCommand(Colors.Fg.YELLOW, ColorLayer.FG)
private val FG_BLUE_COMMAND = ColorCommand(Colors.Fg.BLUE, ColorLayer.FG)
private val FG_MAGENTA_COMMAND = ColorCommand(Colors.Fg.MAGENTA, ColorLayer.FG)
private val FG_CYAN_COMMAND = ColorCommand(Colors.Fg.CYAN, ColorLayer.FG)
private val FG_WHITE_COMMAND = ColorCommand(Colors.Fg.WHITE, ColorLayer.FG)

private val FG_BLACK_BRIGHT_COMMAND = ColorCommand(Colors.Fg.BLACK_BRIGHT, ColorLayer.FG)
private val FG_RED_BRIGHT_COMMAND = ColorCommand(Colors.Fg.RED_BRIGHT, ColorLayer.FG)
private val FG_GREEN_BRIGHT_COMMAND = ColorCommand(Colors.Fg.GREEN_BRIGHT, ColorLayer.FG)
private val FG_YELLOW_BRIGHT_COMMAND = ColorCommand(Colors.Fg.YELLOW_BRIGHT, ColorLayer.FG)
private val FG_BLUE_BRIGHT_COMMAND = ColorCommand(Colors.Fg.BLUE_BRIGHT, ColorLayer.FG)
private val FG_MAGENTA_BRIGHT_COMMAND = ColorCommand(Colors.Fg.MAGENTA_BRIGHT, ColorLayer.FG)
private val FG_CYAN_BRIGHT_COMMAND = ColorCommand(Colors.Fg.CYAN_BRIGHT, ColorLayer.FG)
private val FG_WHITE_BRIGHT_COMMAND = ColorCommand(Colors.Fg.WHITE_BRIGHT, ColorLayer.FG)

private val BG_BLACK_COMMAND = ColorCommand(Colors.Bg.BLACK, ColorLayer.BG)
private val BG_RED_COMMAND = ColorCommand(Colors.Bg.RED, ColorLayer.BG)
private val BG_GREEN_COMMAND = ColorCommand(Colors.Bg.GREEN, ColorLayer.BG)
private val BG_YELLOW_COMMAND = ColorCommand(Colors.Bg.YELLOW, ColorLayer.BG)
private val BG_BLUE_COMMAND = ColorCommand(Colors.Bg.BLUE, ColorLayer.BG)
private val BG_MAGENTA_COMMAND = ColorCommand(Colors.Bg.MAGENTA, ColorLayer.BG)
private val BG_CYAN_COMMAND = ColorCommand(Colors.Bg.CYAN, ColorLayer.BG)
private val BG_WHITE_COMMAND = ColorCommand(Colors.Bg.WHITE, ColorLayer.BG)

private val BG_BLACK_BRIGHT_COMMAND = ColorCommand(Colors.Bg.BLACK_BRIGHT, ColorLayer.BG)
private val BG_RED_BRIGHT_COMMAND = ColorCommand(Colors.Bg.RED_BRIGHT, ColorLayer.BG)
private val BG_GREEN_BRIGHT_COMMAND = ColorCommand(Colors.Bg.GREEN_BRIGHT, ColorLayer.BG)
private val BG_YELLOW_BRIGHT_COMMAND = ColorCommand(Colors.Bg.YELLOW_BRIGHT, ColorLayer.BG)
private val BG_BLUE_BRIGHT_COMMAND = ColorCommand(Colors.Bg.BLUE_BRIGHT, ColorLayer.BG)
private val BG_MAGENTA_BRIGHT_COMMAND = ColorCommand(Colors.Bg.MAGENTA_BRIGHT, ColorLayer.BG)
private val BG_CYAN_BRIGHT_COMMAND = ColorCommand(Colors.Bg.CYAN_BRIGHT, ColorLayer.BG)
private val BG_WHITE_BRIGHT_COMMAND = ColorCommand(Colors.Bg.WHITE_BRIGHT, ColorLayer.BG)

private val INVERT_COMMAND = object : AnsiCsiCommand(Colors.INVERT) {
    override fun updateState(state: KonsoleState) {
        state.inverted = this
    }
}

private fun toBlackCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_BLACK_BRIGHT_COMMAND else FG_BLACK_COMMAND
    ColorLayer.BG -> if (isBright) BG_BLACK_BRIGHT_COMMAND else BG_BLACK_COMMAND
}

private fun toRedCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_RED_BRIGHT_COMMAND else FG_RED_COMMAND
    ColorLayer.BG -> if (isBright) BG_RED_BRIGHT_COMMAND else BG_RED_COMMAND
}

private fun toGreenCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_GREEN_BRIGHT_COMMAND else FG_GREEN_COMMAND
    ColorLayer.BG -> if (isBright) BG_GREEN_BRIGHT_COMMAND else BG_GREEN_COMMAND
}

private fun toYellowCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_YELLOW_BRIGHT_COMMAND else FG_YELLOW_COMMAND
    ColorLayer.BG -> if (isBright) BG_YELLOW_BRIGHT_COMMAND else BG_YELLOW_COMMAND
}

private fun toBlueCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_BLUE_BRIGHT_COMMAND else FG_BLUE_COMMAND
    ColorLayer.BG -> if (isBright) BG_BLUE_BRIGHT_COMMAND else BG_BLUE_COMMAND
}

private fun toMagentaCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_MAGENTA_BRIGHT_COMMAND else FG_MAGENTA_COMMAND
    ColorLayer.BG -> if (isBright) BG_MAGENTA_BRIGHT_COMMAND else BG_MAGENTA_COMMAND
}

private fun toCyanCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_CYAN_BRIGHT_COMMAND else FG_CYAN_COMMAND
    ColorLayer.BG -> if (isBright) BG_CYAN_BRIGHT_COMMAND else BG_CYAN_COMMAND
}

private fun toWhiteCommand(colorLayer: ColorLayer, isBright: Boolean) = when(colorLayer) {
    ColorLayer.FG -> if (isBright) FG_WHITE_BRIGHT_COMMAND else FG_WHITE_COMMAND
    ColorLayer.BG -> if (isBright) BG_WHITE_BRIGHT_COMMAND else BG_WHITE_COMMAND
}

fun KonsoleScope.black(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toBlackCommand(colorLayer, isBright))
}

fun KonsoleScope.grey(colorLayer: ColorLayer = ColorLayer.FG) {
    black(colorLayer, isBright = true)
}

fun KonsoleScope.red(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toRedCommand(colorLayer, isBright))
}

fun KonsoleScope.green(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toGreenCommand(colorLayer, isBright))
}

fun KonsoleScope.yellow(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toYellowCommand(colorLayer, isBright))
}

fun KonsoleScope.blue(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toBlueCommand(colorLayer, isBright))
}

fun KonsoleScope.magenta(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toMagentaCommand(colorLayer, isBright))
}

fun KonsoleScope.cyan(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toCyanCommand(colorLayer, isBright))
}

fun KonsoleScope.white(colorLayer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    applyCommand(toWhiteCommand(colorLayer, isBright))
}

fun KonsoleScope.invert() {
    applyCommand(INVERT_COMMAND)
}

fun KonsoleScope.black(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        black(colorLayer, isBright)
        scopedBlock()
    }
}

fun KonsoleScope.grey(
    colorLayer: ColorLayer = ColorLayer.FG,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    black(colorLayer, isBright = true, scopedBlock)
}

fun KonsoleScope.red(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        red(colorLayer, isBright)
        scopedBlock()
    }
}

fun KonsoleScope.green(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        green(colorLayer, isBright)
        scopedBlock()
    }
}

fun KonsoleScope.yellow(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        yellow(colorLayer, isBright)
        scopedBlock()
    }
}

fun KonsoleScope.blue(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        blue(colorLayer, isBright)
        scopedBlock()
    }
}

fun KonsoleScope.cyan(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        cyan(colorLayer, isBright)
        scopedBlock()
    }
}

fun KonsoleScope.magenta(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        magenta(colorLayer, isBright)
        scopedBlock()
    }
}

fun KonsoleScope.white(
    colorLayer: ColorLayer = ColorLayer.FG,
    isBright: Boolean = false,
    scopedBlock: KonsoleBlock.() -> Unit
) {
    scopedState {
        white(colorLayer, isBright)
        scopedBlock()
    }
}

fun KonsoleScope.invert(scopedBlock: KonsoleBlock.() -> Unit) {
    scopedState {
        invert()
        scopedBlock()
    }
}