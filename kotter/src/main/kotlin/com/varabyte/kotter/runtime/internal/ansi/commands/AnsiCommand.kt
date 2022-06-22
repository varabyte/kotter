package com.varabyte.kotter.runtime.internal.ansi.commands

import com.varabyte.kotter.runtime.SectionState
import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Osc
import com.varabyte.kotter.runtime.render.Renderer
import java.net.URI

internal open class AnsiCommand(ansiCode: String) : TerminalCommand(ansiCode)
internal open class AnsiCsiCommand(csiCode: Csi.Code) : AnsiCommand(csiCode.toFullEscapeCode())

internal open class AnsiOscCommand(oscCode: Osc.Code) : AnsiCommand(oscCode.toFullEscapeCode())

internal open class AnchorCommand(uri: URI? = null, params: Map<String, String>? = null) :
    AnsiOscCommand(
        Osc.Code(
            Osc.ANCHOR.id,
            Osc.Code.Parts(
                listOf(
                    params?.map { "${it.key}=${it.value}" }?.joinToString(":") ?: "",
                    uri?.toString() ?: "",
                )
            )
        )
    ) {

    override fun applyTo(state: SectionState, renderer: Renderer<*>) {
        state.deferred.anchor = this
    }
}