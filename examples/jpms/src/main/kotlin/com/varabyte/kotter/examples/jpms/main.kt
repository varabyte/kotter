package com.varabyte.kotter.examples.jpms

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.anim.textAnimOf
import com.varabyte.kotter.foundation.collections.liveListOf
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.text.ColorLayer.*
import kotlin.time.Duration.Companion.milliseconds

fun main() = session {
    val x = textAnimOf(listOf("x", "o"), 100.milliseconds)
    section {
        textLine("$x")
        textLine("This program is intentionally bland; instead, the focus is on the build script.")
        textLine("However, we include emojis to make sure that the twemoji dependency works: ")
        textLine("* Family: \uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC67\u200D\uD83D\uDC66")
        textLine("* Heart: ❤\uFE0F")
        textLine("* US Flag: \uD83C\uDDFA\uD83C\uDDF8")
    }.runUntilKeyPressed(Keys.Q)
}
