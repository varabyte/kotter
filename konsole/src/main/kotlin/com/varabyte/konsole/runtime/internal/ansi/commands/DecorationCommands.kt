package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.runtime.render.Renderer

internal val BOLD_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.BOLD) {
    override fun applyTo(state: KonsoleState, renderer: Renderer) {
        state.deferred.bolded = this
    }
}

internal val CLEAR_BOLD_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.CLEAR_BOLD) {
    override fun applyTo(state: KonsoleState, renderer: Renderer) {
        state.deferred.bolded = null
    }
}

internal val UNDERLINE_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.UNDERLINE) {
    override fun applyTo(state: KonsoleState, renderer: Renderer) {
        state.deferred.underlined = this
    }
}

internal val CLEAR_UNDERLINE_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.CLEAR_UNDERLINE) {
    override fun applyTo(state: KonsoleState, renderer: Renderer) {
        state.deferred.underlined = null
    }
}

internal val STRIKETHROUGH_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.STRIKETHROUGH) {
    override fun applyTo(state: KonsoleState, renderer: Renderer) {
        state.deferred.struckThrough = this
    }
}

internal val CLEAR_STRIKETHROUGH_COMMAND = object : AnsiCsiCommand(Ansi.Csi.Codes.Sgr.Decorations.CLEAR_STRIKETHROUGH) {
    override fun applyTo(state: KonsoleState, renderer: Renderer) {
        state.deferred.struckThrough = null
    }
}