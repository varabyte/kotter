package com.varabyte.kotter.runtime.internal.ansi.commands

import com.varabyte.kotter.runtime.internal.TerminalCommand
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Osc

internal open class AnsiCommand(ansiCode: String) : TerminalCommand(ansiCode)
internal open class AnsiCsiCommand(csiCode: Csi.Code) : AnsiCommand(csiCode.toFullEscapeCode())
internal open class AnsiOscCommand(oscCode: Osc.Code) : AnsiCommand(oscCode.toFullEscapeCode())