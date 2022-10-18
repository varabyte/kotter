package com.varabyte.kotter.foundation.input

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.timer.TestTimer
import com.varabyte.kotter.foundation.timer.useTestTimer
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotter.terminal.resolveRerenders
import com.varabyte.kotter.terminal.sendCode
import com.varabyte.kotter.terminal.type
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.fail

class InputSupportTest {
    @Test
    fun `runUntilKeyPressed exits after the corresponding key is pressed`() = testSession { terminal ->
        section {}.runUntilKeyPressed(Keys.Q) {
            terminal.type('q')
        }
    }

    @Test
    fun `can type input`() = testSession { terminal ->
        lateinit var typed: String

        section {
            text("> ")
            input()
            text("<")
        }.onFinishing {
            // When input initially renders, the cursor is blinking on.
            // We'll check again after the section exists, when the cursor should be turned off.
            assertThat(terminal.resolveRerenders()).containsExactly(
                "> Hello${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.Colors.CLEAR_INVERT}<${Codes.Sgr.RESET}",
                ""
            ).inOrder()
        }.runUntilInputEntered {
            onInputEntered {
                typed = input
            }

            terminal.type('H', 'e', 'l', 'l', 'o', Ansi.CtrlChars.ENTER)
        }

        assertThat(typed).isEqualTo("Hello")

        // When section has exited, the blinking cursor is removed
        assertThat(terminal.resolveRerenders()).containsExactly(
            "> Hello <${Codes.Sgr.RESET}",
            ""
        ).inOrder()
    }

    @Test
    fun `it is an exception to run two input calls in the same block`() = testSession { terminal ->
        section {
            input()
            assertThrows<IllegalStateException> {
                input()
            }
        }.run()

        section {
            input(id = "first", isActive = true)
            assertThrows<IllegalStateException> {
                input(id = "second", isActive = true)
            }
        }.run()
    }

    @Test
    fun `cursor blinks off and on`() = testSession { terminal ->
        var timer: TestTimer? = null
        val renderFinished = ArrayBlockingQueue<Unit>(1)
        var numRenders = 0

        section {
            // Timer must be set before input is called the first time
            if (timer == null) {
                timer = data.useTestTimer()
            }

            text("> ")
            input(initialText = "Hello")
            text("<")
        }.onRendered {
            val currentTime = timer!!.currentTime
            when {
                (currentTime % 1000L) == 0L -> {
                    assertThat(terminal.resolveRerenders()).containsExactly(
                        "> Hello${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.Colors.CLEAR_INVERT}<${Codes.Sgr.RESET}",
                        ""
                    ).inOrder()
                }
                (currentTime % 500L) == 0L -> {
                    assertThat(terminal.resolveRerenders()).containsExactly(
                        "> Hello <${Codes.Sgr.RESET}",
                        ""
                    ).inOrder()
                }
                else -> {
                    fail("Unexpected fake timer time: $currentTime")
                }
            }

            numRenders++
            renderFinished.add(Unit)
        }.run {
            renderFinished.take()

            val timer = timer!! // Set in section block
            // Dev note: We intentionally end on a state where the cursor is not blinking. Otherwise, we'd have to
            // handle extra logic to test for the additional rerender that happens AFTERWARDS which disables the
            // blinking cursor. But we already test that in a different unit test, so we sidestep the extra work here.
            for (i in 0 until 5) {
                timer.fastForward(Duration.ofMillis(BLINKING_DURATION_MS.toLong()))
                renderFinished.take()
            }
        }

        // Initial render + 5 rerenders due to timer elapsed / blinking cursor
        assertThat(numRenders).isEqualTo(6)
    }

    @Test
    fun `input calls are activated when first rendered and deactivated on section end`() = testSession { terminal ->
        val wasActivated = CountDownLatch(1)
        val wasDeactivated = CountDownLatch(1)

        section {
            input()
        }.runUntilInputEntered {
            onInputActivated {
                // Auto-activated on initial render
                wasActivated.countDown()
            }

            onInputDeactivated {
                // Auto-deactivated when section exists
                wasDeactivated.countDown()
            }

            terminal.type(Ansi.CtrlChars.ENTER)
        }

        // Verify that activation and deactivation events occurred
        wasActivated.await()
        wasDeactivated.await()
    }

    @Test
    fun `can use arrow keys to navigate input`() = testSession { terminal ->
        section {
            input(initialText = "Hello")
        }.onFinishing {
            // When input first renders, the cursor is blinking on.
            // If we tested this after the section block, the cursor would be gone
            assertThat(terminal.resolveRerenders()).containsExactly(
                "He${Codes.Sgr.Colors.INVERT}l${Codes.Sgr.Colors.CLEAR_INVERT}lo ${Codes.Sgr.RESET}",
                ""
            ).inOrder()
        }.runUntilInputEntered {
            // At this point, cursor is PAST the o
            terminal.sendCode(Codes.Keys.LEFT) // o
            terminal.sendCode(Codes.Keys.LEFT) // l (second)
            terminal.sendCode(Codes.Keys.LEFT) // l (first)
            terminal.type(Ansi.CtrlChars.ENTER)
        }
    }

    @Test
    fun `can use home to move to the front of input`() = testSession { terminal ->
        section {
            input(initialText = "Hello")
        }.onFinishing {
            // When input first renders, the cursor is blinking on.
            // If we tested this after the section block, the cursor would be gone
            assertThat(terminal.resolveRerenders()).containsExactly(
                "${Codes.Sgr.Colors.INVERT}H${Codes.Sgr.Colors.CLEAR_INVERT}ello ${Codes.Sgr.RESET}",
                ""
            ).inOrder()
        }.runUntilInputEntered {
            terminal.sendCode(Codes.Keys.HOME)
            terminal.type(Ansi.CtrlChars.ENTER)
        }
    }

