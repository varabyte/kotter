package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.kotter.terminal.native.NativeTerminal

internal actual val defaultTerminalProviders: List<() -> Terminal> = listOf({ NativeTerminal() })