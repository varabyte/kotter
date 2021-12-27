package com.varabyte.kotter.runtime.text

interface TextArea {
    /**
     * Return the number of characters in each line, excluding newlines.
     */
    val lineLengths: List<Int>
    fun isEmpty(): Boolean
    fun toRawText(): String
}

/**
 * Return the number of lines in this text area, further using an optional passed-in width which, if set, means
 * the terminal will auto-add a newline for wrapping at that column.
 */
fun TextArea.numLines(width: Int = Int.MAX_VALUE): Int {
    val lineLengths = lineLengths
    return lineLengths.size +
            // The line gets an implicit newline once it goes ONE over the terminal width - or in other
            // words, a 20 character line fits perfectly in a 20 column terminal, so don't treat that case
            // as an extra newline until we hit 21 characters
            lineLengths.sumOf { len -> (len - 1) / width }
}