    @Test
    fun `can use end to move to the end of input`() = testSession { terminal ->
        section {
            input(initialText = "Hello")
        }.onFinishing {
            // When input first renders, the cursor is blinking on.
            // If we tested this after the section block, the cursor would be gone
            assertThat(terminal.resolveRerenders()).containsExactly(
                "Hello${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.RESET}", // CLEAR_INVERT lumped into RESET
                ""
            ).inOrder()
        }.runUntilInputEntered {
            terminal.sendCode(Codes.Keys.HOME) // We know this puts the cursor at the beginning from the previous test
            terminal.sendCode(Codes.Keys.END)
            terminal.type(Ansi.CtrlChars.ENTER)
        }
    }

    @Test
    fun `can delete input in place using the delete key`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input(initialText = "Hello")
        }.runUntilInputEntered {
            onInputEntered {
                typed = input
            }

            // At this point, cursor is PAST the o
            terminal.sendCode(Codes.Keys.LEFT) // o
            terminal.sendCode(Codes.Keys.LEFT) // l (second)
            terminal.sendCode(Codes.Keys.LEFT) // l (first)
            terminal.sendCode(Codes.Keys.DELETE)
            terminal.sendCode(Codes.Keys.DELETE)

            terminal.type(Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("Heo")
    }

    @Test
    fun `can delete input in front of cursor by using the backspace key`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input(initialText = "Hello")
        }.runUntilInputEntered {
            onInputEntered {
                typed = input
            }

            // At this point, cursor is PAST the o
            terminal.sendCode(Codes.Keys.LEFT) // o
            terminal.sendCode(Codes.Keys.LEFT) // l (second)
            // Both BACKSPACE and DELETE keys are interpreted as BACKSPACE due to different terminal behaviors
            terminal.type(Ansi.CtrlChars.BACKSPACE)
            terminal.type(Ansi.CtrlChars.DELETE)

            terminal.type(Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("Hlo")
    }

    @Test
    fun `can use onInputChanged to modify input`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input()
        }.runUntilInputEntered {
            onInputChanged {
                input = input.toLowerCase()
            }
            onInputEntered {
                typed = input
            }

            terminal.type('H', 'E', 'L', 'L', 'O', Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("hello")
    }

    @Test
    fun `onInputChanged can reject input`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input()
        }.runUntilInputEntered {
            onInputChanged {
                if (input.contains('l')) rejectInput()
            }
            onInputEntered {
                typed = input
            }

            terminal.type('H', 'e', 'l', 'l', 'o', Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("Heo")
    }

    @Test
    fun `onInputEntered can reject input`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input()
        }.runUntilInputEntered {
            onInputEntered {
                if (input == "Hell") {
                    rejectInput()
                } else {
                    typed = input
                }
            }

            terminal.type('H', 'e', 'l', 'l', Ansi.CtrlChars.ENTER, 'o', Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("Hello")
    }

    @Test
    fun `onInputEntered can clear input`() = testSession { terminal ->
        lateinit var typed: String

        section {
            text(">")
            input()
            text("<")
        }.runUntilInputEntered {
            onInputEntered {
                typed = input
                clearInput()
            }

            terminal.type('H', 'e', 'l', 'l', 'o', Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("Hello")

        assertThat(terminal.resolveRerenders()).containsExactly(
            "> <${Codes.Sgr.RESET}",
            ""
        ).inOrder()
    }

    @Test
    fun `input can show autocompletions`() = testSession { terminal ->
        section {
            input(Completions("Hello"))
        }.onFinishing {
            assertThat(terminal.resolveRerenders()).containsExactly(
                "He${Codes.Sgr.Colors.Fg.BLACK_BRIGHT}${Codes.Sgr.Colors.INVERT}l${Codes.Sgr.Colors.CLEAR_INVERT}lo ${Codes.Sgr.RESET}",
                ""
            ).inOrder()

        }.runUntilInputEntered {
            terminal.type('H', 'e', Ansi.CtrlChars.ENTER)
        }
    }

    @Test
    fun `input can be completed by pressing right`() = testSession { terminal ->
        section {
            input(Completions("Hello"))
        }.runUntilInputEntered {
            terminal.type('H', 'e')
            terminal.sendCode(Codes.Keys.RIGHT)
            terminal.type(Ansi.CtrlChars.ENTER)
        }

        assertThat(terminal.resolveRerenders()).containsExactly(
            "Hello ${Codes.Sgr.RESET}",
            ""
        ).inOrder()
    }

    @Test
    fun `input can be transformed`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input(transform = { '*' })
        }.runUntilInputEntered {
            onInputEntered {
                typed = input
            }

            terminal.type('p', 'a', 's', 's', 'w', 'o', 'r', 'd', Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("password")

        assertThat(terminal.resolveRerenders()).containsExactly(
            "******** ${Codes.Sgr.RESET}",
            ""
        ).inOrder()
    }

    @Test
    fun `can render two inputs with different IDs`() = testSession { terminal ->
        var secondIsActive by liveVarOf(false)

        section {
            input(id = "first", isActive = !secondIsActive); textLine()
            input(id = "second", isActive = secondIsActive)
        }.onFinishing {
            // When input initially renders, the cursor is blinking on.
            assertThat(terminal.resolveRerenders()).containsExactly(
                "first",
                "second${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.RESET}", // CLEAR_INVERT skipped because RESET handles it
                ""
            ).inOrder()
        }.run {
            onKeyPressed {
                if (key == Keys.DOWN) {
                    secondIsActive = true
                }
            }

            terminal.type("first")
            terminal.sendCode(Codes.Keys.DOWN)
            terminal.type("second")
        }
    }
}