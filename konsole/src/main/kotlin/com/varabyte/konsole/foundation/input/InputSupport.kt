package com.varabyte.konsole.foundation.input

import com.varabyte.konsole.foundation.text.invert
import com.varabyte.konsole.foundation.text.text
import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.KonsoleBlock
import com.varabyte.konsole.runtime.RenderScope
import com.varabyte.konsole.runtime.concurrent.ConcurrentData
import com.varabyte.konsole.runtime.internal.ansi.Ansi
import com.varabyte.konsole.runtime.runUntilSignal
import com.varabyte.konsole.runtime.terminal.Terminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

private object KeyFlowKey : ConcurrentData.Key<Flow<Key>> {
    // Once created, we keep it alive for the app, because Flow is designed to be collected multiple times, meaning
    // there's no reason for us to keep recreating it. It's pretty likely that if an app uses input in one block, it
    // will use input again in others. (We can always revisit this decision later and scope this to a KonsoleBlock
    // lifecycle)
    override val lifecycle = KonsoleApp.Lifecycle
}

/**
 * Create a [Flow<Key>] value which converts the ANSI values read from a terminal into Konsole's simpler abstraction
 * (which is a flat collection of keys instead of multi-encoded bytes and other historical legacy).
 */
private fun ConcurrentData.prepareKeyFlow(terminal: Terminal) {
    tryPut(KeyFlowKey) {
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
private fun ConcurrentData.prepareInput(terminal: Terminal, onInputChanged: () -> Unit) {
    if (!tryPut(OnlyCalledOncePerRenderKey) { }) {
        throw IllegalStateException("Calling `input` more than once in a render pass is not supported")
    }

    prepareKeyFlow(terminal)
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
    data.prepareInput(terminal, onInputChanged = { requestRerender() })

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

/**
 * Start running a job that collects keypresses and sends them to callbacks.
 *
 * This is a no-op when called after the first time.
 */
private fun ConcurrentData.prepareOnKeyPressed(terminal: Terminal) {
    prepareKeyFlow(terminal)
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
    data.prepareOnKeyPressed(terminal)
    if (!data.tryPut(KeyPressedCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onKeyPressed` callback at a time is supported.")
    }
}

fun KonsoleBlock.runUntilKeyPressed(vararg keys: Key, block: suspend KonsoleBlock.RunScope.() -> Unit) {
    runUntilSignal {
        data.prepareOnKeyPressed(terminal)
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