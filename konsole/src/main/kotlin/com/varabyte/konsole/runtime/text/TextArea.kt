package com.varabyte.konsole.runtime.text

interface TextArea {
    val numLines: Int
    fun isEmpty(): Boolean
    fun toRawText(): String
}