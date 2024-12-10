package com.varabyte.kotter.foundation.input

import com.varabyte.kotter.foundation.anim.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotter.platform.concurrent.locks.*
import com.varabyte.kotter.platform.internal.system.*
import com.varabyte.kotter.runtime.*
import com.varabyte.kotter.runtime.concurrent.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotter.runtime.internal.text.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.min
import kotlin.time.Duration

// A channel of keys that any coroutine can write to or read from.
private val KeyFlowKey = Section.Lifecycle.createKey<MutableSharedFlow<Key>>()

// A job that reads system input and converts incoming byte values into Kotter `Key` instances.
private val ReadKeyJobKey = Section.Lifecycle.createKey<Job>()
private val ReadKeyJobReadyKey = Section.Lifecycle.createKey<CompletableDeferred<Unit>>()

// The job that consumes `KeyFlowKey`'s `Key` output and uses it to affect the active `input`, if any is declared in the
// section block.
private val UpdateInputJobKey = Section.Lifecycle.createKey<Job>()
private val UpdateInputJobReadyKey = Section.Lifecycle.createKey<CompletableDeferred<Unit>>()

private val DefaultPageSizeKey = Session.Lifecycle.createKey<Int>()

/**
 * Create a [Flow<Key>] value which converts bytes read from a terminal into keys, handling some gnarly multibyte
 * cases and smoothing over other inconsistent, historical legacy.
 */
private fun ConcurrentScopedData.prepareKeyFlow(section: Section) {
    val terminal = section.session.terminal
    run {
        tryPut(KeyFlowKey) { MutableSharedFlow() }
        tryPut(ReadKeyJobReadyKey) { CompletableDeferred() }
        tryPut(ReadKeyJobKey, provideInitialValue = {
            val keyLock = ReentrantLock()
            val escSeq = StringBuilder()
            var lastKeyTime: Long
            section.coroutineScope.launch {
                terminal.read()
                    .onSubscription { getValue(ReadKeyJobReadyKey).complete(Unit) }
                    .collect { byte ->
                        val c = byte.toChar()
                        val key = keyLock.withLock {
                            lastKeyTime = getCurrentTimeMs()
                            when {
                                escSeq.isNotEmpty() -> {
                                    // Normally, we get here if we're continuing an existing esc sequence, but if so
                                    // some reason a previous one was never consumed *and* we are starting a new ESC
                                    // sequence, just clear out anything left over from before. This could happen for
                                    // example if the user just pressed ESC (which puts an ESC in the escSeq queue and
                                    // waits a while before sending it out), but maybe also we end up getting an escape
                                    // sequence that we didn't know how to handle, and without doing this, that old
                                    // sequence would block us from working ever again.
                                    if (c == Ansi.CtrlChars.ESC) escSeq.clear()

                                    escSeq.append(c)
                                    val code = Ansi.EscSeq.toCsiCode(escSeq)
                                    if (code != null) {
                                        escSeq.clear()
                                        when (code) {
                                            Ansi.Csi.Codes.Keys.UP -> Keys.UP
                                            Ansi.Csi.Codes.Keys.DOWN -> Keys.DOWN
                                            Ansi.Csi.Codes.Keys.LEFT -> Keys.LEFT
                                            Ansi.Csi.Codes.Keys.RIGHT -> Keys.RIGHT
                                            Ansi.Csi.Codes.Keys.HOME, Ansi.Csi.Codes.Cursor.MOVE_TO_LINE_START -> Keys.HOME
                                            Ansi.Csi.Codes.Keys.INSERT -> Keys.INSERT
                                            Ansi.Csi.Codes.Keys.DELETE -> Keys.DELETE
                                            Ansi.Csi.Codes.Keys.END, Ansi.Csi.Codes.Cursor.MOVE_TO_LINE_END -> Keys.END
                                            Ansi.Csi.Codes.Keys.PG_UP -> Keys.PAGE_UP
                                            Ansi.Csi.Codes.Keys.PG_DOWN -> Keys.PAGE_DOWN
                                            else -> null
                                        }
                                    } else {
                                        null
                                    }
                                }

                                else -> {
                                    when (c) {
                                        Ansi.CtrlChars.EOF -> Keys.EOF
                                        // Windows uses BACKSPACE, *nix uses DELETE? Best to support both
                                        Ansi.CtrlChars.BACKSPACE, Ansi.CtrlChars.DELETE -> Keys.BACKSPACE
                                        Ansi.CtrlChars.TAB -> Keys.TAB
                                        Ansi.CtrlChars.ENTER -> Keys.ENTER
                                        Ansi.CtrlChars.ESC -> {
                                            escSeq.append(c)
                                            // This is kind of ugly, but we need to detect the difference between the
                                            // user pressing ESC on their own vs it being the first character in a chain
                                            // of an escape sequence generated by the terminal. If the terminal
                                            // generates an escape sequence, the whole thing is consumed sub
                                            // millisecond, so waiting a couple dozen ms to be sure we aren't getting
                                            // any followup characters. Note that a user can hold the keys down which
                                            // generates a bunch of key signals, so we additionally make sure there
                                            // hasn't been any other key pressed.
                                            section.coroutineScope.launch {
                                                val delayMs = 50L
                                                var doneWaiting = false
                                                var sendEsc = false
                                                while (!doneWaiting) {
                                                    delay(10L)
                                                    keyLock.withLock {
                                                        if (getCurrentTimeMs() - lastKeyTime > delayMs) {
                                                            sendEsc =
                                                                escSeq.length == 1 && escSeq.contains(Ansi.CtrlChars.ESC)
                                                            if (sendEsc) escSeq.clear()
                                                            doneWaiting = true
                                                        }
                                                    }
                                                }
                                                if (sendEsc) getValue(KeyFlowKey).emit(Keys.ESC)
                                            }
                                            null
                                        }

                                        else -> if (!c.isISOControl()) CharKey(c) else null
                                    }
                                }
                            }
                        }

                        if (key != null) {
                            getValue(KeyFlowKey).emit(key)
                        }
                    }
            }
        })
    }
}

