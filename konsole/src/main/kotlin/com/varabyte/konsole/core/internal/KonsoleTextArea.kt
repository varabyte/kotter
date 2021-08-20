package com.varabyte.konsole.core.internal

import kotlin.math.max

interface KonsoleTextArea {
    val w: Int
    val h: Int
}

class MutableKonsoleTextArea : KonsoleTextArea {
    private var stringBuilder = StringBuilder()

    private var currLineX = 0
        set(value) {
            field = value
            maxW = max(maxW, value)
        }
    private var currLineY = 0
        set(value) {
            field = value
            maxH = max(maxH, value)
        }
    private var maxW = 0
    private var maxH = 0

    override val w: Int
        get() = maxW
    override val h: Int
        get() = maxH

    fun append(c: Char): MutableKonsoleTextArea {
        when (c) {
            '\n' -> {
                currLineX = 0
                currLineY++
            }
            else -> {
                stringBuilder.append(c)
                currLineX++
            }
        }
        return this
    }

    fun append(str: String): MutableKonsoleTextArea {
        val lines = str.split('\n')
        lines.forEachIndexed { index, line ->
            stringBuilder.append(line)
            currLineX += line.length

            if (index < lines.size - 1) {
                append('\n')
            }
        }
        return this
    }

    fun appendLine(c: Char) = append(c).append('\n')
    fun appendLine(str: String) = append(str).append('\n')
}