package com.varabyte.konsole.ansi.commands

import com.varabyte.konsole.ansi.Ansi.Csi
import com.varabyte.konsole.core.KonsoleCommand
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

open class AnsiCommand(private val ansiCode: String) : KonsoleCommand {
    final override fun applyTo(textArea: MutableKonsoleTextArea) {
        textArea.append(ansiCode)
    }
}

open class AnsiCsiCommand(csiCode: Csi.Code) : AnsiCommand(csiCode.toFullEscapeCode())