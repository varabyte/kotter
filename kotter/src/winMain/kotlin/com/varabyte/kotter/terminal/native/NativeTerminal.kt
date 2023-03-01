package com.varabyte.kotter.terminal.native

import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.terminal.Terminal
import kotlinx.cinterop.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import platform.posix.puts
import platform.windows.*

actual class NativeTerminal : Terminal {
    private val stdInHandle = GetStdHandle(STD_INPUT_HANDLE)!!
    private val origModeVar = nativeHeap.alloc<DWORDVar>().apply {
        // Disable all console features, meaning we're indicating we'll handle processing all input ourselves
        // See also: https://learn.microsoft.com/en-us/windows/console/high-level-console-modes
        SetConsoleMode(
            stdInHandle,
            ENABLE_VIRTUAL_TERMINAL_PROCESSING // Parse ANSI control characters
                .or(ENABLE_PROCESSED_INPUT)  // The system will handle CTRL-C
                .convert()
        )
    }

    init {
        puts("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}?25l") // hide the cursor
    }

    override fun write(text: String) {
        print(text)
    }

    // Thanks to JLine3: https://github.com/jline/jline3/blob/2c55e39b0380a1b6ce4696bb6068c0091568d336/terminal/src/main/java/org/jline/terminal/impl/AbstractWindowsTerminal.java#L278
    private suspend fun FlowCollector<Int>.handleControlChars(keyCode: Int, isCtrl: Boolean, isAlt: Boolean, isShift: Boolean): Boolean {
        // TODO: Revisit adding support for control chars + meta keys if requested; should be OK to not worry about this
        //  for a first pass.
        if (isCtrl || isAlt || isShift) return false

        suspend fun emitStr(str: String) {
            str.forEach { c -> emit(c.code) }
        }
        suspend fun emitCode(csiCode: Ansi.Csi.Code) {
            emitStr(csiCode.toFullEscapeCode())
        }

        when (keyCode) {
            0x21 -> emitCode(Ansi.Csi.Codes.Keys.PG_UP)
            0x22 -> emitCode(Ansi.Csi.Codes.Keys.PG_DOWN)
            0x23 -> emitCode(Ansi.Csi.Codes.Keys.END)
            0x24 -> emitCode(Ansi.Csi.Codes.Keys.HOME)
            0x25 -> emitCode(Ansi.Csi.Codes.Keys.LEFT)
            0x26 -> emitCode(Ansi.Csi.Codes.Keys.UP)
            0x27 -> emitCode(Ansi.Csi.Codes.Keys.RIGHT)
            0x28 -> emitCode(Ansi.Csi.Codes.Keys.DOWN)
            0x2D -> emitCode(Ansi.Csi.Codes.Keys.INSERT)
            0x2E -> emitCode(Ansi.Csi.Codes.Keys.DELETE)

            else -> return false
        }
        return true
    }

    // Thanks to Jline3: https://github.com/jline/jline3/blob/2c55e39b0380a1b6ce4696bb6068c0091568d336/terminal/src/main/java/org/jline/terminal/impl/AbstractWindowsTerminal.java#L215
    private suspend fun FlowCollector<Int>.processKeyEvent(keyEvent: KEY_EVENT_RECORD) {
        val isKeyDown = keyEvent.bKeyDown == TRUE
        val virtualKeyCode = keyEvent.wVirtualKeyCode.convert<Int>()
        val c = keyEvent.uChar.UnicodeChar
        val controlKeyState = keyEvent.dwControlKeyState.toInt()

        val isCtrl = (controlKeyState.and((LEFT_CTRL_PRESSED.or(RIGHT_CTRL_PRESSED))) != 0)
        val isAlt = (controlKeyState.and((LEFT_ALT_PRESSED.or(RIGHT_ALT_PRESSED))) != 0)
        val isShift = (controlKeyState.and(SHIFT_PRESSED) != 0)

        if (isKeyDown) {
            if (!handleControlChars(virtualKeyCode, isCtrl, isAlt, isShift)) {
                c.toInt().takeIf { it > 0 }?.let { emit(it) }
            }
        }
    }

    private val charFlow: Flow<Int> by lazy {
        flow {
            var quit = false
            val context = currentCoroutineContext()
            memScoped {
                val inputRecord = alloc<INPUT_RECORD>()
                val numEventsRead = alloc<DWORDVar>()
                while (!quit && context.isActive) {
                    val readResult = ReadConsoleInput!!(stdInHandle, inputRecord.ptr, 1.convert(), numEventsRead.ptr)
                    if (readResult != 0) {
                        for (i in 0 until numEventsRead.value.toInt()) {
                            if (inputRecord.EventType.toInt() == KEY_EVENT) {
                                processKeyEvent(inputRecord.Event.KeyEvent)
                            }
                        }
                    } else {
                        quit = true
                    }
                }
            }
        }
    }

    override fun read(): Flow<Int> = charFlow

    override fun clear() {
        puts("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}2J") // Clear console
        puts("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}H") // Move cursor to top-left
    }

    override fun close() {
        puts("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}?25h") // restore the cursor
        SetConsoleMode(stdInHandle, origModeVar.value)
        nativeHeap.free(origModeVar)
    }
}
