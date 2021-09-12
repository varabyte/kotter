package com.varabyte.konsole.runtime

import com.varabyte.konsole.runtime.concurrent.ConcurrentScopedData
import com.varabyte.konsole.runtime.internal.executor.KonsoleExecutor
import com.varabyte.konsole.runtime.terminal.Terminal

class KonsoleApp internal constructor(internal val terminal: Terminal) {
    /**
     * A long-lived lifecycle that sticks around for the length of the entire app.
     *
     * This lifecycle can be used for storing data that should live across multiple blocks.
     */
    object Lifecycle : ConcurrentScopedData.Lifecycle
    val data = ConcurrentScopedData()

    internal val executor = KonsoleExecutor
    internal val activeBlock: KonsoleBlock? get() = data[ActiveBlockKey]

    init {
        data.start(Lifecycle)
    }

    internal fun assertNoActiveBlocks() {
        check(!data.isActive(KonsoleBlock.Lifecycle)) {
            "A previous konsole block was never finished. Did you forget to call `run` on it?"
        }
    }

    fun konsole(block: RenderScope.() -> Unit): KonsoleBlock {
        assertNoActiveBlocks()
        return KonsoleBlock(this, block)
    }

    internal fun dispose() {
        // Protect against dispose being called multiple times
        if (data.isActive(Lifecycle)) {
            data.stopAll()
            terminal.close()
        }
    }
}