package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.ansi.Ansi.Csi
import com.varabyte.konsole.runtime.internal.text.MutableTextArea

internal open class AnsiCommand(private val ansiCode: String) : KonsoleCommand {
    final override fun applyTo(textArea: MutableTextArea) {
        textArea.append(ansiCode)
    }
}

internal open class AnsiCsiCommand(csiCode: Csi.Code) : AnsiCommand(csiCode.toFullEscapeCode())