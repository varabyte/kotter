package com.varabyte.konsole.core.input

import com.varabyte.konsole.ansi.commands.invert
import com.varabyte.konsole.ansi.commands.text
import com.varabyte.konsole.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** State needed to support the `input()` function */
private class InputState(scope: KonsoleScope) {
    object Key : KonsoleData.Key<InputState> {
        override val lifecycle = KonsoleBlock.Lifecycle
    }
    var text by scope.KonsoleVar("")
    var index by scope.KonsoleVar(0)
}

private object KeyReaderJobKey : KonsoleData.Key<Job> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

/**
 * If necessary, instantiate data that the [input] method expects to exist.
 *
 * Is a no-op after the first time.
 */
private fun KonsoleScope.prepareInput() {
    data.tryPut(InputState.Key) { InputState(this) }
    data.tryPut(
        KeyReaderJobKey,
        provideInitialValue = {
            CoroutineScope(Dispatchers.IO).launch {
                data.getValue(KeyFlowKey).collect { key ->
                    data.get(InputState.Key) {
                        when (key) {
                            Keys.LEFT -> index = (index - 1).coerceAtLeast(0)
                            Keys.RIGHT -> index = (index + 1).coerceAtMost(text.length)
                            Keys.HOME -> index = 0
                            Keys.END -> index = text.length
                            Keys.DELETE -> {
                                if (index <= text.lastIndex) {
                                    text = text.removeRange(index, index + 1)
                                }
                            }

                            Keys.BACKSPACE -> {
                                if (index > 0) {
                                    text = text.removeRange(index - 1, index)
                                    index--
                                }
                            }
                            else ->
                                if (key is CharKey) {
                                    text = "${text.take(index)}${key.code}${text.takeLast(text.length - index)}"
                                    index += 1 // Otherwise, already index = 0, first char
                                }
                        }
                    }
                }
            }
        },
        dispose = { job -> job.cancel() }
    )
}

fun KonsoleScope.input() {
    prepareInput()

    val self = this
    data.get(InputState.Key) {
        for (i in 0 until index) {
            self.text(text[i])
        }
        self.invert()
        self.text(text.elementAtOrNull(index) ?: ' ')
        self.invert()
        for (i in (index + 1)..text.lastIndex) {
            self.text(text[i])
        }
    }
}