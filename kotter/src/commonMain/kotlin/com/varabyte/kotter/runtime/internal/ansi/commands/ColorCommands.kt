package com.varabyte.kotter.runtime.internal.ansi.commands

import com.varabyte.kotter.runtime.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes.Sgr.Colors
import com.varabyte.kotter.runtime.render.*


internal object ColorCommands {
    object Fg {
        internal class Color(csiCode: Csi.Code) : AnsiCsiCommand(csiCode) {
            companion object {
                fun lookup(index: Int) = Color(Colors.Fg.lookup(index))
                fun truecolor(r: Int, g: Int, b: Int) = Color(Colors.Fg.truecolor(r, g, b))
            }
            override fun applyTo(state: SectionState, renderer: Renderer<*>) {
                state.deferred.fgColor = this
            }
        }

        internal object Clear : AnsiCsiCommand(Colors.Fg.Clear) {
            override fun applyTo(state: SectionState, renderer: Renderer<*>) {
                state.deferred.fgColor = null
            }
        }

        val Black = Color(Colors.Fg.Black)
        val Red = Color(Colors.Fg.Red)
        val Green = Color(Colors.Fg.Green)
        val Yellow = Color(Colors.Fg.Yellow)
        val Blue = Color(Colors.Fg.Blue)
        val Magenta = Color(Colors.Fg.Magenta)
        val Cyan = Color(Colors.Fg.Cyan)
        val White = Color(Colors.Fg.White)

        val BrightBlack = Color(Colors.Fg.BrightBlack)
        val BrightRed = Color(Colors.Fg.BrightRed)
        val BrightGreen = Color(Colors.Fg.BrightGreen)
        val BrightYellow = Color(Colors.Fg.BrightYellow)
        val BrightBlue = Color(Colors.Fg.BrightBlue)
        val BrightMagenta = Color(Colors.Fg.BrightMagenta)
        val BrightCyan = Color(Colors.Fg.BrightCyan)
        val BrightWhite = Color(Colors.Fg.BrightWhite)
    }

    object Bg {
        internal class Color(csiCode: Csi.Code) : AnsiCsiCommand(csiCode) {
            companion object {
                fun lookup(index: Int) = Color(Colors.Bg.lookup(index))
                fun truecolor(r: Int, g: Int, b: Int) = Color(Colors.Bg.truecolor(r, g, b))
            }

            override fun applyTo(state: SectionState, renderer: Renderer<*>) {
                state.deferred.bgColor = this
            }
        }

        internal object Clear : AnsiCsiCommand(Colors.Bg.Clear) {
            override fun applyTo(state: SectionState, renderer: Renderer<*>) {
                state.deferred.bgColor = null
            }
        }

        val Black = Color(Colors.Bg.Black)
        val Red = Color(Colors.Bg.Red)
        val Green = Color(Colors.Bg.Green)
        val Yellow = Color(Colors.Bg.Yellow)
        val Blue = Color(Colors.Bg.Blue)
        val Magenta = Color(Colors.Bg.Magenta)
        val Cyan = Color(Colors.Bg.Cyan)
        val White = Color(Colors.Bg.White)

        val BrightBlack = Color(Colors.Bg.BrightBlack)
        val BrightRed = Color(Colors.Bg.BrightRed)
        val BrightGreen = Color(Colors.Bg.BrightGreen)
        val BrightYellow = Color(Colors.Bg.BrightYellow)
        val BrightBlue = Color(Colors.Bg.BrightBlue)
        val BrightMagenta = Color(Colors.Bg.BrightMagenta)
        val BrightCyan = Color(Colors.Bg.BrightCyan)
        val BrightWhite = Color(Colors.Bg.BrightWhite)
    }

    val Invert = object : AnsiCsiCommand(Colors.Invert) {
        override fun applyTo(state: SectionState, renderer: Renderer<*>) {
            state.deferred.inverted = this
        }
    }

    val ClearInvert = object : AnsiCsiCommand(Colors.ClearInvert) {
        override fun applyTo(state: SectionState, renderer: Renderer<*>) {
            state.deferred.inverted = null
        }
    }
}
