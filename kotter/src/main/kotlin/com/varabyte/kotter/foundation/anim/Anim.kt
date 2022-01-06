package com.varabyte.kotter.foundation.anim

import com.varabyte.kotter.foundation.liveVarOf
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
}