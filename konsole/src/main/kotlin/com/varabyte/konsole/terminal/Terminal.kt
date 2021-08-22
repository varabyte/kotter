package com.varabyte.konsole.terminal

import com.varabyte.konsole.KonsoleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

val DefaultTerminalIO by lazy { KonsoleSettings.provideTerminal() }

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface Terminal {
    fun write(text: String)
    fun read(): Flow<Int>
}

/**
 * A class which interacts directly with the underlying system terminal, e.g. println
 */
class SystemTerminal : Terminal {
    override fun write(text: String) {
        print(text)
        System.out.flush()
    }

    override fun read(): Flow<Int> = callbackFlow {
        TODO("Not yet implemented")
    }
}
