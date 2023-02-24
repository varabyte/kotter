package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.render.RenderScope
import kotlin.time.Duration

/**
 * An [Anim] that triggers a render callback every frame.
 *
 * The animation callback is scoped to a [RenderScope] and can therefore use any of the methods provided by it, such as
 * `color`, `textLine`, etc.
 *
 * You trigger a render animation by calling it with a render scope as its parameter. Using one looks like this:
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
class RenderAnim internal constructor(session: Session, private val template: Template)
    : Anim(session, template.numFrames, template.frameDuration, template.looping) {

    /**
     * A template for a render animation, useful if you want to define an animation once but instantiate several copies
     * of it throughout your program.
     */
    class Template(
        val numFrames: Int,
        val frameDuration: Duration,
        val looping: Boolean = true,
        val handler: RenderScope.(Int) -> Unit
    ) {
        init {
            require(frameDuration.isPositive()) { "Invalid animation created with non-positive frame length" }
            require(numFrames > 0) { "Invalid animation created with no frames" }
        }
    }

    /** Apply the current frame of this render animation onto the target [renderScope]. */
    operator fun invoke(renderScope: RenderScope) {
        requestAnimate()
        template.handler.invoke(renderScope, currFrame)
    }
}

fun Session.renderAnimOf(template: RenderAnim.Template) = RenderAnim(this, template)
/**
 * Instantiate a [RenderAnim] tied to the current [Session].
 *
 * @param numFrames The number of frames in this animation. This value must be greater than 0.
 * @param frameDuration The length of each frame.
 * @param looping If true, this animation will play forever in a loop. Otherwise, it will stop when it reaches the end.
 */
fun Session.renderAnimOf(numFrames: Int, frameDuration: Duration, looping: Boolean = true, handler: RenderScope.(Int) -> Unit) =
    RenderAnim(this, RenderAnim.Template(numFrames, frameDuration, looping, handler))