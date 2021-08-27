package com.varabyte.konsole.core.input

import com.varabyte.konsole.ansi.commands.invert
import com.varabyte.konsole.ansi.commands.text
import com.varabyte.konsole.core.*
import com.varabyte.konsole.core.KeyFlowKey
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

private object UpdateInputJobKey : KonsoleData.Key<Job> {
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
        UpdateInputJobKey,
        provideInitialValue = {
            CoroutineScope(Dispatchers.IO).launch {
                data.getValue(KeyFlowKey).collect { key ->
                    data.get(InputState.Key) {
                        var proposedText: String? = null
                        var proposedIndex: Int? = null
                        when (key) {
                            Keys.LEFT -> index = (index - 1).coerceAtLeast(0)
                            Keys.RIGHT -> index = (index + 1).coerceAtMost(text.length)
                            Keys.HOME -> index = 0
                            Keys.END -> index = text.length
                            Keys.DELETE -> {
                                if (index <= text.lastIndex) {
                                    proposedText = text.removeRange(index, index + 1)
                                }
                            }

                            Keys.BACKSPACE -> {
                                if (index > 0) {
                                    proposedText = text.removeRange(index - 1, index)
                                    proposedIndex = index - 1
                                }
                            }

                            Keys.ENTER -> {
                                data.get(InputEnteredCallbacksKey) {
                                    val scope = OnInputEnteredScope(text)
                                    forEach { callback -> scope.callback() }
                                }
                            }
                            else ->
                                if (key is CharKey) {
                                    proposedText = "${text.take(index)}${key.code}${text.takeLast(text.length - index)}"
                                    proposedIndex = index + 1
                                }
                        }

                        if (proposedText != null) {
                            data.get(InputChangedCallbacksKey) {
                                val scope = OnInputChangedScope(input = proposedText!!, prevInput = text)
                                forEach { callback -> scope.callback() }

                                proposedText = if (!scope.rejected) scope.input else scope.prevInput
                            }

                            if (proposedText != text) {
                                text = proposedText!!
                                index = (proposedIndex ?: index).coerceIn(0, text.length)
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

private object KeyPressedJobKey : KonsoleData.Key<Job> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

class OnKeyPressedScope(val key: Key)
fun KonsoleBlock.RunScope.onKeyPressed(listener: OnKeyPressedScope.() -> Unit) {
    if (!data.tryPut(
            KeyPressedJobKey,
            provideInitialValue = {
                CoroutineScope(Dispatchers.IO).launch {
                    data.getValue(KeyFlowKey).collect { key -> OnKeyPressedScope(key).listener() }
                }
            },
            dispose = { job -> job.cancel() }
        )
    ) {
        throw IllegalStateException("Currently only one `onKeyPressed` callback at a time is supported.")
    }
}

class OnInputChangedScope(var input: String, val prevInput: String) {
    internal var rejected = false
    fun rejectInput() { rejected = true }
}
private object InputChangedCallbacksKey : KonsoleData.Key<MutableList<OnInputChangedScope.() -> Unit>> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

fun KonsoleBlock.RunScope.onInputChanged(listener: OnInputChangedScope.() -> Unit) {
    data.putIfAbsent(InputChangedCallbacksKey, provideInitialValue = { mutableListOf() }) { add(listener) }
}

class OnInputEnteredScope(val input: String)
private object InputEnteredCallbacksKey : KonsoleData.Key<MutableList<OnInputEnteredScope.() -> Unit>> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

fun KonsoleBlock.RunScope.onInputEntered(listener: OnInputEnteredScope.() -> Unit) {
    data.putIfAbsent(InputEnteredCallbacksKey, provideInitialValue = { mutableListOf() }) { add(listener) }
}

fun KonsoleBlock.runUntilTextEntered(block: suspend KonsoleBlock.RunScope.() -> Unit) {
    runUntilSignal {
        onInputEntered { signal() }
        block()
    }
}