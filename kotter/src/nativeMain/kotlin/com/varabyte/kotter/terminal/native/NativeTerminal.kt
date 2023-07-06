package com.varabyte.kotter.terminal.native

import com.varabyte.kotter.runtime.terminal.*

expect class NativeTerminal constructor() : Terminal

class CreateNativeTerminalException :
    Exception("Kotter applications must run in an interactive terminal. Legacy terminals may not be supported. Check that you're running the binary directly and not via an external runner, like Gradle?")
