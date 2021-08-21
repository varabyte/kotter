package com.varabyte.konsole.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

var DefaultTerminalIOProvider: () -> TerminalIO = { SystemTerminalIO() }
val DefaultTerminalIO by lazy { DefaultTerminalIOProvider() }

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface TerminalIO {
    fun write(text: String)
    fun read(): Flow<Int>
}

/**
 * A class which interacts directly with the underlying system terminal, e.g. println
 */
class SystemTerminalIO : TerminalIO {
    override fun write(text: String) {
        print(text)
        System.out.flush()
    }

    override fun read(): Flow<Int> = callbackFlow {
        TODO("Not yet implemented")
    }
}

/**
 * A class which delegates to a virtual terminal that is opened in a standalone window.
 */
class VirtualTerminalIO : TerminalIO {
    override fun write(text: String) {
        TODO("Not yet implemented")
    }

    override fun read(): Flow<Int> {
        TODO("Not yet implemented")
    }
}