package dev.fordewe.fohudmon

import android.app.Application

object DevHud {
    fun install(app: Application, config: HudConfig = HudConfig()) = Unit
    fun install(app: Application, block: HudConfig.() -> HudConfig) = Unit
    fun dismiss() = Unit
}