/**
 * Send one or more [Key] presses programmatically.
 */
fun RunScope.sendKeys(vararg keys: Key) {
    if (keys.isEmpty()) return
    data.waitForInputReady()
    section.coroutineScope.launch {
        val keyFlow = data.getValue(KeyFlowKey)
        keys.forEach { keyFlow.emit(it) }
    }
}

/**
 * @property pageSize How many lines to scroll up / down when the user presses page up / page down.
 */
private class MultilineState(
    private val parentState: InputState,
    var pageSize: Int,
) {
    /**
     * The line position we should try to set our cursor to if we can.
     *
     * Note: In a multiline context, the cursor index is the offset from the beginning of all the text, while the line
     * index is the offset from the last newline.
     *
     * The reason this property is called "ideal" and not actual is because we might be moving up / down several lines,
     * and some intermediate lines are too short to hit it. However, when we go back to a line that is long enough, we
     * should try to set the cursor to this position again.
     */
    var idealLineIndex = 0
        private set

    fun updateIdealLineIndex() {
        idealLineIndex = parentState.text.getLineIndex(parentState.cursorIndex)
    }
}

/** State needed to support the `input()` function */
private class InputState(val id: Any, val cursorState: BlinkingCursorState, multilineConfig: MultilineConfig?) {
    var isActive = false

    val multilineState: MultilineState? = multilineConfig?.let { config -> MultilineState(this, config.pageSize) }

    private var _text = ""
    private var _cursorIndex = 0

    var text
        get() = _text
        set(value) {
            if (_text != value) {
                _text = value
                _cursorIndex = _text.length
                if (isActive) cursorState.resetCursor()
            }
        }

    var cursorIndex
        get() = _cursorIndex
        set(value) {
            @Suppress("NAME_SHADOWING")
            val value = value.coerceAtMost(_text.length)
            if (_cursorIndex != value) {
                _cursorIndex = value
                if (isActive) cursorState.resetCursor()
            }
        }
}

// Exposed for testing
internal const val BLINKING_DURATION_MS = 500

private class BlinkingCursorState {
    var blinkOn = true
    var blinkElapsedMs = 0

    fun resetCursor() {
        blinkOn = true
        blinkElapsedMs = 0
    }

    /** Elapse the timer on this input state's cursor animation, returning true if the cursor actually changed. */
    fun elapse(duration: Duration): Boolean {
        val prevBlinkOn = blinkOn
        blinkElapsedMs += duration.inWholeMilliseconds.toInt()
        while (blinkElapsedMs >= BLINKING_DURATION_MS) {
            blinkElapsedMs -= BLINKING_DURATION_MS
            blinkOn = !blinkOn
        }
        return prevBlinkOn != blinkOn
    }
}

private val InputStatesKey = Section.Lifecycle.createKey<MutableMap<Any, InputState>>()
private val BlinkingCursorStateKey = Section.Lifecycle.createKey<BlinkingCursorState>()
private val InputStatesCalledThisRender = MainRenderScope.Lifecycle.createKey<MutableMap<Any, InputState>>()

private fun ConcurrentScopedData.activate(state: InputState) {
    if (state.isActive) return
    get(InputActivatedCallbackKey) {
        val onInputActivatedScope = OnInputActivatedScope(state.id, state.text)
        this.invoke(onInputActivatedScope)
        state.text = onInputActivatedScope.input
    }
    state.isActive = true
}

private fun ConcurrentScopedData.deactivate(state: InputState) {
    if (!state.isActive) return
    get(InputDeactivatedCallbackKey) {
        val onInputDeactivatedScope = OnInputDeactivatedScope(state.id, state.text)
        this.invoke(onInputDeactivatedScope)
        state.text = onInputDeactivatedScope.input
    }
    state.isActive = false
}

private fun String.getLineStartCursorIndex(cursorIndex: Int): Int {
    val ptr = TextPtr(this, cursorIndex)
    ptr.decrementUntil { it == '\n' }
    // Line start should be AFTER previous line's newline. However, don't increment if we're at the start of the text.
    if (ptr.currChar == '\n') ptr.increment()
    return ptr.charIndex
}

private fun String.getLineEndCursorIndex(cursorIndex: Int): Int {
    val ptr = TextPtr(this, cursorIndex)
    if (ptr.currChar != '\n') {
        ptr.incrementUntil { it == '\n' }
    }
    return ptr.charIndex
}

private fun String.getLineIndex(cursorIndex: Int): Int {
    return cursorIndex - getLineStartCursorIndex(cursorIndex)
}

private fun String.insertAtCursorIndex(cursorIndex: Int, c: Char): String {
    return "${this.substring(0, cursorIndex)}$c${this.substring(cursorIndex)}"
}

/**
 * The default page size for multiline inputs.
 *
 * See also [multilineInput].
 */
var Session.defaultPageSize: Int
    get() {
        return data[DefaultPageSizeKey] ?: 5
    }
    set(value) {
        data[DefaultPageSizeKey] = value
    }

// Semi hack alert: Input tests run too fast, meaning there's a good chance that they'll try to send keypresses before
// we're ready. This method collects all the different completable deferred keys we use for the various systems. This
// should be called liberally anywhere users might call functions to interact with our input system. After this is
// called, it is guaranteed that the various flows behind these systems are hooked up.
// There may be a better way than this but I haven't figured it out yet....
private fun ConcurrentScopedData.waitForInputReady() {
    runBlocking {
        get(ReadKeyJobReadyKey)?.await()
        get(UpdateInputJobReadyKey)?.await()
        get(KeyPressedJobReadyKey)?.await()
    }
}

