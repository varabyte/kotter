package com.varabyte.konsole.terminal

val DefaultTerminalIO by lazy { SystemTerminalIO() }

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface TerminalIO {
    fun write(text: String)
    suspend fun read(): Int
}

/**
 * A class which interacts directly with the underlying system terminal, e.g. println
 */
class SystemTerminalIO : TerminalIO {
    override fun write(text: String) {
        print(text)
        System.out.flush()
    }

    override suspend fun read(): Int {
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

    override suspend fun read(): Int {
        TODO("Not yet implemented")
    }
}