package com.varabyte.konsole.runtime.internal.text

internal interface TextArea {
    val numLines: Int
    fun isEmpty(): Boolean
    val lastChar: Char?
}

internal class MutableTextArea : TextArea {
    private var stringBuilder = StringBuilder()

    override var numLines = 1
        private set

    override fun isEmpty() = stringBuilder.isEmpty()
    override val lastChar: Char? get() = stringBuilder.lastOrNull()

    fun clear(): MutableTextArea {
        stringBuilder.clear()
        numLines = 1
        return this
    }

    fun append(c: Char): MutableTextArea {
        stringBuilder.append(c)
        if (c == '\n') ++numLines
        return this
    }

    fun append(str: String): MutableTextArea {
        val lines = str.split('\n')
        lines.forEachIndexed { index, line ->
            stringBuilder.append(line)
            if (index < lines.size - 1) {
                append('\n')
            }
        }
        return this
    }

    override fun toString(): String {
        return stringBuilder.toString()
    }
}