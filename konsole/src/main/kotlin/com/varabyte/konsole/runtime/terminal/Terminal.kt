package com.varabyte.konsole.runtime.terminal

import kotlinx.coroutines.flow.Flow

/**
 * An interface for abstracting input and output for various terminal implementations.
 */
interface Terminal : AutoCloseable {
    val width: Int get() = Int.MAX_VALUE
    fun write(text: String)
    fun read(): Flow<Int>
}