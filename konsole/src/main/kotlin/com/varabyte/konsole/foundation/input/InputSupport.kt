package com.varabyte.konsole.foundation.input

import com.varabyte.konsole.foundation.text.invert
import com.varabyte.konsole.foundation.text.text
import com.varabyte.konsole.runtime.KeyFlowKey
import com.varabyte.konsole.runtime.KonsoleBlock
import com.varabyte.konsole.runtime.RenderScope
import com.varabyte.konsole.runtime.concurrent.ConcurrentData
import com.varabyte.konsole.runtime.runUntilSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** State needed to support the `input()` function */
private class InputState {
    object Key : ConcurrentData.Key<InputState> {
        override val lifecycle = KonsoleBlock.Lifecycle
    }

    var text = ""
    var index = 0
}

private object UpdateInputJobKey : ConcurrentData.Key<Job> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

private object OnlyCalledOncePerRenderKey : ConcurrentData.Key<Unit> {
    override val lifecycle = RenderScope.Lifecycle
}

/**
 * If necessary, instantiate data that the [input] method expects to exist.
 *
 * Is a no-op after the first time.
 */
private fun ConcurrentData.prepareInput(onInputChanged: () -> Unit) {
    if (!tryPut(OnlyCalledOncePerRenderKey) { }) {
        throw IllegalStateException("Calling `input` more than once in a render pass is not supported")
    }
    tryPut(InputState.Key) { InputState() }
    tryPut(
        UpdateInputJobKey,
        provideInitialValue = {
            CoroutineScope(Dispatchers.IO).launch {
                getValue(KeyFlowKey).collect { key ->
                    get(InputState.Key) {
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
                                get(InputEnteredCallbackKey) { this.invoke(OnInputEnteredScope(text)) }
                                get(SystemInputEnteredCallbackKey) { this.invoke() }
                            }
                            else ->
                                if (key is CharKey) {
                                    proposedText = "${text.take(index)}${key.code}${text.takeLast(text.length - index)}"
                                    proposedIndex = index + 1
                                }
                        }

                        if (proposedText != null) {
                            get(InputChangedCallbacksKey) {
                                val scope = OnInputChangedScope(input = proposedText!!, prevInput = text)
                                forEach { callback -> scope.callback() }

                                proposedText = if (!scope.rejected) scope.input else scope.prevInput
                            }

                            if (proposedText != text) {
                                text = proposedText!!
                                index = (proposedIndex ?: index).coerceIn(0, text.length)
                                onInputChanged()
                            }
                        }
                    }
                }
            }
        },
        dispose = { job -> job.cancel() }
    )
}

fun RenderScope.input() {
    data.prepareInput(onInputChanged = { requestRerender() })

    val self = this
    data.get(InputState.Key) {
        for (i in 0 until index) {
            self.text(text[i])
        }
        invert { self.text(text.elementAtOrNull(index) ?: ' ') }
        for (i in (index + 1)..text.lastIndex) {
            self.text(text[i])
        }
    }
}

class OnKeyPressedScope(val key: Key)

private object KeyPressedJobKey : ConcurrentData.Key<Job> {
    override val lifecycle = KonsoleBlock.Lifecycle
}
private object KeyPressedCallbackKey : ConcurrentData.Key<OnKeyPressedScope.() -> Unit> {
    override val lifecycle = KonsoleBlock.Lifecycle
}
// Note: We create a separate key here from above to ensure we can trigger the system callback only AFTER the user
// callback was triggered. That's because the system handler may fire a signal which, if sent out too early, could
// result in the user callback not getting a chance to run.
private object SystemKeyPressedCallbackKey : ConcurrentData.Key<OnKeyPressedScope.() -> Unit> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

/** Start running a job that collects keypresses and sends them to callbacks.
 *
 * This is a no-op when called after the first time.
 */
private fun ConcurrentData.prepareOnKeyPressed() {
    tryPut(
        KeyPressedJobKey,
        provideInitialValue = {
            CoroutineScope(Dispatchers.IO).launch {
                getValue(KeyFlowKey).collect { key ->
                    val scope = OnKeyPressedScope(key)
                    get(KeyPressedCallbackKey) { this.invoke(scope) }
                    get(SystemKeyPressedCallbackKey) { this.invoke(scope) }
                }
            }
        },
        dispose = { job -> job.cancel() }
    )
}

fun KonsoleBlock.RunScope.onKeyPressed(listener: OnKeyPressedScope.() -> Unit) {
    data.prepareOnKeyPressed()
    if (!data.tryPut(KeyPressedCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onKeyPressed` callback at a time is supported.")
    }
}

fun KonsoleBlock.runUntilKeyPressed(vararg keys: Key, block: suspend KonsoleBlock.RunScope.() -> Unit) {
    runUntilSignal {
        data.prepareOnKeyPressed()
        data[SystemKeyPressedCallbackKey] = { if (keys.contains(key)) signal() }
        block()
    }
}

class OnInputChangedScope(var input: String, val prevInput: String) {
    internal var rejected = false
    fun rejectInput() { rejected = true }
}
private object InputChangedCallbacksKey : ConcurrentData.Key<MutableList<OnInputChangedScope.() -> Unit>> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

fun KonsoleBlock.RunScope.onInputChanged(listener: OnInputChangedScope.() -> Unit) {
    data.putIfAbsent(InputChangedCallbacksKey, provideInitialValue = { mutableListOf() }) { add(listener) }
}

class OnInputEnteredScope(val input: String)
private object InputEnteredCallbackKey : ConcurrentData.Key<OnInputEnteredScope.() -> Unit> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

// Note: We create a separate key here from above to ensure we can trigger the system callback only AFTER the user
// callback was triggered. That's because the system handler may fire a signal which, if sent out too early, could
// result in the user callback not getting a chance to run.
private object SystemInputEnteredCallbackKey : ConcurrentData.Key<() -> Unit> {
    override val lifecycle = KonsoleBlock.Lifecycle
}

fun KonsoleBlock.RunScope.onInputEntered(listener: OnInputEnteredScope.() -> Unit) {
    if (!data.tryPut(InputEnteredCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onInputEntered` callback at a time is supported.")
    }
}

fun KonsoleBlock.runUntilInputEntered(block: suspend KonsoleBlock.RunScope.() -> Unit) {
    runUntilSignal {
        data[SystemInputEnteredCallbackKey] = { signal() }
        block()
    }
}