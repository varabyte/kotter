package com.varabyte.konsole.text

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.ansi.AnsiKonsoleCommand
import com.varabyte.konsole.core.KonsoleBlock
import com.varabyte.konsole.core.KonsoleBlockState
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.scopedState

enum class ColorLayer {
    FG,
    BG
}

private class ColorKonsoleCommand(ansiCommand: String, private val layer: ColorLayer)
    : AnsiKonsoleCommand(ansiCommand) {
    override fun updateState(state: KonsoleBlockState) {
        when(layer) {
            ColorLayer.FG -> state.fgColor = this
            ColorLayer.BG -> state.bgColor = this
        }
    }
}

private val FG_BLACK_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.BLACK, ColorLayer.FG)
private val FG_RED_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.RED, ColorLayer.FG)
private val FG_GREEN_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.GREEN, ColorLayer.FG)
private val FG_YELLOW_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.YELLOW, ColorLayer.FG)
private val FG_BLUE_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.BLUE, ColorLayer.FG)
private val FG_MAGENTA_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.MAGENTA, ColorLayer.FG)
private val FG_CYAN_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.CYAN, ColorLayer.FG)
private val FG_WHITE_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.WHITE, ColorLayer.FG)

private val FG_BLACK_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.BLACK_BRIGHT, ColorLayer.FG)
private val FG_RED_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.RED_BRIGHT, ColorLayer.FG)
private val FG_GREEN_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.GREEN_BRIGHT, ColorLayer.FG)
private val FG_YELLOW_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.YELLOW_BRIGHT, ColorLayer.FG)
private val FG_BLUE_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.BLUE_BRIGHT, ColorLayer.FG)
private val FG_MAGENTA_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.MAGENTA_BRIGHT, ColorLayer.FG)
private val FG_CYAN_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.CYAN_BRIGHT, ColorLayer.FG)
private val FG_WHITE_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Fg.WHITE_BRIGHT, ColorLayer.FG)

private val BG_BLACK_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.BLACK, ColorLayer.BG)
private val BG_RED_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.RED, ColorLayer.BG)
private val BG_GREEN_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.GREEN, ColorLayer.BG)
private val BG_YELLOW_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.YELLOW, ColorLayer.BG)
private val BG_BLUE_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.BLUE, ColorLayer.BG)
private val BG_MAGENTA_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.MAGENTA, ColorLayer.BG)
private val BG_CYAN_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.CYAN, ColorLayer.BG)
private val BG_WHITE_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.WHITE, ColorLayer.BG)

private val BG_BLACK_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.BLACK_BRIGHT, ColorLayer.BG)
private val BG_RED_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.RED_BRIGHT, ColorLayer.BG)
private val BG_GREEN_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.GREEN_BRIGHT, ColorLayer.BG)
private val BG_YELLOW_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.YELLOW_BRIGHT, ColorLayer.BG)
private val BG_BLUE_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.BLUE_BRIGHT, ColorLayer.BG)
private val BG_MAGENTA_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.MAGENTA_BRIGHT, ColorLayer.BG)
private val BG_CYAN_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.CYAN_BRIGHT, ColorLayer.BG)
private val BG_WHITE_BRIGHT_COMMAND = ColorKonsoleCommand(AnsiCodes.Colors.Bg.WHITE_BRIGHT, ColorLayer.BG)

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

fun KonsoleScope.clearColor(colorLayer: ColorLayer = ColorLayer.FG) {
    when (colorLayer) {
        ColorLayer.FG -> state.fgColor = null
        ColorLayer.BG -> state.bgColor = null
    }
    state.applyTo(block)
}

fun KonsoleScope.clearColors() {
    state.fgColor = null
    state.bgColor = null
    state.applyTo(block)
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

