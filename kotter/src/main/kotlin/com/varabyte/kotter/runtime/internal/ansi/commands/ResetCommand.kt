package com.varabyte.kotter.runtime.internal.ansi.commands

import com.varabyte.kotter.runtime.internal.ansi.Ansi

internal val RESET_COMMAND = AnsiCsiCommand(Ansi.Csi.Codes.Sgr.RESET)