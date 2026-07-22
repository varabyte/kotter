package com.varabyte.kotter.terminal.native

import com.varabyte.kotter.runtime.coroutines.KotterDispatchers
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.terminal.Terminal
import com.varabyte.kotter.runtime.terminal.TerminalSize
import com.varabyte.kotter.runtime.terminal.createOrReuse
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import platform.posix.*
import kotlin.concurrent.AtomicInt

// Workaround needed for the fact that K/N doesn't expose
// platform.posix.TIOCGWINSZ in macos platforms at this posix layer
internal expect val TIOCGWINSZ: ULong

private val windowChangeSignalReceived = AtomicInt(0)

// Thanks to https://viewsourcecode.org/snaptoken/kilo/index.html!
actual class NativeTerminal : Terminal {
    init {
        if (isatty(STDOUT_FILENO) == 0 || isatty(STDIN_FILENO) == 0) throw CreateNativeTerminalException()
    }

    val origTermios = nativeHeap.alloc<termios>().apply {
        val origTermios = this
        tcgetattr(STDIN_FILENO, origTermios.ptr)

        memScoped {
            // Enter raw mode and disable some commands we don't want Kotter apps to worry about
            // See also org.jline.terminal.impl.enterRawMode and the post linked earlier in this file
            val newTermios = alloc<termios>()
            memcpy(newTermios.ptr, origTermios.ptr, sizeOf<termios>().convert())

            newTermios.c_iflag = newTermios.c_iflag.and((IXON.or(ICRNL.or(INLCR))).inv().convert())
            newTermios.c_lflag = newTermios.c_lflag.and((ECHO.or(ICANON.or(IEXTEN))).inv().convert())
            newTermios.c_cc[VMIN] = 0U
            newTermios.c_cc[VTIME] = 1U

            tcsetattr(STDIN_FILENO, TCSAFLUSH, newTermios.ptr)
        }
    }

    init {
        printf("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}?25l") // hide the cursor
        fflush(stdout) // Needed or else the command seems to get missed

        signal(SIGWINCH, staticCFunction { _: Int -> windowChangeSignalReceived.value = 1 })
    }

    private var _size: TerminalSize? = null
    override val size: TerminalSize
        get() {
            return memScoped {
                val winsize = alloc<winsize>()
                ioctl(STDOUT_FILENO, TIOCGWINSZ, winsize.ptr)
                _size.createOrReuse(winsize.ws_col.toInt(), winsize.ws_row.toInt())
                    .also { _size = it }
            }
        }

    private val mutableEvents = Terminal.MutableEvents()
    override val events = mutableEvents.asReadOnly()

    private var closed = false

    override fun write(text: String) {
        print(text)
    }

    private val charFlow: SharedFlow<Int> by lazy {
        flow {
            var quit = false
            val context = currentCoroutineContext()
            memScoped {
                val cVar = alloc<IntVar>()
                while (!quit && context.isActive) {
                    val readResult = read(STDIN_FILENO, cVar.ptr, 1u)

                    // `read` calls can get interrupted by signals before continuing. Check if a signal that we
                    // registered for was tripped.
                    if (windowChangeSignalReceived.compareAndSet(1, 0)) {
                        mutableEvents.sizeChanged.emit(size)
                    }

                    if (closed) {
                        // terminal was just closed between this read and last read
                        quit = true
                    } else if (readResult > 0L) {
                        emit(cVar.value)
                    } else if (readResult == -1L) {
                        if (errno != EINTR) { // interrupt errors are OK and even expected
                            quit = true
                        }
                    }

                    yield()
                }
            }
        }.shareIn(CoroutineScope(KotterDispatchers.IO), SharingStarted.Lazily)
    }

    override fun read() = charFlow

    override fun clear() {
        system("clear")
    }

    override fun close() {
        printf("${Ansi.CtrlChars.ESC}${Ansi.EscSeq.CSI}?25h") // restore the cursor
        fflush(stdout)
        tcsetattr(STDIN_FILENO, TCSAFLUSH, origTermios.ptr)
        nativeHeap.free(origTermios)
        closed = true
    }
}
