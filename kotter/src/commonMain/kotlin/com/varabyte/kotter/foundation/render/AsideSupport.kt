package com.varabyte.kotter.foundation.render

import com.varabyte.kotter.runtime.*
import com.varabyte.kotter.runtime.render.*

@Deprecated(
    "AsideRenderScope should have been declared under the runtime package originally. Please change `foundation` to `runtime` in the import.",
    replaceWith = ReplaceWith("com.varabyte.kotter.runtime.render.AsideRenderScope")
)
typealias AsideRenderScope = com.varabyte.kotter.runtime.render.AsideRenderScope

/**
 * A one-off render block that can be triggered within a [RenderScope.run] block.
 *
 * Occasionally, it is useful to generate a bit of extra text that doesn't really belong in the main rendering area.
 * An aside call looks like:
 *
 * ```
 * section {
 *   textLine("Main render text")
 * }.run {
 *   aside { textLine("Aside text") }
 * }
 * ```
 *
 * For example, you could use an aside block in a text adventure game, where there was a main area describing the
 * current room  with asides used to output the result of user actions:
 *
 * ```
 * val gameEngine = ...
 * section {
 *   textLine(gameEngine.roomDescription())
 *   textLine("Your move?")
 *   for (choice in gameEngine.choices()) {
 *     text("* "); textLine(choice)
 *   }
 *   input()
 * }.runUntilSignal {
 *   onInputEntered {
 *     val result = gameEngine.handle(input); clearInput()
 *     aside { textLine(result) }
 *     if (gameEngine.shouldQuit) {
 *       signal()
 *     } else {
 *       rerender() // The game engine state changed, update the section!
 *     }
 *   }
 * }
 * ```
 *
 * which might generate output like:
 *
 * ```text
 * You moved north.            <-- From aside
 * You picked up a torch.      <-- From aside
 * You moved east.             <-- From aside
 *
 * ▼ The active rendering area begins here ▼
 * You are in a big room with three doors, one to the west,
 * one to the east, and one to the north. There is a coin
 * on the floor.
 *
 * Your move?
 * * go east
 * * go west
 * * go north
 * * pick up coin
 * pick up co█
 * ```
 *
 * The aside block essentially *prepends* text in front of the active, constantly repainting block. It gives the user
 * the illusion that there's a main rendering area that spits out historical, static text in its wake as a side effect
 * of it running.
 *
 * A compiler program could be another good example of this -- you might have a spinner plus some text saying which
 * file is currently being compiled, with finished files output using asides.
 */
fun RunScope.aside(render: com.varabyte.kotter.runtime.render.AsideRenderScope.() -> Unit) {
    val session = section.session

    val asideRenderer =
        Renderer(session) { com.varabyte.kotter.runtime.render.AsideRenderScope(it) }.apply { render(render) }
    session.data.putIfAbsent(AsideRendersKey, { mutableListOf() }) {
        add(asideRenderer)
        section.requestRerender()
    }
}
