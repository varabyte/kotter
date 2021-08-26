package com.varabyte.konsole.core

import com.varabyte.konsole.ansi.Ansi
import com.varabyte.konsole.core.input.CharKey
import com.varabyte.konsole.core.input.Key
import com.varabyte.konsole.core.input.Keys
import com.varabyte.konsole.core.internal.executor.KonsoleExecutor
import com.varabyte.konsole.terminal.SystemTerminal
import com.varabyte.konsole.terminal.Terminal
import com.varabyte.konsole.terminal.swing.SwingTerminal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.ExecutorService

internal object KeyFlowKey : KonsoleData.Key<Flow<Key>> {
    override val lifecycle = KonsoleApp.Lifecycle
}

class KonsoleApp internal constructor(
    internal val executor: ExecutorService = KonsoleExecutor,
    internal val terminal: Terminal = run {
        try {
            SystemTerminal()
        } catch (ex: Exception) {
            SwingTerminal.create()
        }
    }
) {
    object Lifecycle : KonsoleData.Lifecycle

    internal val data = KonsoleData()

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

    fun konsole(block: KonsoleScope.() -> Unit): KonsoleBlock = KonsoleBlock(this, block)

    /** Create a [KonsoleVar] whose scope is tied to this app. */
    fun <T> KonsoleVar(value: T): KonsoleVar<T> = KonsoleVar(value) { data[ActiveBlockKey] }

    internal fun dispose() {
        data.dispose(Lifecycle)
    }
}

fun konsoleApp(block: KonsoleApp.() -> Unit) {
    val app = KonsoleApp().apply(block)
    app.dispose()
}