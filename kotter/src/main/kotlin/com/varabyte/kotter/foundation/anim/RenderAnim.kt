package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.timer.addTimer
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotter.runtime.render.RenderScope
import java.time.Duration

class RenderAnim internal constructor(session: Session, val template: Template)
    : Anim(session, template.numFrames, template.frameDuration) {

    class Template(val numFrames: Int, val frameDuration: Duration, val handler: RenderScope.(Int) -> Unit) {
        init {
            require(!frameDuration.isNegative && !frameDuration.isZero) { "Invalid animation created with non-positive frame length" }
            require(numFrames > 0) { "Invalid animation created with no frames" }
        }
    }

    operator fun invoke(renderScope: RenderScope) {
        // This will only add a timer the first time this is called
        session.data.addTimer(ONE_FRAME_60FPS, repeat = true, key = this) { elapse(duration) }
        template.handler.invoke(renderScope, currFrame)
    }
}

fun Session.renderAnimOf(template: RenderAnim.Template) = RenderAnim(this, template)
fun Session.renderAnimOf(numFrames: Int, frameDuration: Duration, handler: RenderScope.(Int) -> Unit) =
    RenderAnim(this, RenderAnim.Template(numFrames, frameDuration, handler))