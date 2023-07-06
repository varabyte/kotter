package com.varabyte.kotter.foundation

import com.varabyte.kotter.runtime.terminal.*
import com.varabyte.kotter.terminal.native.*

internal actual val defaultTerminalProviders: List<() -> Terminal> = listOf({ NativeTerminal() })
