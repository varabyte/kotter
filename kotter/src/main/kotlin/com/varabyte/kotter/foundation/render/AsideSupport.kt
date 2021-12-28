package com.varabyte.kotter.foundation.render

import com.varabyte.kotter.runtime.AsideRendersKey
import com.varabyte.kotter.runtime.Section
import com.varabyte.kotter.runtime.render.RenderScope
import com.varabyte.kotter.runtime.render.Renderer

/**
 * An aside block is essentially a `section`-lite block. It looks similar to one, but with a few less features (e.g. the
 * `input` method doesn't work on it), and it only ever runs once.
 *
 * The aside block is prepended in front of the active block, and this is useful if the main active area (that is
 * repainting / refreshing constantly) wants to generate additional text as a side effect (e.g. a compiler that
 * generates a history warnings and errors as it runs).
 */
fun Section.RunScope.aside(block: RenderScope.() -> Unit) {
    val session = section.session

    val asideRenderer = Renderer(session).apply { render(block) }

    session.data.putIfAbsent(AsideRendersKey, { mutableListOf() }) {
        add(asideRenderer)
        section.requestRerender()
    }
}