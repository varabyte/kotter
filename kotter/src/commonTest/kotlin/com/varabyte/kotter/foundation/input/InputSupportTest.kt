package com.varabyte.kotter.foundation.input

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotter.runtime.internal.ansi.Ansi.Csi.Codes
import com.varabyte.kotterx.test.foundation.*
import com.varabyte.kotterx.test.runtime.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.truthish.assertThat
import com.varabyte.truthish.assertThrows
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

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
            blockUntilRenderMatches(terminal) {
                text("> Hello"); invert(); text(' '); clearInvert(); text("<")
            }

            terminal.type(Ansi.CtrlChars.ENTER)
        }

        assertThat(typed).isEqualTo("Hello")

        terminal.assertMatches {
            // When section has exited, the blinking cursor is removed
            text("> Hello <")
        }
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
    fun `input initialText will have newlines stripped`() = testSession { terminal ->
        section {
            input(initialText = "No\n\nNewlines")
        }.run()

        terminal.assertMatches {
            text("NoNewlines ")
        }
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
            @Suppress("NAME_SHADOWING") val timer = timer!!

            // Run a few times just to verify that the blinking continues in a cycle
            for (i in 0 until 3) {
                blockUntilRenderMatches(terminal) {
                    text("> Hello"); invert(); text(' '); clearInvert(); text("<")
                }
                timer.fastForward(BLINKING_DURATION_MS.milliseconds)

                blockUntilRenderMatches(terminal) {
                    text("> Hello <")

                }
                timer.fastForward(BLINKING_DURATION_MS.milliseconds)
            }
        }
    }

    @Test
    fun `input calls are activated when first rendered and deactivated on section end`() = testSession { terminal ->
        val wasActivated = CompletableDeferred<Unit>()
        val wasDeactivated = CompletableDeferred<Unit>()

        section {
            input()
        }.runUntilInputEntered {
            onInputActivated {
                // Auto-activated on initial render
                wasActivated.complete(Unit)
            }

            onInputDeactivated {
                // Auto-deactivated when section exists
                wasDeactivated.complete(Unit)
            }

            terminal.type(Ansi.CtrlChars.ENTER)
        }

        // Verify that activation and deactivation events occurred
        runBlocking {
            wasActivated.await()
            wasDeactivated.await()
        }
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

            blockUntilRenderMatches(terminal) {
                text("He"); invert(); text('l'); clearInvert(); text("lo ")
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

            blockUntilRenderMatches(terminal) {
                invert(); text('H'); clearInvert(); text("ello ")
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

            blockUntilRenderMatches(terminal) {
                // No need to "clearInvert". It's merged into the "reset" command added at the end of the section.
                text("Hello"); invert(); text(' ')
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

            blockUntilRenderMatches(terminal) {
                text("H"); invert(); text('l'); clearInvert(); text("lo ")
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

            blockUntilRenderMatches(terminal) {
                text("Hel"); invert(); text(' ')
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
                input = input.lowercase()
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

        terminal.assertMatches {
            text("> <")
        }
    }

    @Test
    fun `input can show autocompletions`() = testSession { terminal ->
        section {
            input(Completions("Hello"))
        }.run {
            terminal.type('H', 'e')

            blockUntilRenderMatches(terminal) {
                text("He"); black(isBright = true); invert(); text('l'); clearInvert(); text("lo ")
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

            blockUntilRenderMatches(terminal) {
                text("Hello"); invert(); text(' ')
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

        terminal.assertMatches {
            text("******** ")
        }
    }

    @Test
    fun `input rendering can be formatted with customFormat`() = testSession { terminal ->
        section {
            cyan {
                input(
                    customFormat = { if (index in 2..4) red() },
                    initialText = "active test",
                    isActive = true,
                    id = "active"
                ); textLine()
                input(
                    customFormat = { if (isActive) green() else black() },
                    initialText = "inactive test",
                    isActive = false,
                    id = "inactive"
                )
            }
        }.run()

        terminal.assertMatches {
            cyan {
                text("ac"); red(); text("tiv"); cyan(); text("e test"); text(' '); textLine()
                // Extra space where cursor goes -----------------------------^
                black { text("inactive test") }
            }
        }
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
            blockUntilRenderMatches(terminal) {
                text("first"); invert(); textLine(' ') // < inverted space is the cursor
                // no need to clearInvert, since no text ever follows the cursor
            }

            terminal.sendCode(Codes.Keys.DOWN)
            blockUntilRenderMatches(terminal) {
                textLine("first")
                invert(); text(' ')
            }

            terminal.type("second")
            blockUntilRenderMatches(terminal) {
                textLine("first")
                text("second"); invert(); text(' ')
            }
        }
    }

    @Test
    fun `can set and get input directly even though you shouldn't usually`() = testSession { terminal ->

        section {
            text("1>"); input(id = "first", isActive = true); textLine("<")
            text("2>"); input(id = "second", isActive = false); textLine("<")
        }.run {
            blockUntilRenderMatches(terminal) {
                text("1>"); invert(); text(' '); clearInvert(); textLine("<")
                textLine("2><")
            }

            assertThat(getInput(id = "first")).isEqualTo("")
            assertThat(getInput(id = "second")).isEqualTo("")

            setInput("456", id = "second")
            blockUntilRenderMatches(terminal) {
                text("1>"); invert(); text(' '); clearInvert(); textLine("<")
                textLine("2>456<")
            }

            assertThat(getInput(id = "first")).isEqualTo("")
            assertThat(getInput(id = "second")).isEqualTo("456")

            setInput("123", cursorIndex = 1, id = "first")
            blockUntilRenderMatches(terminal) {
                text("1>1"); invert(); text('2'); clearInvert(); textLine("3 <")
                textLine("2>456<")
            }

            assertThat(getInput(id = "first")).isEqualTo("123")
            assertThat(getInput(id = "second")).isEqualTo("456")

            assertThat(getInput(id = "none")).isNull()
        }
    }

    @Test
    fun `setInput will have its newlines stripped`() = testSession {
        section {
            input(initialText = "initial")
        }.run {
            assertThat(getInput()).isEqualTo("initial")
            setInput("No\nNewlines\nAllowed")
            assertThat(getInput()).isEqualTo("NoNewlinesAllowed")
        }
    }
}
