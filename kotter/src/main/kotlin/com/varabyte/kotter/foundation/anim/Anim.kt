package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.timer.addTimer
import com.varabyte.kotter.runtime.Session
import java.time.Duration

/**
 * Base class for animation types.
 *
 * Handles logic around managing the current frame and running an animation timer.
 *
 * Users won't directly instantiate this class. Check out its inheritors instead.
 *
 * @property numFrames How many frames this animation has.
 */
abstract class Anim protected constructor(protected val session: Session, val numFrames: Int, frameDuration: Duration) {
    companion object {
        /** A useful duration which represents the duration of a single frame for an animation running 60fps */
        val ONE_FRAME_60FPS = Duration.ofMillis(16)
    }

    /**
     * Whether this animation is paused or not.
     *
     * If paused, the animation timer will continue to fire but no longer elapse the animation.
     */
    var paused by session.liveVarOf(false)

    private var _currFrame by session.liveVarOf(0)

    /**
     * Manually set the current frame (0-indexed) that this animation should display.
     *
     * It's not expected users will need to use this ever, but it can be nice to have a way to reset an animation back
     * to its first frame at the very least, i.e. by calling `anim.currFrame = 0`
     *
     * This property will throw an exception if the value passed in is out of bounds.
     */
    var currFrame: Int
        get() = _currFrame
        set(value) {
            if (_currFrame == value) return

            // Note: Another option is to allow people to specify out of bound values and just wrap it ourselves.
            // We may do that later, especially if we get feedback from users asking for it, but for now, since I'm not
            // sure, we'll choose to fail fast. It's easier to go from strict to loose rather than the other way around.
            require(value in 0 until numFrames) {
                "Animation frame out of bounds. Tried to set $value but should be between 0 and ${numFrames - 1}."
            }

            _currFrame = value
            elapsedMs = 0
        }

    // This counter will go up as time passes but get consumed to move frames forward. In other words, it always means
    // "how many frames to move forward from now", as opposed to being a global counter running since the anim started.
    private var elapsedMs: Int = 0
    private val frameMs = frameDuration.toMillis().toInt()

    private fun elapse(duration: Duration) {
        elapsedMs += duration.toMillis().toInt()
        var numFramesToProgress = 0
        while (elapsedMs >= frameMs) {
            numFramesToProgress++
            elapsedMs -= frameMs
        }

        _currFrame = (_currFrame + numFramesToProgress) % numFrames
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
                if (stopTimer) {
                    repeat = false
                    stopTimer = false // Reset for next time this animation starts
                } else {
                    if (!paused) {
                        elapse(duration)
                    }
                }
            }
        }
    }
}