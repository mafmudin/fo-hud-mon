package dev.fordewe.fohudmon

import android.view.Choreographer

class FpsMonitor {
    private var lastFrameTimeNs: Long = 0L
    private var currentFps: Float = 0f
    private var isRunning = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            if (lastFrameTimeNs != 0L) {
                val deltaMs = (frameTimeNanos - lastFrameTimeNs) / 1_000_000f
                if (deltaMs > 0) currentFps = 1000f / deltaMs
            }
            lastFrameTimeNs = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        isRunning = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun getFps(): Float = currentFps
}
