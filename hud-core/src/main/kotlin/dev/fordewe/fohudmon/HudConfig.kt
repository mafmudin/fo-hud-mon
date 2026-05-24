package dev.fordewe.fohudmon

data class HudConfig(
    val showCpu: Boolean       = true,
    val showMemory: Boolean    = true,
    val showNetwork: Boolean   = true,
    val showStorage: Boolean   = true,
    val showFps: Boolean       = true,
    val showThreads: Boolean   = false,
    val updateIntervalMs: Long = 500L,
    val position: HudPosition  = HudPosition.TOP_END,
    val opacity: Float         = 0.85f,
    val logCapacity: Int       = 1200,
) {
    init {
        require(logCapacity > 0) { "logCapacity must be > 0, got $logCapacity" }
    }
}

enum class HudPosition {
    TOP_START, TOP_END, BOTTOM_START, BOTTOM_END
}
