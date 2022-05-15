package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.runtime.Session
import java.time.Duration

class TextAnim internal constructor(session: Session, val template: Template)
    : CharSequence, Anim(session, template.frames.size, template.frameDuration) {

    class Template(val frames: List<String>, val frameDuration: Duration) {
        init {
            require(!frameDuration.isNegative && !frameDuration.isZero) { "Invalid animation created with non-positive frame length" }
            require(frames.isNotEmpty()) { "Invalid animation created with no frames" }
        }
    }

    private val currText get() = template.frames[currFrame]

    private var referencedLastFrame = false
    private var callbackAdded = false
    private var stopTimer = false

    /**
     * We wrap all animation property accesses in this special block which kickstarts the timer for this animation if
     * it hasn't already been done so.
     */
    private fun <R> readProperty(block: () -> R): R {
        requestAnimate()
        return block()
    }

    override fun toString() = readProperty { currText }
    override val length get() = readProperty { currText.length }
    override fun get(index: Int) = readProperty { currText[index] }
    override fun subSequence(startIndex: Int, endIndex: Int) = readProperty { currText.subSequence(startIndex, endIndex) }
}

fun Session.textAnimOf(template: TextAnim.Template) = TextAnim(this, template)
fun Session.textAnimOf(frames: List<String>, frameDuration: Duration) =
    TextAnim(this, TextAnim.Template(frames, frameDuration))