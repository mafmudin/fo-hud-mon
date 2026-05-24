package dev.fordewe.fohudmon

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class HudOverlay(
    private val context: Context,
    private val config: HudConfig,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val collector     = MetricCollector(context, config.updateIntervalMs)
    private val fpsMonitor    = FpsMonitor()
    private lateinit var hudView: HudView
    private var pollingJob: Job? = null

    // Returns false if SYSTEM_ALERT_WINDOW permission is not granted.
    // Caller (Activity) is responsible for redirecting to permission settings.
    fun show(): Boolean {
        if (!Settings.canDrawOverlays(context)) return false

        collector.init()
        fpsMonitor.start()

        hudView = HudView(context, config)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = config.position.toGravity()
        }

        windowManager.addView(hudView, params)
        startPolling()
        return true
    }

    fun hide() {
        pollingJob?.cancel()
        fpsMonitor.stop()
        if (::hudView.isInitialized) windowManager.removeView(hudView)
    }

    private fun startPolling() {
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val metrics = collector.collect().copy(fps = fpsMonitor.getFps())
                withContext(Dispatchers.Main) {
                    if (::hudView.isInitialized) hudView.update(metrics)
                }
                delay(config.updateIntervalMs)
            }
        }
    }
}

private fun HudPosition.toGravity() = when (this) {
    HudPosition.TOP_START    -> Gravity.TOP or Gravity.START
    HudPosition.TOP_END      -> Gravity.TOP or Gravity.END
    HudPosition.BOTTOM_START -> Gravity.BOTTOM or Gravity.START
    HudPosition.BOTTOM_END   -> Gravity.BOTTOM or Gravity.END
}
