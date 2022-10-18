package com.varabyte.kotter.foundation.input

import com.varabyte.kotter.foundation.testSession
import com.varabyte.kotter.runtime.internal.ansi.Ansi
import com.varabyte.kotter.terminal.type
import com.varabyte.truthish.assertThat
import kotlin.test.Test

class InputSupportTest {
    @Test
    fun `runUntilKeyPressed exits after the corresponding key is pressed`() = testSession { terminal ->
        section {}.runUntilKeyPressed(Keys.Q) {
            terminal.type('q')
        }
    }

    @Test
    fun `can get input from input()`() = testSession { terminal ->
        lateinit var typed: String

        section {
            input()
        }.runUntilInputEntered {
            onInputEntered {
                typed = input
            }

            terminal.type('H', 'e', 'l', 'l', 'o', Ansi.CtrlChars.ENTER)
        }
        assertThat(typed).isEqualTo("Hello")
    }
}