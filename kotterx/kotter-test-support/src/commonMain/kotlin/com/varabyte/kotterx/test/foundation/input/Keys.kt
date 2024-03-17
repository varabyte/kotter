package com.varabyte.kotterx.test.foundation.input

import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.runtime.internal.ansi.*
import com.varabyte.kotterx.test.terminal.*

/**
 * Press one or more [Key]s.
 *
 * Technically, a Kotter [Key] represents the final transformation of input passed into a terminal, so using them here
 * as the initial input is technically a bit backwards. But for practicality, it's much easier to use this method than
 * understanding the nuances of what sort of input a terminal needs to result in the expected [Key] to get created.
 *
 * This method works by converting the passed in keys into the actual terminal input that will just result in those keys
 * getting created again on the other end.
 */
suspend fun TestTerminal.press(vararg keys: Key) {
    keys.forEach { key ->
        when (key) {
            is CharKey -> sendKey(key.code.code)

            Keys.ENTER -> type(Ansi.CtrlChars.ENTER)
            Keys.ESC -> type(Ansi.CtrlChars.ESC)
            Keys.BACKSPACE -> type(Ansi.CtrlChars.BACKSPACE)
            Keys.DELETE -> type(Ansi.CtrlChars.DELETE)
            Keys.EOF -> type(Ansi.CtrlChars.EOF)
            Keys.TAB -> type(Ansi.CtrlChars.TAB)

            Keys.UP -> sendCode(Ansi.Csi.Codes.Keys.UP)
            Keys.DOWN -> sendCode(Ansi.Csi.Codes.Keys.DOWN)
            Keys.LEFT -> sendCode(Ansi.Csi.Codes.Keys.LEFT)
            Keys.RIGHT -> sendCode(Ansi.Csi.Codes.Keys.RIGHT)

            Keys.HOME -> sendCode(Ansi.Csi.Codes.Keys.HOME)
            Keys.END -> sendCode(Ansi.Csi.Codes.Keys.END)
            Keys.INSERT -> sendCode(Ansi.Csi.Codes.Keys.INSERT)
            Keys.PAGE_UP -> sendCode(Ansi.Csi.Codes.Keys.PG_UP)
            Keys.PAGE_DOWN -> sendCode(Ansi.Csi.Codes.Keys.PG_DOWN)

            else -> error("Unsupported key: $key")
        }
    }
}
