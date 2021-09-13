package com.varabyte.konsole.runtime.internal.ansi.commands

import com.varabyte.konsole.runtime.KonsoleState
import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.render.Renderer

/**
 * A console command which prints text to the screen, and should ensure the current state effects are applied if not
 * already.
 */
internal open class KonsoleTextCommand(text: String) : KonsoleCommand(text) {
    override fun applyTo(state: KonsoleState, renderer: Renderer) {
        if (text != "\n") {
            state.applyTo(renderer)
        }
        super.applyTo(state, renderer)
    }
}

internal class CharCommand(char: Char) : KonsoleTextCommand(char.toString()) {
    init {
        require(char != '\n') { "Newlines should be represented by the NEWLINE_COMMAND" }
    }
}
internal class TextCommand(text: CharSequence) : KonsoleTextCommand(text.toString()) {
    init {
        require(!text.contains("\n")) { "Newlines should be represented by the NEWLINE_COMMAND" }
    }
}
internal val NEWLINE_COMMAND = object : KonsoleTextCommand("\n") {
    override fun applyTo(state: KonsoleState, renderer: Renderer) {
        // In some terminals, if you have a background color enabled, and you append a newline, the background color
        // extends to the end of the next line. This looks awful, as if the background color is leaking, and
        // unfortunately with Konsole this would happen every time you did something like:
        // `blue(BG) { textLine("Hello") }; textLine("World")`
        // Above, a user would expect for the world "Hello" to be backgrounded by blue and for "World" to show up in
        // normal colors on the next line, but instead "Hello" is blue (good) and "World" would look correct (good) but
        // the whole line trailing AFTER "World" would be blue (really bad)
        //
        // The way we fix this here is by detecting when a background color is set and resetting state BEFORE the
        // newline. When text is encountered again, any deferred state will be reapplied.
        if (state.applied.bgColor != null) {
            state.applied.bgColor = null
            renderer.appendCommand(BG_CLEAR_COMMAND)
        }
        super.applyTo(state, renderer)
    }
}