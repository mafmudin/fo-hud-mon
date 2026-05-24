package dev.fordewe.fohudmon

import android.app.Application
import java.io.File

object DevHud {

    private var overlay: HudOverlay? = null
    val isInstalled: Boolean get() = overlay != null

    fun install(app: Application, config: HudConfig = HudConfig()) {
        if (isInstalled) return
        val newOverlay = HudOverlay(app, config)
        if (newOverlay.show()) overlay = newOverlay
    }

    fun install(app: Application, block: HudConfig.() -> HudConfig) {
        install(app, HudConfig().block())
    }

    fun dismiss() {
        overlay?.hide()
        overlay = null
    }

    /** Returns all captured metrics as a CSV string. Empty string if not installed or no data. */
    fun dumpLog(): String = overlay?.logger?.toCsv() ?: ""

    /**
     * Writes captured metrics to a CSV file in the app's external files directory.
     * Returns the file, or null if the HUD is not installed.
     * Pull with: adb pull <path>
     */
    fun exportLog(): File? = overlay?.exportLog()

    /** Clears the in-memory metric log. */
    fun clearLog() { overlay?.logger?.clear() }

    /** Number of metric samples currently held in the log. */
    val logSize: Int get() = overlay?.logger?.size ?: 0
}