/**
 * If necessary, instantiate data that the [input] method expects to exist.
 *
 * Is a no-op after the first time.
 */
private fun ConcurrentScopedData.prepareInput(
    scope: MainRenderScope,
    id: Any,
    initialText: String,
    isActive: Boolean,
    multilineConfig: MultilineConfig?,
) {
    val section = scope.section
    prepareKeyFlow(section)

    val cursorState = putOrGet(BlinkingCursorStateKey) {
        val cursorState = BlinkingCursorState()
        // This block represents global state that gets triggered just once for all input blocks in this section, so we
        // do some quick init side effects as well

        addTimer(Anim.ONE_FRAME_60FPS, repeat = true, key = cursorState) {
            if (cursorState.elapse(elapsed)) {
                section.requestRerender()
            }
        }

        section.onRendered {
            // We need to indirectly detect if a previously active input state was not called this frame. This can
            // happen either if we called OTHER input states this frame OR if we called NO input states this frame.
            // We can detect both of these cases by querying the "input states called this render" key.
            val idsRenderedThisFrame = mutableSetOf<Any>()
            remove(InputStatesCalledThisRender) {
                idsRenderedThisFrame.addAll(this.keys)
            }
            get(InputStatesKey) {
                val unrenderedActiveInputStates =
                    this.values.filter { it.isActive && !idsRenderedThisFrame.contains(it.id) }
                if (unrenderedActiveInputStates.isNotEmpty()) {
                    unrenderedActiveInputStates.forEach { deactivate(it) }
                }
            }

            if (idsRenderedThisFrame.isEmpty()) {
                // A minor touch, but always make sure the cursor starts from scratch anytime a new input method is
                // called in the future
                cursorState.resetCursor()
            }
        }

        section.onFinishing {
            // If we are exiting the block but by chance the blinking cursor was on, turn it off!
            if (cursorState.blinkOn) {
                cursorState.resetCursor()
                cursorState.blinkOn = false
                section.requestRerender()
            }
        }

        cursorState
    }!!

    putIfAbsent(InputStatesKey, provideInitialValue = { mutableMapOf() }) {
        val inputStates = this
        val state = inputStates[id]?.also { multilineConfig?.update(it.multilineState!!) } ?: InputState(
            id,
            cursorState,
            multilineConfig
        ).apply {
            text = initialText
            cursorIndex = initialText.length
            multilineState?.updateIdealLineIndex()
        }.also { inputStates[id] = it }

        putIfAbsent(InputStatesCalledThisRender, provideInitialValue = { mutableMapOf() }) {
            val renderedInputStates = this
            if (renderedInputStates.contains(id)) {
                throw IllegalStateException("Got more than one `input` in a single render pass with ID $id")
            }
            if (isActive && renderedInputStates.values.any { it.isActive }) {
                throw IllegalStateException("Having more than one active `input` in a single render pass is not supported")
            }
            renderedInputStates[id] = state
        }

        if (state.isActive != isActive) {
            if (isActive) {
                activate(state)
            } else {
                deactivate(state)
            }
        }
    }

    run {
        tryPut(UpdateInputJobReadyKey) { CompletableDeferred<Unit>().also { get(RunDelayersKey) { add(it) } } }
        tryPut(
            UpdateInputJobKey,
            provideInitialValue = {
                section.coroutineScope.launch {
                    getValue(KeyFlowKey)
                        .onSubscription {
                            // Wait for the key flow to be hooked up before signaling to listeners that we are also
                            // ready (since we'll miss keys that are sent before the "read key" job is also up).
                            getValue(ReadKeyJobReadyKey).await()
                            getValue(UpdateInputJobReadyKey).complete(Unit)
                        }
                        .collect { key ->
                            withActiveInput {
                                val prevText = text
                                val prevCursorIndex = cursorIndex
                                var proposedText: String? = null
                                var proposedCursorIndex: Int? = null

                                fun moveCursorUp() {
                                    if (multilineState == null) return
                                    val prevLineEndCursorIndex = text.getLineStartCursorIndex(cursorIndex) - 1
                                    if (prevLineEndCursorIndex >= 0) {
                                        val prevLineEndIndex = text.getLineIndex(prevLineEndCursorIndex)
                                        val diffToIdealLineIndex =
                                            (prevLineEndIndex - multilineState.idealLineIndex).coerceAtLeast(0)
                                        cursorIndex = prevLineEndCursorIndex - diffToIdealLineIndex
                                    }
                                }

                                fun moveCursorDown() {
                                    if (multilineState == null) return
                                    val nextLineStartCursorIndex = text.getLineEndCursorIndex(cursorIndex) + 1
                                    if (nextLineStartCursorIndex <= text.length) {
                                        val nextLineEndCursorIndex =
                                            text.getLineEndCursorIndex(nextLineStartCursorIndex)
                                        val nextLineLength = nextLineEndCursorIndex - nextLineStartCursorIndex
                                        cursorIndex =
                                            nextLineStartCursorIndex + min(
                                                multilineState.idealLineIndex,
                                                nextLineLength
                                            )
                                    }
                                }

                                when (key) {
                                    Keys.LEFT -> {
                                        cursorIndex = (cursorIndex - 1).coerceAtLeast(0)
                                        multilineState?.updateIdealLineIndex()
                                    }

                                    Keys.RIGHT -> {
                                        if (cursorIndex < text.length) {
                                            cursorIndex++
                                            multilineState?.updateIdealLineIndex()
                                        } else {
                                            get(CompleterKey) {
                                                complete(text)?.let { completion ->
                                                    val finalText = text + completion
                                                    proposedText = finalText
                                                    proposedCursorIndex = finalText.length
                                                }
                                            }
                                        }
                                    }

                                    Keys.UP -> {
                                        moveCursorUp()
                                    }

                                    Keys.DOWN -> {
                                        moveCursorDown()
                                    }

                                    Keys.HOME -> {
                                        cursorIndex = text.getLineStartCursorIndex(cursorIndex)
                                        multilineState?.updateIdealLineIndex()
                                    }

                                    Keys.END -> {
                                        cursorIndex = text.getLineEndCursorIndex(cursorIndex)
                                        multilineState?.updateIdealLineIndex()
                                    }

                                    Keys.PAGE_UP -> {
                                        if (multilineState == null) return@withActiveInput
                                        repeat(multilineState.pageSize) { moveCursorUp() }
                                    }

                                    Keys.PAGE_DOWN -> {
                                        if (multilineState == null) return@withActiveInput
                                        repeat(multilineState.pageSize) { moveCursorDown() }
                                    }

                                    Keys.DELETE -> {
                                        if (cursorIndex <= text.lastIndex) {
                                            proposedText = text.removeRange(cursorIndex, cursorIndex + 1)
                                            proposedCursorIndex = cursorIndex
                                        }
                                    }

                                    Keys.BACKSPACE -> {
                                        if (cursorIndex > 0) {
                                            proposedText = text.removeRange(cursorIndex - 1, cursorIndex)
                                            proposedCursorIndex = cursorIndex - 1
                                        }
                                    }

                                    Keys.ENTER, Keys.EOF -> {
                                        if ((multilineState == null && key == Keys.ENTER) || (multilineState != null && key == Keys.EOF)) {
                                            var rejected = false
                                            var cleared = false
                                            get(InputEnteredCallbackKey) {
                                                val onInputEnteredScope = OnInputEnteredScope(id, text)
                                                this.invoke(onInputEnteredScope)
                                                rejected = onInputEnteredScope.rejected
                                                cleared = onInputEnteredScope.cleared
                                            }
                                            if (cleared) {
                                                getValue(InputStatesKey).remove(id)
                                            }
                                            if (!rejected) {
                                                get(SystemInputEnteredCallbackKey) { this.invoke() }
                                            }
                                        } else if (multilineState != null) {
                                            check(key == Keys.ENTER)

                                            proposedText = text.insertAtCursorIndex(cursorIndex, '\n')
                                            proposedCursorIndex = cursorIndex + 1
                                        }
                                    }

                                    else ->
                                        if (key is CharKey) {
                                            proposedText = text.insertAtCursorIndex(cursorIndex, key.code)
                                            proposedCursorIndex = cursorIndex + 1
                                        }
                                }

                                if (proposedText != null) {
                                    get(InputChangedCallbackKey) {
                                        val onInputChangedScope =
                                            OnInputChangedScope(id, input = proposedText!!, prevInput = text)
                                        this.invoke(onInputChangedScope)

                                        if (!onInputChangedScope.rejected) {
                                            proposedText = onInputChangedScope.input
                                        } else {
                                            proposedText = onInputChangedScope.prevInput
                                            proposedCursorIndex = cursorIndex
                                        }
                                    }

                                    text = proposedText!!
                                    cursorIndex = (proposedCursorIndex ?: cursorIndex).coerceIn(0, text.length)
                                    // If the user typed something that shifted the text, we should update the current
                                    // line index as well
                                    if (text != prevText) multilineState?.updateIdealLineIndex()
                                }

                                if (text != prevText || cursorIndex != prevCursorIndex) {
                                    section.requestRerender()
                                }
                            }
                        }
                }
            },
        )
    }
}

