package dev.fordewe.fohudmon

import android.app.Application

object DevHud {

    private var overlay: HudOverlay? = null

    fun install(app: Application, config: HudConfig = HudConfig()) {
        overlay = HudOverlay(app, config).also { it.show() }
    }

    fun install(app: Application, block: HudConfig.() -> HudConfig) {
        install(app, HudConfig().block())
    }

    fun dismiss() = overlay?.hide()
}
