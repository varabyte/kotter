package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.terminal.*
import com.varabyte.kotter.terminal.system.*
import com.varabyte.kotter.terminal.virtual.*

internal actual val defaultTerminalProviders: List<() -> Terminal>
    get() = listOf({ SystemTerminal() }, { VirtualTerminal.create() })