/**
 * Fetch the current value of some [input] call from anywhere within a [RunScope.run] block, if one is set.
 *
 * You should ideally only check input values within [onInputChanged], [onInputEntered] etc. callbacks, but for edge
 * cases it may be useful to fetch input outside of those events.
 *
 * See also: [input], [setInput]
 *
 * @param id If set, find the input with the matching ID. This can be useful if you have multiple input blocks defined
 *   at the same time.
 */
fun SectionScope.getInput(id: Any = Unit): String? {
    var input: String? = null
    data.get(InputStatesKey) { input = this[id]?.text }
    return input
}

/**
 * Set the value for some [input] call directly from anywhere in a [RunScope.run] block.
 *
 * This should be extremely rare to do! But perhaps you need to set the text asynchronously
 * (e.g. [onInputEntered] is blocking) or inside on [onKeyPressed] callback, etc.
 *
 * However, try using [onInputChanged], [onInputEntered], etc. first. This will result in code that is easier for
 * readers to follow.
 *
 * See also: [input], [getInput]
 *
 * @param text The text to replace the current input with
 * @param cursorIndex If specified, the index of the cursor position; otherwise, it will be placed after the end of the
 *   text.
 * @param id If set, find the input with the matching ID. This can be useful if you have multiple input blocks defined
 *   at the same time.
 */
fun RunScope.setInput(text: String, cursorIndex: Int = text.length, id: Any = Unit) {
    data.get(InputStatesKey) {
        this[id]?.apply {
            @Suppress("NAME_SHADOWING") var text = text
            if (this.multilineState == null) text = text.replace("\n", "")

            if (this.text != text || this.cursorIndex != cursorIndex) {
                this.text = text
                this.cursorIndex = cursorIndex

                rerender()
            }
        }
    }
}

/**
 * Programmatically trigger an "input entered" event.
 *
 * For example, maybe you want to auto-trigger an event after some time runs out.
 *
 * Note that this request can still be rejected by the [onInputEntered] callback.
 *
 * This call works for both [input] and [multilineInput] calls. Conversely, if no input is currently active, calling
 * this will do nothing.
 */
fun RunScope.enterInput() {
    val runScope = this
    data.withActiveInput {
        if (this.multilineState == null) {
            runScope.sendKeys(Keys.ENTER)
        } else {
            runScope.sendKeys(Keys.EOF)
        }
    }
}

/**
 * Interface for a class that can provide suggested auto-completions for an [input] call, given some initial text.
 */
