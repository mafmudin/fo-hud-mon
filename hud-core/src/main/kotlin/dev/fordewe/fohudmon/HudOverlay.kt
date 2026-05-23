package dev.fordewe.fohudmon

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
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

class HudOverlay(
    private val context: Context,
    private val config: HudConfig,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val collector     = MetricCollector(context, config.updateIntervalMs)
    private val fpsMonitor    = FpsMonitor()
    private lateinit var hudView: HudView
    private var pollingJob: Job? = null

    fun show() {
        if (!Settings.canDrawOverlays(context)) {
            redirectToPermissionSettings()
            return
        }

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

    private fun redirectToPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun HudPosition.toGravity() = when (this) {
    HudPosition.TOP_START    -> Gravity.TOP or Gravity.START
    HudPosition.TOP_END      -> Gravity.TOP or Gravity.END
    HudPosition.BOTTOM_START -> Gravity.BOTTOM or Gravity.START
    HudPosition.BOTTOM_END   -> Gravity.BOTTOM or Gravity.END
}
