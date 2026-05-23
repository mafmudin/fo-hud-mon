package dev.fordewe.fohudmon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

class HudView(context: Context, private val config: HudConfig) : View(context) {

    private val rows = mutableListOf<Pair<String, String>>()

    private val bgPaint = Paint().apply {
        color = Color.argb((config.opacity * 255).toInt(), 0, 0, 0)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 28f
        typeface = Typeface.MONOSPACE
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val padding    = 24f
    private val lineHeight = 38f

    fun update(metrics: Metrics) {
        rows.clear()
        if (config.showFps)     rows += "FPS" to "%.0f".format(metrics.fps)
        if (config.showCpu)     rows += "CPU" to "%.1f%%".format(metrics.cpuAppPercent)
        if (config.showMemory)  rows += "RAM" to "${metrics.ramRssKb / 1024} MB"
        if (config.showNetwork) rows += " ↓ " to formatBytes(metrics.netRxBps) + "/s"
        if (config.showNetwork) rows += " ↑ " to formatBytes(metrics.netTxBps) + "/s"
        if (config.showStorage) rows += "DSK" to formatBytes(metrics.storageFreeBytes) + " free"
        if (config.showThreads) rows += "THR" to "${metrics.threadCount}"
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = 260f
        val h = padding * 2 + rows.size * lineHeight
        setMeasuredDimension(w.toInt(), maxOf(h.toInt(), 1))
    }

    override fun onDraw(canvas: Canvas) {
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, 12f, 12f, bgPaint)

        rows.forEachIndexed { i, (label, value) ->
            val y = padding + (i + 1) * lineHeight
            canvas.drawText(label, padding, y, labelPaint)
            canvas.drawText(value, 120f, y, valuePaint)
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000f)
        bytes >= 1_000     -> "%.0f KB".format(bytes / 1_000f)
        else               -> "$bytes B"
    }
}
