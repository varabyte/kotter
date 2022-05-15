package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.timer.addTimer
import com.varabyte.kotter.runtime.Session
import java.time.Duration

abstract class Anim(protected val session: Session, protected val numFrames: Int, frameDuration: Duration) {
    companion object {
        val ONE_FRAME_60FPS = Duration.ofMillis(16)
    }

    protected var elapsedMs: Int = 0
    protected var currFrame by session.liveVarOf(0)
        private set

    protected val frameMs = frameDuration.toMillis().toInt()
    protected val animMs = frameMs * numFrames

    protected fun elapse(duration: Duration) {
        elapsedMs = (elapsedMs + duration.toMillis().toInt()) % animMs
        currFrame = elapsedMs / frameMs
    }


    private var animRequestedThisFrame = false
    private var listeningForRenderCallback = false
    private var stopTimer = false

    /**
     * Indicate a request that we'd like to animate this animation on this frame.
     *
     * This should be done any time the animation is referenced within a render pass. There is no harm in calling this
     * method multiple times in a single render pass. If a render pass occurs without an animation request, then the
     * timer for this animation will be cleared.
     */
    protected fun requestAnimate() {
        val activeSection = session.activeSection ?: return

        animRequestedThisFrame = true
        if (!listeningForRenderCallback) {
            listeningForRenderCallback = true
            activeSection.onRendered {
                if (!animRequestedThisFrame) {
                    stopTimer = true
                    listeningForRenderCallback = false
                    removeListener = true
                }
                animRequestedThisFrame = false // Reset for next render frame
            }

            // Set key to this animation, just to show our intention / ensure that we'll only ever start one timer per
            // animation instance ever, even though due to the logic of this block, it shouldn't be necessary.
            session.data.addTimer(ONE_FRAME_60FPS, repeat = true, key = this) {
                elapse(duration)
                if (stopTimer) {
                    repeat = false
                    stopTimer = false // Reset for next time this animation starts
                }
            }
        }
    }
}