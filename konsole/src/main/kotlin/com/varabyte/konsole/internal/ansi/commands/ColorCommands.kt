package com.varabyte.konsole.internal.ansi.commands

import com.varabyte.konsole.core.KonsoleState
import com.varabyte.konsole.core.text.ColorLayer
import com.varabyte.konsole.internal.ansi.Ansi.Csi
import com.varabyte.konsole.internal.ansi.Ansi.Csi.Codes.Sgr.Colors

internal class ColorCommand(csiCode: Csi.Code, private val layer: ColorLayer)
    : AnsiCsiCommand(csiCode) {
    override fun updateState(state: KonsoleState) {
        when(layer) {
            ColorLayer.FG -> state.fgColor = this
            ColorLayer.BG -> state.bgColor = this
        }
    }
}

internal val FG_BLACK_COMMAND = ColorCommand(Colors.Fg.BLACK, ColorLayer.FG)
internal val FG_RED_COMMAND = ColorCommand(Colors.Fg.RED, ColorLayer.FG)
internal val FG_GREEN_COMMAND = ColorCommand(Colors.Fg.GREEN, ColorLayer.FG)
internal val FG_YELLOW_COMMAND = ColorCommand(Colors.Fg.YELLOW, ColorLayer.FG)
internal val FG_BLUE_COMMAND = ColorCommand(Colors.Fg.BLUE, ColorLayer.FG)
internal val FG_MAGENTA_COMMAND = ColorCommand(Colors.Fg.MAGENTA, ColorLayer.FG)
internal val FG_CYAN_COMMAND = ColorCommand(Colors.Fg.CYAN, ColorLayer.FG)
internal val FG_WHITE_COMMAND = ColorCommand(Colors.Fg.WHITE, ColorLayer.FG)

internal val FG_BLACK_BRIGHT_COMMAND = ColorCommand(Colors.Fg.BLACK_BRIGHT, ColorLayer.FG)
internal val FG_RED_BRIGHT_COMMAND = ColorCommand(Colors.Fg.RED_BRIGHT, ColorLayer.FG)
internal val FG_GREEN_BRIGHT_COMMAND = ColorCommand(Colors.Fg.GREEN_BRIGHT, ColorLayer.FG)
internal val FG_YELLOW_BRIGHT_COMMAND = ColorCommand(Colors.Fg.YELLOW_BRIGHT, ColorLayer.FG)
internal val FG_BLUE_BRIGHT_COMMAND = ColorCommand(Colors.Fg.BLUE_BRIGHT, ColorLayer.FG)
internal val FG_MAGENTA_BRIGHT_COMMAND = ColorCommand(Colors.Fg.MAGENTA_BRIGHT, ColorLayer.FG)
internal val FG_CYAN_BRIGHT_COMMAND = ColorCommand(Colors.Fg.CYAN_BRIGHT, ColorLayer.FG)
internal val FG_WHITE_BRIGHT_COMMAND = ColorCommand(Colors.Fg.WHITE_BRIGHT, ColorLayer.FG)

internal val BG_BLACK_COMMAND = ColorCommand(Colors.Bg.BLACK, ColorLayer.BG)
internal val BG_RED_COMMAND = ColorCommand(Colors.Bg.RED, ColorLayer.BG)
internal val BG_GREEN_COMMAND = ColorCommand(Colors.Bg.GREEN, ColorLayer.BG)
internal val BG_YELLOW_COMMAND = ColorCommand(Colors.Bg.YELLOW, ColorLayer.BG)
internal val BG_BLUE_COMMAND = ColorCommand(Colors.Bg.BLUE, ColorLayer.BG)
internal val BG_MAGENTA_COMMAND = ColorCommand(Colors.Bg.MAGENTA, ColorLayer.BG)
internal val BG_CYAN_COMMAND = ColorCommand(Colors.Bg.CYAN, ColorLayer.BG)
internal val BG_WHITE_COMMAND = ColorCommand(Colors.Bg.WHITE, ColorLayer.BG)

internal val BG_BLACK_BRIGHT_COMMAND = ColorCommand(Colors.Bg.BLACK_BRIGHT, ColorLayer.BG)
internal val BG_RED_BRIGHT_COMMAND = ColorCommand(Colors.Bg.RED_BRIGHT, ColorLayer.BG)
internal val BG_GREEN_BRIGHT_COMMAND = ColorCommand(Colors.Bg.GREEN_BRIGHT, ColorLayer.BG)
internal val BG_YELLOW_BRIGHT_COMMAND = ColorCommand(Colors.Bg.YELLOW_BRIGHT, ColorLayer.BG)
internal val BG_BLUE_BRIGHT_COMMAND = ColorCommand(Colors.Bg.BLUE_BRIGHT, ColorLayer.BG)
internal val BG_MAGENTA_BRIGHT_COMMAND = ColorCommand(Colors.Bg.MAGENTA_BRIGHT, ColorLayer.BG)
internal val BG_CYAN_BRIGHT_COMMAND = ColorCommand(Colors.Bg.CYAN_BRIGHT, ColorLayer.BG)
internal val BG_WHITE_BRIGHT_COMMAND = ColorCommand(Colors.Bg.WHITE_BRIGHT, ColorLayer.BG)

internal val INVERT_COMMAND = object : AnsiCsiCommand(Colors.INVERT) {
    override fun updateState(state: KonsoleState) {
        state.inverted = this
    }
}