interface InputCompleter {
    /**
     * Given some [input], return a suffix that should complete it, or null if the string does not have a matching
     * completion.
     *
     * For example, for `"y"`, you might return `"es"`.
     */
    fun complete(input: String): String?

    val color: Color get() = Color.BRIGHT_BLACK
}

/**
 * A default [InputCompleter] that provides completions given a list of possible values.
 *
 * For example:
 *
 * ```
 * input(Completions("yes", "no"))
 * ```
 *
 * will suggest `"yes"` for `""`, `"y"`, and `"ye"`, while suggesting `"no"` for `"n"`.
 *
 * If there are multiple matches, e.g. `"Colorado"` and `"Connecticut"` for `"co"`, the item earlier in the list will be
 * suggested as the completion.
 */
open class Completions(private vararg val values: String, private val ignoreCase: Boolean = true) : InputCompleter {
    override fun complete(input: String): String? {
        return values.firstOrNull { value ->
            value.startsWith(input, ignoreCase)
        }?.substring(input.length)
    }
}

private val CompleterKey = Section.Lifecycle.createKey<InputCompleter>()

/**
 * Information passed into the `viewMap` callback in the [input][com.varabyte.kotter.foundation.input.input] method.
 *
 * The user can check the current character being transformed (via the [ch] property), but the entire [input] so far and
 * the character's [index] into it are also provided in case the context helps with the mapping. It's expected that none
 * of these values will be needed in many cases, e.g. masking a password, but you can refer to them if you need to!
 *
 * For an example, to visually render all input text as uppercase (while the underlying input will be whatever case the
 * user typed in), you could call:
 *
 * ```
 * input(viewMap = { ch.uppercaseChar() })
 * ```
 *
 * or for masking a password, simply:
 *
 * ```
 * input(viewMap = { '*' })
 * ```
 *
 * @property input The backing text associated with the [input][com.varabyte.kotter.foundation.input.input] call.
 * @property index The index of the current character in the string being mapped.
 */
class ViewMapScope(val input: String, val index: Int) {
    /**
     * The current source character being mapped.
     *
     * This is a convenience property identical to `input[index]`.
     */
    val ch: Char = input[index]
}

/**
 * Information passed into the `customFormat` callback in the [input][com.varabyte.kotter.foundation.input.input] method.
 *
 * The callback will be triggered once per character in the `input` string when it's about to get rendered (excluding
 * any autocomplete suggestion text). The user can then call any of the formatting methods which will be applied to THE
 * CURRENT CHARACTER ONLY.
 *
 * For an example, to visually highlight all non-digits as invalid (or, red):
 *
 * ```
 * input(customFormat = { if (ch.isDigit()) green() else red() })
 * ```
 *
 * The following format will apply an underline to all characters:
 *
 * ```
 * input(customFormat = { underline() })
 * ```
 *
 * @property input The backing text associated with the [input][com.varabyte.kotter.foundation.input.input] call.
 * @property index The index of the current character in the string being mapped.
 * @property isActive Whether the input is currently active. This is useful if you want to render the input differently
 *   when the input has active focus or not.
 */
class CustomFormatScope(val input: String, val index: Int, val isActive: Boolean) {
    internal var fgColor: Color? = null
        private set
    internal var bgColor: Color? = null
        private set
    internal var isBold: Boolean = false
        private set
    internal var isUnderline: Boolean = false
        private set
    internal var isStrikethrough: Boolean = false
        private set

    /**
     * The current source character being mapped.
     *
     * This is a convenience property identical to `input[index]`.
     */
    val ch: Char = input[index]

    internal val changed get() = fgColor != null || bgColor != null || isBold || isUnderline || isStrikethrough

    fun color(color: Color, layer: ColorLayer = ColorLayer.FG) {
        when (layer) {
            ColorLayer.FG -> fgColor = color
            ColorLayer.BG -> bgColor = color
        }
    }

    fun bold() {
        isBold = true
    }

    fun underline() {
        isUnderline = true
    }

    fun strikethrough() {
        isStrikethrough = true
    }
}

fun CustomFormatScope.black(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    color(if (isBright) Color.BRIGHT_BLACK else Color.BLACK, layer)
}

fun CustomFormatScope.red(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    color(if (isBright) Color.BRIGHT_RED else Color.RED, layer)
}

fun CustomFormatScope.green(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    color(if (isBright) Color.BRIGHT_GREEN else Color.GREEN, layer)
}

fun CustomFormatScope.yellow(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    color(if (isBright) Color.BRIGHT_YELLOW else Color.YELLOW, layer)
}

fun CustomFormatScope.blue(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    color(if (isBright) Color.BRIGHT_BLUE else Color.BLUE, layer)
}

fun CustomFormatScope.magenta(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    color(if (isBright) Color.BRIGHT_MAGENTA else Color.MAGENTA, layer)
}

fun CustomFormatScope.cyan(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    color(if (isBright) Color.BRIGHT_CYAN else Color.CYAN, layer)
}

fun CustomFormatScope.white(layer: ColorLayer = ColorLayer.FG, isBright: Boolean = false) {
    color(if (isBright) Color.BRIGHT_WHITE else Color.WHITE, layer)
}

private class MultilineConfig(
    val pageSize: Int,
) {
    fun update(targetState: MultilineState) {
        targetState.pageSize = pageSize
    }
}

