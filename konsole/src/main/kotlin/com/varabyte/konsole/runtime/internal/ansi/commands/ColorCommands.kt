package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.internal.ansi.Ansi.Csi
import com.varabyte.konsole.runtime.internal.ansi.Ansi.Csi.Codes.Sgr.Colors

internal class FgColorCommand(csiCode: Csi.Code) : AnsiCsiCommand(csiCode) {
    override fun updateState(state: KonsoleState) {
        state.fgColor = this
    }
}

internal class BgColorCommand(csiCode: Csi.Code) : AnsiCsiCommand(csiCode) {
    override fun updateState(state: KonsoleState) {
        state.bgColor = this
    }
}

internal val FG_BLACK_COMMAND = FgColorCommand(Colors.Fg.BLACK)
internal val FG_RED_COMMAND = FgColorCommand(Colors.Fg.RED)
internal val FG_GREEN_COMMAND = FgColorCommand(Colors.Fg.GREEN)
internal val FG_YELLOW_COMMAND = FgColorCommand(Colors.Fg.YELLOW)
internal val FG_BLUE_COMMAND = FgColorCommand(Colors.Fg.BLUE)
internal val FG_MAGENTA_COMMAND = FgColorCommand(Colors.Fg.MAGENTA)
internal val FG_CYAN_COMMAND = FgColorCommand(Colors.Fg.CYAN)
internal val FG_WHITE_COMMAND = FgColorCommand(Colors.Fg.WHITE)

internal val FG_BLACK_BRIGHT_COMMAND = FgColorCommand(Colors.Fg.BLACK_BRIGHT)
internal val FG_RED_BRIGHT_COMMAND = FgColorCommand(Colors.Fg.RED_BRIGHT)
internal val FG_GREEN_BRIGHT_COMMAND = FgColorCommand(Colors.Fg.GREEN_BRIGHT)
internal val FG_YELLOW_BRIGHT_COMMAND = FgColorCommand(Colors.Fg.YELLOW_BRIGHT)
internal val FG_BLUE_BRIGHT_COMMAND = FgColorCommand(Colors.Fg.BLUE_BRIGHT)
internal val FG_MAGENTA_BRIGHT_COMMAND = FgColorCommand(Colors.Fg.MAGENTA_BRIGHT)
internal val FG_CYAN_BRIGHT_COMMAND = FgColorCommand(Colors.Fg.CYAN_BRIGHT)
internal val FG_WHITE_BRIGHT_COMMAND = FgColorCommand(Colors.Fg.WHITE_BRIGHT)

internal object FG_CLEAR_COMMAND : AnsiCsiCommand(Colors.Fg.CLEAR) {
    override fun updateState(state: KonsoleState) {
        state.fgColor = null
    }
}

internal val BG_BLACK_COMMAND = BgColorCommand(Colors.Bg.BLACK)
internal val BG_RED_COMMAND = BgColorCommand(Colors.Bg.RED)
internal val BG_GREEN_COMMAND = BgColorCommand(Colors.Bg.GREEN)
internal val BG_YELLOW_COMMAND = BgColorCommand(Colors.Bg.YELLOW)
internal val BG_BLUE_COMMAND = BgColorCommand(Colors.Bg.BLUE)
internal val BG_MAGENTA_COMMAND = BgColorCommand(Colors.Bg.MAGENTA)
internal val BG_CYAN_COMMAND = BgColorCommand(Colors.Bg.CYAN)
internal val BG_WHITE_COMMAND = BgColorCommand(Colors.Bg.WHITE)

internal val BG_BLACK_BRIGHT_COMMAND = BgColorCommand(Colors.Bg.BLACK_BRIGHT)
internal val BG_RED_BRIGHT_COMMAND = BgColorCommand(Colors.Bg.RED_BRIGHT)
internal val BG_GREEN_BRIGHT_COMMAND = BgColorCommand(Colors.Bg.GREEN_BRIGHT)
internal val BG_YELLOW_BRIGHT_COMMAND = BgColorCommand(Colors.Bg.YELLOW_BRIGHT)
internal val BG_BLUE_BRIGHT_COMMAND = BgColorCommand(Colors.Bg.BLUE_BRIGHT)
internal val BG_MAGENTA_BRIGHT_COMMAND = BgColorCommand(Colors.Bg.MAGENTA_BRIGHT)
internal val BG_CYAN_BRIGHT_COMMAND = BgColorCommand(Colors.Bg.CYAN_BRIGHT)
internal val BG_WHITE_BRIGHT_COMMAND = BgColorCommand(Colors.Bg.WHITE_BRIGHT)

internal object BG_CLEAR_COMMAND : AnsiCsiCommand(Colors.Bg.CLEAR) {
    override fun updateState(state: KonsoleState) {
        state.bgColor = null
    }
}

internal val INVERT_COMMAND = object : AnsiCsiCommand(Colors.INVERT) {
    override fun updateState(state: KonsoleState) {
        state.inverted = this
    }
}