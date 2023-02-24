package com.varabyte.kotter.terminal.native

import com.varabyte.kotter.runtime.terminal.Terminal
import kotlinx.coroutines.flow.Flow

class NativeTerminal : Terminal {
    override fun write(text: String) {
        print(text)
    }

    override fun read(): Flow<Int> {
        TODO("Not yet implemented")
    }

    override fun clear() {
    }

    override fun close() {
    }
}