private fun MainRenderScope.handleInput(
    completer: InputCompleter?,
    initialText: String,
    id: Any,
    viewMap: (ViewMapScope.() -> Char)?,
    customFormat: (CustomFormatScope.() -> Unit)?,
    isActive: Boolean,
    multilineConfig: MultilineConfig?,
) {
    data.prepareInput(this, id, initialText, isActive, multilineConfig)
    completer?.let { data[CompleterKey] = it }

    with(data.getValue(InputStatesKey)[id]!!) {
        val transformedText = if (viewMap != null) {
            text.mapIndexed { i, _ -> ViewMapScope(text, i).viewMap() }.joinToString("")
        } else text

        // Just asserting that for now this is always a 1:1 transformation. We may change this later but if we do, be
        // careful that cursor keys still work as expected.
        check(text.length == transformedText.length)

        // First, check the hard but common case. If we're the currently active input, render it with current
        // completions and cursor
        if (this.isActive) {
            val completion = try {
                completer?.complete(text)
            } catch (ex: Exception) {
                null
            } ?: ""


            // Note: Trailing space as cursor can be put AFTER last character
            val finalText = "$transformedText$completion "

            scopedState { // Make sure color changes don't leak
                for (i in finalText.indices) {
                    var customFormatApplied = false
                    if (customFormat != null && i <= text.lastIndex) {
                        val format = CustomFormatScope(text, i, isActive = true).apply(customFormat)
                        if (format.changed) {
                            customFormatApplied = true
                            pushState()
                            if (format.fgColor != null) color(format.fgColor!!)
                            if (format.bgColor != null) color(format.bgColor!!, layer = ColorLayer.BG)
                            if (format.isBold) bold()
                            if (format.isUnderline) underline()
                            if (format.isStrikethrough) strikethrough()
                        }
                    }
                    if (i == text.length && completer != null && completion.isNotEmpty()) {
                        color(completer.color)
                    }
                    if (i == cursorIndex && cursorState.blinkOn) {
                        invert()
                    }
                    // Put in a placeholder space for every newline, which allows a cursor to appear in that place
                    if (finalText[i] == '\n') text(' ')
                    text(finalText[i])
                    if (i == cursorIndex && cursorState.blinkOn) {
                        clearInvert()
                    }
                    if (customFormatApplied) {
                        popState()
                    }
                }
            }
        }
        // Otherwise, this input is dormant, and acts like normal text
        else {
            if (customFormat == null) {
                text(transformedText)
            } else {
                transformedText.forEachIndexed { i, c ->
                    val format = CustomFormatScope(text, i, isActive = false).apply(customFormat)
                    if (format.changed) {
                        pushState()
                        if (format.fgColor != null) color(format.fgColor!!)
                        if (format.bgColor != null) color(format.bgColor!!, layer = ColorLayer.BG)
                        if (format.isBold) bold()
                        if (format.isUnderline) underline()
                        if (format.isStrikethrough) strikethrough()
                    }
                    text(c)
                    if (format.changed) {
                        popState()
                    }
                }
            }
        }

        // Multiline inputs always render in their own area and therefore end with an extra newline
        // A preceding newline is added when `multilineInput` is called.
        if (multilineState != null) textLine()
    }
}


/**
 * A function which, when called, will replace itself dynamically with text typed by the user, plus a blinking cursor.
 *
 * You can use the `onInputChanged` and `onInputEntered` callbacks to query the value as the user types it / commits it:
 *
 * ```
 * var name = ""
 * section {
 *   text("Enter your name: "); input()
 * }.runUntilInputEntered {
 *   onInputEntered {
 *     name = input // here, "input" is what the user typed in
 *   }
 * }
 * ```
 *
 * Usually you'll only need to call `input()` once in a whole section, but occasionally you may use more than one.
 *
 * There are two main cases:
 *
 * **1 - Only one input is shown at a time, but the inputs are different.**
 *
 * ```
 * when (state) {
 *   ASK_NAME -> text("Your name? "); input(id = "name")
 *   ASK_AGE -> text("Your age? "); input(id = "age")
 *   ...
 * }
 * ...
 * onKeyPressed {
 *   Keys.TAB -> state = state.next()
 * }
 * ```
 *
 * In this case, you should ensure that each input has a unique ID, so that Kotter realizes that a new input has gotten
 * focus, and can show its last known value.
 *
 * **2 - Multiple inputs are shown at the same time.**
 * ```
 * text("Red value:   "); input(id = "red", isActive = (state == EDIT_RED))
 * text("Green value: "); input(id = "green", isActive = (state == EDIT_GREEN))
 * text("Blue value:  "); input(id = "blue", isActive = (state == EDIT_BLUE))
 * ...
 * onKeyPressed {
 *   Keys.TAB -> state = state.next()
 * }
 * ```
 *
 * In addition to using unique IDs per input, you should make sure your logic works so that at most only one of them is
 * active at a time. If you call `input()` twice in a single section with both being active, it'll throw a runtime
 * exception.
 *
 * @param completer Optional logic for suggesting auto-completions based on what the user typed in. See
 *   [Completions] which is a generally useful and common implementation.
 * @param initialText Text which will be used the first time `input()` is called and ignored subsequently. Note that
 *   newlines are now allowed in single-line inputs, and any newlines in this value will be removed.
 * @param id See docs above for more details. The value of this parameter can be anything - this method simply does an
 *   equality check on it against a previous value.
 * @param viewMap If set, *visually* transform the text by specifying the target character each letter in the text
 *   should map to. This doesn't affect the input's actual value, just the value that is rendered on screen. This is
 *   particularly useful for password inputs, which would look like `viewMap = { '*' }`.
 * @param customFormat If set, allow making custom formatting changes to the input text as it's being rendered.
 *   See [CustomFormatScope] for more details.
 * @param isActive See docs above for more details. If multiple calls to input are made in a single section, at most one
 *   of them can be active at a time.
 */
fun MainRenderScope.input(
    completer: InputCompleter? = null,
    initialText: String = "",
    id: Any = Unit,
    viewMap: (ViewMapScope.() -> Char)? = null,
    customFormat: (CustomFormatScope.() -> Unit)? = null,
    isActive: Boolean = true
) {
    handleInput(completer, initialText.replace("\n", ""), id, viewMap, customFormat, isActive, multilineConfig = null)
}

