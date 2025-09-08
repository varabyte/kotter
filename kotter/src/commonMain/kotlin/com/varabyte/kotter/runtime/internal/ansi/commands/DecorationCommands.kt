package com.varabyte.kotter.runtime.internal.ansi.commands

import com.varabyte.kotter.runtime.SectionState
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.render.Renderer

internal object DecorationCommands {
    internal val Bold = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.Bold) {
        override fun applyTo(state: SectionState, renderer: Renderer<*>) {
            state.deferred.bolded = this
        }
    }

    internal val ClearBold = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.ClearBold) {
        override fun applyTo(state: SectionState, renderer: Renderer<*>) {
            state.deferred.bolded = null
        }
    }

    internal val Underline = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.Underline) {
        override fun applyTo(state: SectionState, renderer: Renderer<*>) {
            state.deferred.underlined = this
        }
    }

    internal val ClearUnderline = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.ClearUnderline) {
        override fun applyTo(state: SectionState, renderer: Renderer<*>) {
            state.deferred.underlined = null
        }
    }

    internal val Strikethrough = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.Strikethrough) {
        override fun applyTo(state: SectionState, renderer: Renderer<*>) {
            state.deferred.struckThrough = this
        }
    }

    internal val ClearStrikethrough = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.ClearStrikethrough) {
        override fun applyTo(state: SectionState, renderer: Renderer<*>) {
            state.deferred.struckThrough = null
        }
    }
}

