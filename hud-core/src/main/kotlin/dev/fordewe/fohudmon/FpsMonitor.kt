package dev.fordewe.fohudmon

import android.view.Choreographer

internal class FpsMonitor {
    private var frameCount = 0
    private var windowStartNs = 0L
    private var currentFps: Float = 0f
    private var isRunning = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            if (windowStartNs == 0L) {
                windowStartNs = frameTimeNanos
            }

            frameCount++
            val elapsedMs = (frameTimeNanos - windowStartNs) / 1_000_000L
            if (elapsedMs >= WINDOW_MS) {
                currentFps = frameCount * 1000f / elapsedMs
                frameCount = 0
                windowStartNs = frameTimeNanos
            }

            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        isRunning = true
        frameCount = 0
        windowStartNs = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun getFps(): Float = currentFps

    private companion object {
        const val WINDOW_MS = 1000L
    }
}
