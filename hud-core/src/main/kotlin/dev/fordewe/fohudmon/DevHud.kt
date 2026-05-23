package dev.fordewe.fohudmon

import android.app.Application

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
}
