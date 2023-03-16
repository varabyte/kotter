package com.varabyte.kotter.terminal.native

internal actual val TIOCGWINSZ: ULong = platform.posix.TIOCGWINSZ.toULong()