package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.kotter.terminal.system.SystemTerminal
import com.varabyte.kotter.terminal.virtual.VirtualTerminal

internal actual val defaultTerminalProviders: List<() -> Terminal>
    get() = listOf({ SystemTerminal() }, { VirtualTerminal.create() })
