package com.varabyte.kotter.runtime.internal.ansi.commands

import com.varabyte.kotter.runtime.*
import com.varabyte.kotter.runtime.internal.*
import com.varabyte.kotter.runtime.render.*

/**
 * A console command which prints text to the screen, and should ensure the current state effects are applied if not
 * already.
 */
internal open class TextCommand(text: String) : TerminalCommand(text) {
    override fun applyTo(state: SectionState, renderer: Renderer<*>) {
        if (text != "\n") {
            state.applyTo(renderer)
        }
        super.applyTo(state, renderer)
    }
}

internal class CharCommand(char: Char) : TextCommand(char.toString()) {
    init {
        require(char != '\n') { "Newlines should be represented by the NEWLINE_COMMAND" }
    }
}

internal class CharSequenceCommand(text: CharSequence) : TextCommand(text.toString()) {
    init {
        require(text.isNotEmpty()) { "Do not create empty CharSequenceCommands as they'd force state to apply for no reason" }
        require(!text.contains("\n")) { "Newlines should be represented by the NEWLINE_COMMAND" }
    }
}

internal open class NewlineCommand(text: String) : TextCommand(text)

internal val NEWLINE_COMMAND = object : NewlineCommand("\n") {
    override fun applyTo(state: SectionState, renderer: Renderer<*>) {
        // In some terminals, if you have a background color enabled, and you append a newline, the background color
        // extends to the end of the next line. This looks awful, as if the background color is leaking, and
        // unfortunately this would happen every time you did something like:
        // `blue(BG) { textLine("Hello") }; textLine("World")`
        // Above, a user would expect for the world "Hello" to be backgrounded by blue and for "World" to show up in
        // normal colors on the next line, but instead "Hello" is blue (good) and "World" would look correct (good) but
        // the whole line trailing AFTER "World" would be blue (really bad).
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

/**
 * A placeholder command which represents when the terminal will insert a newline automatically.
 *
 * While on its own it's a no-op, by inserting it into the list of commands, it can be detected and used to influence
 * repaint logic.
 */
internal val IMPLICIT_NEWLINE_COMMAND = NewlineCommand("")
