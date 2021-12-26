package com.varabyte.kotter.runtime.internal.ansi.commands

import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi

internal open class AnsiCommand(ansiCode: String) : TerminalCommand(ansiCode)
internal open class AnsiCsiCommand(csiCode: Csi.Code) : AnsiCommand(csiCode.toFullEscapeCode())