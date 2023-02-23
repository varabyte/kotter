package com.varabyte.kotter.foundation.input

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.timer.TestTimer
import com.varabyte.kotter.foundation.timer.useTestTimer
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.testSession
import com.varabyte.kotterx.test.runtime.blockUntilRenderWhen
import com.varabyte.kotterx.test.terminal.resolveRerenders
import com.varabyte.kotterx.test.terminal.sendCode
import com.varabyte.kotterx.test.terminal.type
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.test.Test

class InputSupportTest {
    @Test
    fun `runUntilKeyPressed exits after the corresponding key is pressed`() = testSession { terminal ->
        section {}.runUntilKeyPressed(Keys.Q) {
            terminal.type('q')
        }
    }

    @Test
    fun `can type and enter input`() = testSession { terminal ->
        lateinit var typed: String

        section {
            text("> ")
            input()
            text("<")
        }.runUntilInputEntered {
            onInputEntered {
                typed = input
            }

            terminal.type('H', 'e', 'l', 'l', 'o')
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "> Hello${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.Colors.CLEAR_INVERT}<${Codes.Sgr.RESET}",
                    ""
                )
            }

            terminal.type(Ansi.CtrlChars.ENTER)
        }

        assertThat(typed).isEqualTo("Hello")

        // When section has exited, the blinking cursor is removed
        assertThat(terminal.resolveRerenders()).containsExactly(
            "> Hello <${Codes.Sgr.RESET}",
            ""
        ).inOrder()
    }

    @Test
    fun `it is an exception to run two active input calls in the same block`() = testSession {
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

        section {
            // Timer must be set before input is called the first time
            if (timer == null) {
                timer = data.useTestTimer()
            }

            text("> ")
            input(initialText = "Hello")
            text("<")
        }.run {
            val timer = timer!!

            // Run a few times just to verify that the blinking continues in a cycle
            for (i in 0 until 3) {
                blockUntilRenderWhen {
                    terminal.resolveRerenders() == listOf(
                        "> Hello${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.Colors.CLEAR_INVERT}<${Codes.Sgr.RESET}",
                        ""
                    )
                }
                timer.fastForward(Duration.ofMillis(BLINKING_DURATION_MS.toLong()))

                blockUntilRenderWhen {
                    terminal.resolveRerenders() == listOf(
                        "> Hello <${Codes.Sgr.RESET}",
                        ""
                    )
                }
                timer.fastForward(Duration.ofMillis(BLINKING_DURATION_MS.toLong()))
            }
        }
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
        }.run {
            // At this point, cursor is PAST the o
            terminal.sendCode(Codes.Keys.LEFT) // o
            terminal.sendCode(Codes.Keys.LEFT) // l (second)
            terminal.sendCode(Codes.Keys.LEFT) // l (first)

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "He${Codes.Sgr.Colors.INVERT}l${Codes.Sgr.Colors.CLEAR_INVERT}lo ${Codes.Sgr.RESET}",
                    ""
                )
            }
        }
    }

    @Test
    fun `can use home to move to the front of input`() = testSession { terminal ->
        section {
            input(initialText = "Hello")
        }.run {
            // At this point, cursor is PAST the o
            terminal.sendCode(Codes.Keys.HOME) // h

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "${Codes.Sgr.Colors.INVERT}H${Codes.Sgr.Colors.CLEAR_INVERT}ello ${Codes.Sgr.RESET}",
                    ""
                )
            }
        }
    }

    @Test
    fun `can use end to move to the end of input`() = testSession { terminal ->
        section {
            input(initialText = "Hello")
        }.run {
            terminal.sendCode(Codes.Keys.HOME) // We know this puts the cursor at the beginning from the previous test
            terminal.sendCode(Codes.Keys.END)

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "Hello${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.RESET}", // CLEAR_INVERT lumped into RESET
                    ""
                )
            }
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
    fun `delete ignored if input rejected`() = testSession { terminal ->
        section {
            input(initialText = "Hello")
        }.run {
            onInputChanged {
                if (input == "Hlo") rejectInput()
            }

            // At this point, cursor is PAST the o
            terminal.sendCode(Codes.Keys.LEFT) // o
            terminal.sendCode(Codes.Keys.LEFT) // l (second)
            terminal.sendCode(Codes.Keys.LEFT) // l (first)
            terminal.sendCode(Codes.Keys.LEFT) // e (first)
            terminal.sendCode(Codes.Keys.DELETE) // Hllo
            terminal.sendCode(Codes.Keys.DELETE) // Would be "Hlo", but rejected
            terminal.sendCode(Codes.Keys.DELETE) // Would be "Hlo", but rejected again

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "H${Codes.Sgr.Colors.INVERT}l${Codes.Sgr.Colors.CLEAR_INVERT}lo ${Codes.Sgr.RESET}",
                    ""
                )
            }
        }
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
            terminal.type(Ansi.CtrlChars.BACKSPACE) // delete l (first)
            terminal.type(Ansi.CtrlChars.DELETE) // delete e

            terminal.type(Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("Hlo")
    }

    @Test
    fun `backspace ignored if input rejected`() = testSession { terminal ->
        section {
            input(initialText = "Hello")
        }.run {
            onInputChanged {
                if (input == "He") rejectInput()
            }

            terminal.type(Ansi.CtrlChars.BACKSPACE)
            terminal.type(Ansi.CtrlChars.BACKSPACE)
            terminal.type(Ansi.CtrlChars.BACKSPACE)
            terminal.type(Ansi.CtrlChars.BACKSPACE)
            terminal.type(Ansi.CtrlChars.BACKSPACE)
            terminal.type(Ansi.CtrlChars.BACKSPACE)

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "Hel${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.RESET}",
                    ""
                )
            }
        }
    }

    @Test
    fun `can use onInputChanged to modify input`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input()
        }.runUntilInputEntered {
            onInputChanged {
                input = input.lowercase(Locale.getDefault())
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
        }.run {
            terminal.type('H', 'e')

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "He${Codes.Sgr.Colors.Fg.BLACK_BRIGHT}${Codes.Sgr.Colors.INVERT}l${Codes.Sgr.Colors.CLEAR_INVERT}lo ${Codes.Sgr.RESET}",
                    ""
                )
            }
        }
    }

    @Test
    fun `input can be completed by pressing right`() = testSession { terminal ->
        section {
            input(Completions("Hello"))
        }.run {
            terminal.type('H', 'e')
            terminal.sendCode(Codes.Keys.RIGHT)

            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "Hello${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.RESET}",
                    ""
                )
            }
        }
    }

    @Test
    fun `input rendering can be transformed with viewMap`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input(viewMap = { '*' })
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
        }.run {
            onKeyPressed {
                if (key == Keys.DOWN) {
                    secondIsActive = true
                }
            }

            terminal.type("first")
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "first${Codes.Sgr.Colors.INVERT} ",
                    "${Codes.Sgr.Colors.CLEAR_INVERT}${Codes.Sgr.RESET}",
                    "",
                )
                // TODO(#90): Note sure why the CLEAR_INVERT command is ending up on the next line, and why it's not
                //  getting folded into the RESET like happens with other cases.
            }

            terminal.sendCode(Codes.Keys.DOWN)
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "first",
                    "${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.RESET}",
                    "",
                )
            }

            terminal.type("second")
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "first",
                    "second${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.RESET}", // CLEAR_INVERT skipped because RESET handles it
                    ""
                )
            }
        }
    }

    @Test
    fun `can set and get input directly (even though you shoudln't usually)`() = testSession { terminal ->

        section {
            text("1>"); input(id = "first", isActive = true); textLine("<")
            text("2>"); input(id = "second", isActive = false); textLine("<")
        }.run {
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "1>${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.Colors.CLEAR_INVERT}<",
                    "2><",
                    "${Codes.Sgr.RESET}",
                )
            }

            assertThat(getInput(id = "first")).isEqualTo("")
            assertThat(getInput(id = "second")).isEqualTo("")

            setInput("456", id = "second")
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "1>${Codes.Sgr.Colors.INVERT} ${Codes.Sgr.Colors.CLEAR_INVERT}<",
                    "2>456<",
                    "${Codes.Sgr.RESET}",
                )
            }

            assertThat(getInput(id = "first")).isEqualTo("")
            assertThat(getInput(id = "second")).isEqualTo("456")

            setInput("123", cursorIndex = 1, id = "first")
            blockUntilRenderWhen {
                terminal.resolveRerenders() == listOf(
                    "1>1${Codes.Sgr.Colors.INVERT}2${Codes.Sgr.Colors.CLEAR_INVERT}3 <",
                    "2>456<",
                    "${Codes.Sgr.RESET}",
                )
            }

            assertThat(getInput(id = "first")).isEqualTo("123")
            assertThat(getInput(id = "second")).isEqualTo("456")

            assertThat(getInput(id = "none")).isNull()
        }
    }
}