/**
 * Like [input], but allows the user to type multiple lines of text.
 *
 * To end a multiline input string, the user must press Ctrl+D, as pressing Enter just appends a new line.
 *
 * @param pageSize How many lines of text to skip over when pressing Page Up / Page Down. If not specified, uses the
 *   value [defaultPageSize], which you can set instead to affect all inputs.
 */
fun MainRenderScope.multilineInput(
    initialText: String = "",
    id: Any = Unit,
    viewMap: (ViewMapScope.() -> Char)? = null,
    customFormat: (CustomFormatScope.() -> Unit)? = null,
    pageSize: Int? = null,
    isActive: Boolean = true
) {
    addNewlinesIfNecessary(1)
    handleInput(
        null,
        initialText,
        id,
        viewMap,
        customFormat,
        isActive,
        multilineConfig = MultilineConfig(pageSize ?: section.session.defaultPageSize)
    )
}

/**
 * Fields accessible within a callback triggered by [onKeyPressed].
 *
 * @property key The key that was pressed. See also: [Keys]
 */
class OnKeyPressedScope(val key: Key)

private val KeyPressedJobKey = RunScope.Lifecycle.createKey<Job>()
private val KeyPressedJobReadyKey = RunScope.Lifecycle.createKey<CompletableDeferred<Unit>>()
private val KeyPressedCallbackKey = RunScope.Lifecycle.createKey<OnKeyPressedScope.() -> Unit>()

// Note: We create a separate key here from above to ensure we can trigger the system callback only AFTER the user
// callback was triggered. That's because the system handler may fire a signal which, if sent out too early, could
// result in the user callback not getting a chance to run.
private val SystemKeyPressedCallbackKey = RunScope.Lifecycle.createKey<OnKeyPressedScope.() -> Unit>()

/**
 * Start running a job that collects keypresses and sends them to callbacks.
 *
 * This is a no-op when called after the first time.
 */
private fun ConcurrentScopedData.prepareOnKeyPressed(section: Section) {
    prepareKeyFlow(section)
    tryPut(KeyPressedJobReadyKey) { CompletableDeferred() }
    tryPut(
        KeyPressedJobKey,
        provideInitialValue = {
            section.coroutineScope.launch {
                getValue(KeyFlowKey)
                    .onSubscription { getValue(KeyPressedJobReadyKey).complete(Unit) }
                    .collect { key ->
                        val scope = OnKeyPressedScope(key)
                        get(KeyPressedCallbackKey) { this.invoke(scope) }
                        get(SystemKeyPressedCallbackKey) { this.invoke(scope) }
                    }
            }
        },
    )
}

/**
 * A handler you can register in a [RunScope.run] block to intercept keypresses.
 *
 * For example:
 *
 * ```
 * section { ... }.run {
 *   onKeyPressed {
 *     when (key) {
 *       Keys.SPACE -> ...
 *       Keys.ESC -> ...
 *     }
 *   }
 * }
 * ```
 */
