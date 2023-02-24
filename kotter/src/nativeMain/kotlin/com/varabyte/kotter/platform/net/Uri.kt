package com.varabyte.kotter.platform.net

actual class Uri actual constructor(private val uri: String) {
    // TODO: Parse and verify URI, like how the JVM does it.
    override fun toString() = uri
}