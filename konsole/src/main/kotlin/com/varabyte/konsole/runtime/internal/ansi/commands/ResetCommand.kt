package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.internal.ansi.Ansi

internal val RESET_COMMAND = AnsiCsiCommand(Ansi.Csi.Codes.Sgr.RESET)