package com.varabyte.kotter.examples.text

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.collections.liveListOf
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.text.ColorLayer.*

fun main() = session {

    val nums = liveListOf(1, 2, 3)
    section {
        nums.forEach { num -> textLine(num.toString()) }
    }.runUntilKeyPressed(Keys.Q) {
        nums.addAll(listOf(4, 5, 6))
    }


}
