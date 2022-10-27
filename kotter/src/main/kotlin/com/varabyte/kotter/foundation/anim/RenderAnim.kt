package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.render.RenderScope
import java.time.Duration

/**
 * An [Anim] that triggers a render callback every frame.
 *
 * The animation callback is scoped to a [RenderScope] and can use any of the methods provided by it, such as `color`,
 * `textLine`, etc.
 *
 * Using one looks like this:
 *
 * ```
 * session {
 *   val colorAnim = renderAnimOf(Color.values().size, Duration.ofMillis(250)) { i ->
 *     color(Color.values()[i])
 *   }
 *   section {
 *     colorAnim(this) // Side-effect: sets the color for this section
 *     text("RAINBOW")
 *   }.runUntilSignal { ... }
 * }
 * ```
 *
 * If all you're doing is rendering text, consider using [TextAnim] instead.
 */
class RenderAnim internal constructor(session: Session, val template: Template)
    : Anim(session, template.numFrames, template.frameDuration) {

    /**
     * A template for a text animation, useful if you want to define an animation once but instantiate several copies of
     * it throughout your program.
     */
    class Template(val numFrames: Int, val frameDuration: Duration, val handler: RenderScope.(Int) -> Unit) {
        init {
            require(!frameDuration.isNegative && !frameDuration.isZero) { "Invalid animation created with non-positive frame length" }
            require(numFrames > 0) { "Invalid animation created with no frames" }
        }
    }

    operator fun invoke(renderScope: RenderScope) {
        requestAnimate()
        template.handler.invoke(renderScope, currFrame)
    }
}

fun Session.renderAnimOf(template: RenderAnim.Template) = RenderAnim(this, template)
/** Instantiate a [RenderAnim] tied to the current [Session]. */
fun Session.renderAnimOf(numFrames: Int, frameDuration: Duration, handler: RenderScope.(Int) -> Unit) =
    RenderAnim(this, RenderAnim.Template(numFrames, frameDuration, handler))