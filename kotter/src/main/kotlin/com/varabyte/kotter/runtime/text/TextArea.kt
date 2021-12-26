package com.varabyte.kotter.runtime.text

interface TextArea {
    /**
     * Return the number of lines in this text area, further using an optional passed-in width which, if set, means
     * the terminal will auto-add a newline for wrapping at that column.
     */
    fun numLines(width: Int = Int.MAX_VALUE): Int
    fun isEmpty(): Boolean
    fun toRawText(): String
}