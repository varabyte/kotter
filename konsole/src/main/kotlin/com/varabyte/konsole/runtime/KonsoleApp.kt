package com.varabyte.konsole.runtime

import com.varabyte.konsole.runtime.concurrent.ConcurrentData
import com.varabyte.konsole.runtime.terminal.Terminal
import java.util.concurrent.ExecutorService

class KonsoleApp internal constructor(internal val executor: ExecutorService, internal val terminal: Terminal) {
    /**
     * A long-lived lifecycle that sticks around for the length of the entire app.
     *
     * This lifecycle can be used for storing data that should live across multiple blocks.
     */
    object Lifecycle : ConcurrentData.Lifecycle

    internal val data = ConcurrentData()
    internal val activeBlock: KonsoleBlock? get() = data[ActiveBlockKey]

    init {
        data.start(Lifecycle)
    }

    fun konsole(block: RenderScope.() -> Unit): KonsoleBlock = KonsoleBlock(this, block)

    internal fun dispose() {
        data.dispose(Lifecycle)
        terminal.close()
    }
}