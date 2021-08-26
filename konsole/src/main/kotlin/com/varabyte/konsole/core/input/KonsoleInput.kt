package com.varabyte.konsole.core.input

import com.varabyte.konsole.ansi.commands.invert
import com.varabyte.konsole.ansi.commands.text
import com.varabyte.konsole.core.KonsoleData
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.KonsoleVar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** State needed to support the `input()` function */
private class InputState {
    var text by KonsoleVar("")
    var index by KonsoleVar(0)
}

private object InputStateKey : KonsoleData.Key<InputState>
private object KeyReaderJobKey : KonsoleData.Key<Job>

/**
 * If necessary, instantiate data that the [input] method expects to exist.
 *
 * Is a no-op after the first time.
 */
private fun KonsoleScope.prepareInput() {
    data.putIfAbsent(InputStateKey) { InputState() }
    data.putIfAbsent(
        KeyReaderJobKey,
        provideInitialValue = {
            CoroutineScope(Dispatchers.IO).launch {
                keyFlow.collect { key ->
                    data.get(InputStateKey) {
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

                            Keys.ENTER -> {
                                text = ""
                                index = 0
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
    data.get(InputStateKey) {
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