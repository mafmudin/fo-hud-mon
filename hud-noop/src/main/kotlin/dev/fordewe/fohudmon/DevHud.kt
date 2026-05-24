package dev.fordewe.fohudmon

import android.app.Application
import java.io.File

object DevHud {
    fun install(app: Application, config: HudConfig = HudConfig()) = Unit
    fun install(app: Application, block: HudConfig.() -> HudConfig) = Unit
    fun dismiss() = Unit
    fun dumpLog(): String = ""
    fun exportLog(): File? = null
    fun clearLog() = Unit
    val logSize: Int get() = 0
}
