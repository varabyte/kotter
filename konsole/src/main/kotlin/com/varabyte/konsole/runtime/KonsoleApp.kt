package com.varabyte.konsole.runtime

import com.varabyte.konsole.foundation.input.CharKey
import com.varabyte.konsole.foundation.input.Key
import com.varabyte.konsole.foundation.input.Keys
import com.varabyte.konsole.runtime.concurrent.ConcurrentData
import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.runtime.terminal.Terminal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.ExecutorService

internal object KeyFlowKey : ConcurrentData.Key<Flow<Key>> {
    override val lifecycle = KonsoleApp.Lifecycle
}

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
        data.lazyInitializers[KeyFlowKey] = {
            val escSeq = StringBuilder()
            terminal.read().mapNotNull { byte ->
                val c = byte.toChar()
                when {
                    escSeq.isNotEmpty() -> {
                        escSeq.append(c)
                        val code = Ansi.EscSeq.toCode(escSeq)
                        if (code != null) {
                            escSeq.clear()
                            when (code) {
                                Ansi.Csi.Codes.Keys.UP -> Keys.UP
                                Ansi.Csi.Codes.Keys.DOWN -> Keys.DOWN
                                Ansi.Csi.Codes.Keys.LEFT -> Keys.LEFT
                                Ansi.Csi.Codes.Keys.RIGHT -> Keys.RIGHT
                                Ansi.Csi.Codes.Keys.HOME -> Keys.HOME
                                Ansi.Csi.Codes.Keys.END -> Keys.END
                                Ansi.Csi.Codes.Keys.DELETE -> Keys.DELETE
                                else -> null
                            }
                        } else {
                            null
                        }
                    }
                    else -> {
                        when (c) {
                            Ansi.CtrlChars.ESC -> {
                                escSeq.append(c); null
                            }
                            Ansi.CtrlChars.ENTER -> Keys.ENTER
                            Ansi.CtrlChars.BACKSPACE -> Keys.BACKSPACE
                            else -> if (!c.isISOControl()) CharKey(c) else null
                        }
                    }
                }
            }
        }
    }

    fun konsole(block: RenderScope.() -> Unit): KonsoleBlock = KonsoleBlock(this, block)

    internal fun dispose() {
        data.dispose(Lifecycle)
        terminal.close()
    }
}