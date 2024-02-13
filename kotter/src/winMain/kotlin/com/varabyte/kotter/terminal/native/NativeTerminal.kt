package com.varabyte.kotter.terminal.native

import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotter.runtime.terminal.*
import kotlinx.cinterop.Arena
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import platform.posix.printf
import platform.windows.CONSOLE_SCREEN_BUFFER_INFO
import platform.windows.DWORDVar
import platform.windows.ENABLE_PROCESSED_INPUT
import platform.windows.ENABLE_PROCESSED_OUTPUT
import platform.windows.ENABLE_VIRTUAL_TERMINAL_INPUT
import platform.windows.ENABLE_VIRTUAL_TERMINAL_PROCESSING
import platform.windows.GetConsoleMode
import platform.windows.GetConsoleScreenBufferInfo
import platform.windows.GetStdHandle
import platform.windows.INPUT_RECORD
import platform.windows.KEY_EVENT
import platform.windows.KEY_EVENT_RECORD
import platform.windows.LEFT_ALT_PRESSED
import platform.windows.LEFT_CTRL_PRESSED
import platform.windows.RIGHT_ALT_PRESSED
import platform.windows.RIGHT_CTRL_PRESSED
import platform.windows.ReadConsoleInput
import platform.windows.SHIFT_PRESSED
import platform.windows.STD_INPUT_HANDLE
import platform.windows.STD_OUTPUT_HANDLE
import platform.windows.SetConsoleMode
import platform.windows.TRUE

actual class NativeTerminal : Terminal {
    private val stdInHandle = GetStdHandle(STD_INPUT_HANDLE)!!
    private val stdOutHandle = GetStdHandle(STD_OUTPUT_HANDLE)!!

    private val arena = Arena()
    private val origModeInVar = arena.alloc<DWORDVar>()
    private val origModeOutVar = arena.alloc<DWORDVar>()

    init {
        if (
            GetConsoleMode(stdInHandle, origModeInVar.ptr) != TRUE ||
            GetConsoleMode(stdOutHandle, origModeOutVar.ptr) != TRUE
        ) {
            arena.clear()
            throw CreateNativeTerminalException()
        }

        // Disable all console features, meaning we're indicating we'll handle processing all input ourselves
        // See also: https://learn.microsoft.com/en-us/windows/console/high-level-console-modes
        // And also: https://learn.microsoft.com/en-us/windows/console/console-virtual-terminal-sequences#example-of-enabling-virtual-terminal-processing

        if (SetConsoleMode(stdInHandle, ENABLE_VIRTUAL_TERMINAL_INPUT.or(ENABLE_PROCESSED_INPUT).convert()) != TRUE) {
            arena.clear()
            throw CreateNativeTerminalException()
        }

        if (SetConsoleMode(
                stdOutHandle,
                ENABLE_VIRTUAL_TERMINAL_PROCESSING.or(ENABLE_PROCESSED_OUTPUT).convert()
            ) != TRUE
        ) {
            SetConsoleMode(stdInHandle, origModeInVar.value)
            arena.clear()
            throw CreateNativeTerminalException()
        }

        printf("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}?25l") // hide the cursor
    }

    override val width: Int
        get() = memScoped {
            val csbi = alloc<CONSOLE_SCREEN_BUFFER_INFO>()
            GetConsoleScreenBufferInfo(stdOutHandle, csbi.ptr)
            (csbi.srWindow.Right - csbi.srWindow.Left)
        }

    override val height: Int
        get() = memScoped {
            val csbi = alloc<CONSOLE_SCREEN_BUFFER_INFO>()
            GetConsoleScreenBufferInfo(stdOutHandle, csbi.ptr)
            (csbi.srWindow.Bottom - csbi.srWindow.Top)
        }

    private var closed = false

    override fun write(text: String) {
        print(text)
    }

    // Thanks to JLine3: https://github.com/jline/jline3/blob/2c55e39b0380a1b6ce4696bb6068c0091568d336/terminal/src/main/java/org/jline/terminal/impl/AbstractWindowsTerminal.java#L278
    private suspend fun FlowCollector<Int>.handleControlChars(
        keyCode: Int,
        isCtrl: Boolean,
        isAlt: Boolean,
        isShift: Boolean
    ): Boolean {
        // TODO: Revisit adding support for control chars + meta keys if requested; should be OK to not worry about this
        //  for a first pass. Note: system commands (like Ctrl+C / Ctrl+D) are handled by Windows before we get here.
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
                    if (closed) {
                        // terminal was just closed between this read and last read
                        quit = true
                    } else {
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
    }

    override fun read(): Flow<Int> = charFlow

    override fun clear() {
        printf("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}2J") // Clear console
        printf("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}H") // Move cursor to top-left
    }

    override fun close() {
        printf("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}?25h") // restore the cursor
        SetConsoleMode(stdInHandle, origModeInVar.value)
        SetConsoleMode(stdOutHandle, origModeOutVar.value)
        arena.clear()

        closed = true
    }
}
