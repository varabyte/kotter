package com.varabyte.konsole.foundation.anim

import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.timer.addTimer
import com.varabyte.konsole.runtime.KonsoleApp
import com.varabyte.konsole.runtime.KonsoleBlock
import com.varabyte.konsole.runtime.concurrent.ConcurrentScopedData
import java.time.Duration

private object AnimSetKey : ConcurrentScopedData.Key<MutableSet<KonsoleAnim>> {
    override val lifecycle = KonsoleBlock.RunScope.Lifecycle
}

private fun ConcurrentScopedData.prepareAnim(anim: KonsoleAnim) {
    putIfAbsent(AnimSetKey, { mutableSetOf() }) {
        if (this.add(anim)) {
            addTimer(KonsoleAnim.ONE_FRAME_60FPS, repeat = true) {
                anim.elapse(duration)
            }
        }
    }
}

class KonsoleAnim internal constructor(private val app: KonsoleApp, val template: Template): CharSequence {
    companion object {
        val ONE_FRAME_60FPS = Duration.ofMillis(16)
    }

    class Template(val frames: List<String>, val frameDuration: Duration) {
        init {
            require(!frameDuration.isNegative && !frameDuration.isZero) { "Invalid animation created with non-positive frame length" }
            require(frames.isNotEmpty()) { "Invalid animation created with no frames" }
        }
    }

    private var elapsedMs: Int = 0
    private var currFrame by app.konsoleVarOf(template.frames[0])

    private val frameMs = template.frameDuration.toMillis().toInt()
    private val animMs = frameMs * template.frames.size

    internal fun elapse(duration: Duration) {
        elapsedMs = (elapsedMs + duration.toMillis().toInt()) % animMs
        currFrame = template.frames[elapsedMs / frameMs]
    }

    /**
     * We wrap all animation property accesses in this special block which kickstarts the timer for this animation if
     * it hasn't already been done so.
     */
    private fun <R> readProperty(block: () -> R): R {
        app.data.prepareAnim(this)
        return block()
    }

    override fun toString() = readProperty { currFrame }
    override val length get() = readProperty { currFrame.length }
    override fun get(index: Int) = readProperty { currFrame[index] }
    override fun subSequence(startIndex: Int, endIndex: Int) = readProperty { currFrame.subSequence(startIndex, endIndex) }
}

fun KonsoleApp.konsoleAnimOf(template: KonsoleAnim.Template) = KonsoleAnim(this, template)
fun KonsoleApp.konsoleAnimOf(frames: List<String>, frameDuration: Duration) =
    KonsoleAnim(this, KonsoleAnim.Template(frames, frameDuration))