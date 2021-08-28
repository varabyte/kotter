package com.varabyte.konsole.runtime.terminal

import kotlinx.coroutines.flow.Flow

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface Terminal : AutoCloseable {
    fun write(text: String)
    fun read(): Flow<Int>
}