fun RunScope.onKeyPressed(listener: OnKeyPressedScope.() -> Unit) {
    data.prepareOnKeyPressed(section)
    data.waitForInputReady()

    if (!data.tryPut(KeyPressedCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onKeyPressed` callback at a time is supported.")
    }
}

/**
 * A `run` block which runs until one of the specified keys is pressed by the user.
 *
 * As a minimal example:
 *
 * ```
 * section {
 *   textLine("Press Q to quit.")
 *   ...
 * }.runUntilKeyPressed(Keys.Q) {
 *   ...
 * }
 * ```
 */
fun Section.runUntilKeyPressed(vararg keys: Key, block: suspend RunScope.() -> Unit = {}) {
    run {
        session.data.prepareOnKeyPressed(this@runUntilKeyPressed)
        session.data.waitForInputReady()
        // We need to abort as even if the user puts a while(true) in their run block, we still want to exit
        data[SystemKeyPressedCallbackKey] = { if (keys.contains(key)) abort() }
        block()
        CompletableDeferred<Unit>().await() // The only way out of this function is by aborting
    }
}


/**
 * Fields accessible within a callback triggered by [onInputActivated].
 *
 * @property id The ID of the current input, if one was specified in the original call to [input][com.varabyte.kotter.foundation.input.input].
 * @property input The current text value of the input.
 */
class OnInputActivatedScope(val id: Any, var input: String)

private val InputActivatedCallbackKey = RunScope.Lifecycle.createKey<OnInputActivatedScope.() -> Unit>()

private fun ConcurrentScopedData.withActiveInput(block: InputState.() -> Unit) {
    get(InputStatesKey) {
        values.find { it.isActive }?.block()
    }
}

/**
 * A callback you can register in a [RunScope.run] block that will get triggered any time an [input] gains focus.
 *
 * For example:
 *
 * ```
 * section {
 *   when (state) {
 *     ASK_NAME -> text("Your name? "); input(id = "name")
 *     ASK_AGE -> text("Your age? "); input(id = "age")
 *     ...
 *   }
 * }.run {
 *   onInputActivated {
 *     when (id) {
 *       "name" -> ...
 *       "age" - > ...
 *     }
 *   }
 *   onKeyPressed {
 *     Keys.TAB -> state = state.next()
 *   }
 * }
 * ```
 */
fun RunScope.onInputActivated(listener: OnInputActivatedScope.() -> Unit) {
    data.waitForInputReady()
    if (!data.tryPut(InputActivatedCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onInputActivated` callback at a time is supported.")
    } else {
        // There may already be an active input when this callback was registered.
        data.withActiveInput {
            val onInputActivatedScope = OnInputActivatedScope(id, text)
            listener(onInputActivatedScope)
            text = onInputActivatedScope.input
        }
    }
}

/**
 * Fields accessible within a callback triggered by [onInputDeactivated].
 *
 * @property id The ID of the current input, if one was specified in the original call to [input][com.varabyte.kotter.foundation.input.input].
 * @property input The current text value of the input.
 */
class OnInputDeactivatedScope(val id: Any, var input: String)

private val InputDeactivatedCallbackKey = RunScope.Lifecycle.createKey<OnInputDeactivatedScope.() -> Unit>()

/**
 * A callback you can register in a [RunScope.run] block that will get triggered any time an [input] loses focus.
 *
 * For example:
 *
 * ```
 * section {
 *   when (state) {
 *     ASK_NAME -> text("Your name? "); input(id = "name")
 *     ASK_AGE -> text("Your age? "); input(id = "age")
 *     ...
 *   }
 * }.run {
 *   onInputDeactivated {
 *     when (id) {
 *       "name" -> ...
 *       "age" - > ...
 *     }
 *   }
 *   onKeyPressed {
 *     Keys.TAB -> state = state.next()
 *   }
 * }
 * ```
 */
fun RunScope.onInputDeactivated(listener: OnInputDeactivatedScope.() -> Unit) {
    data.waitForInputReady()

    if (!data.tryPut(
            InputDeactivatedCallbackKey,
            provideInitialValue = { listener },
            dispose = {
                val states = data[InputStatesKey] ?: return@tryPut
                val activeState = states.values.firstOrNull { it.isActive } ?: return@tryPut
                data.deactivate(activeState)
            }
        )
    ) {
        throw IllegalStateException("Currently only one `onInputDeactivated` callback at a time is supported.")
    }
}

/**
 * Fields and methods accessible within a callback triggered by [onInputChanged].
 *
 * @property id The ID of the current input, if one was specified in the original call to [input][com.varabyte.kotter.foundation.input.input].
 * @property input The text value of the input entered by the user. This value can be modified, which will affect the
 *   final input rendered.
 * @property prevInput The previous (last good) state of the input.
 */
class OnInputChangedScope(val id: Any, var input: String, val prevInput: String) {
    internal var rejected = false

    /** Indicate that the current [input] change isn't valid and the last state should be restored. */
    fun rejectInput() {
        rejected = true
    }
}

private val InputChangedCallbackKey = RunScope.Lifecycle.createKey<OnInputChangedScope.() -> Unit>()

/**
 * A callback you can register in a [RunScope.run] block that will get triggered any time the changes their [input].
 *
 * The user's input will be provided via the [OnInputChangedScope.input] property. This value can be intercepted and
 * edited at this point.
 *
 * You can also call [OnInputChangedScope.rejectInput] to indicate that the last change should be rejected.
 *
 * For example:
 *
 * ```
 * section {
 *   text("First name: "); input()
 * }.run {
 *   onInputChanged {
 *     if (input.any { !it.isLetter }) rejectInput()
 *   }
 * }
 * ```
 */
fun RunScope.onInputChanged(listener: OnInputChangedScope.() -> Unit) {
    data.waitForInputReady()
    if (!data.tryPut(InputChangedCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onInputChanged` callback at a time is supported.")
    }
}

/**
 * Fields and methods accessible within a callback triggered by [onInputEntered].
 *
 * @property id The ID of the current input, if one was specified in the original call to [input][com.varabyte.kotter.foundation.input.input].
 * @property input The text value of the input entered by the user. At this point, the value is readonly. See also:
 *   [onInputChanged], a callback that lets you modify this value as it is being typed.
 */
class OnInputEnteredScope(val id: Any, val input: String) {
    internal var rejected = false

    /** Indicate that the current [input] isn't valid and shouldn't be accepted as is. */
    fun rejectInput() {
        rejected = true
    }

    internal var cleared = false

    /**
     * Call to reset the input back to blank, which can be useful if you are re-using the same [input] multiple times.
     */
    fun clearInput() {
        cleared = true
    }
}

private val InputEnteredCallbackKey = RunScope.Lifecycle.createKey<OnInputEnteredScope.() -> Unit>()

// Note: We create a separate key here from above to ensure we can trigger the system callback only AFTER the user
// callback was triggered. That's because the system handler may fire a signal which, if sent out too early, could
// result in the user callback not getting a chance to run.
private object SystemInputEnteredCallbackKey : ConcurrentScopedData.Key<() -> Unit> {
    override val lifecycle = RunScope.Lifecycle
}

/**
 * A callback you can register in a [RunScope.run] block which will get triggered any time the user presses the ENTER
 * key an [input] area that has focus.
 *
 * The user's input will be provided via the [OnInputEnteredScope.input] property. This is a good time to update any
 * local variables you have that depend on the user's input, and possibly end the current section.
 *
 * Here's a common pattern, combined with [runUntilInputEntered] to handle exiting the block when the input has been
 * accepted:
 *
 * ```
 * var name = ""
 * section {
 *   text("Name: "); input()
 * }.runUntilInputEntered {
 *   onInputEntered {
 *     name = input
 *   }
 * }
 * ```
 *
 * You can call [OnInputEnteredScope.rejectInput] to indicate that the input still needs to change before it can be
 * accepted.
 */
fun RunScope.onInputEntered(listener: OnInputEnteredScope.() -> Unit) {
    data.waitForInputReady()
    if (!data.tryPut(InputEnteredCallbackKey) { listener }) {
        throw IllegalStateException("Currently only one `onInputEntered` callback at a time is supported.")
    }
}

/**
 * A `run` block which runs until the user has pressed ENTER on some currently active [input].
 *
 * As a minimal example:
 *
 * ```
 * var name: String = ""
 * section {
 *   text("Name: "); input()
 * }.runUntilInputEntered {
 *   onInputEntered { name = input }
 * }
 * ```
 */
fun Section.runUntilInputEntered(block: suspend RunScope.() -> Unit = {}) {
    run {
        session.data.waitForInputReady()
        // We need to abort as even if the user puts a while(true) in their run block, we still want to exit
        data[SystemInputEnteredCallbackKey] = { abort() }
        block()
        CompletableDeferred<Unit>().await() // The only way out of this function is by aborting
    }
}
