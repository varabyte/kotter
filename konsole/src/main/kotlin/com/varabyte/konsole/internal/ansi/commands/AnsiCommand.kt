package com.varabyte.konsole.internal.ansi.commands

import com.varabyte.konsole.internal.KonsoleCommand
import com.varabyte.konsole.internal.MutableKonsoleTextArea
import com.varabyte.konsole.internal.ansi.Ansi.Csi

internal open class AnsiCommand(private val ansiCode: String) : KonsoleCommand {
    final override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(ansiCode)
    }
}

internal open class AnsiCsiCommand(csiCode: Csi.Code) : AnsiCommand(csiCode.toFullEscapeCode())