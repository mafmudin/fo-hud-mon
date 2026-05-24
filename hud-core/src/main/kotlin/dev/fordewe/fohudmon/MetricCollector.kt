package dev.fordewe.fohudmon

import android.content.Context
import android.os.StatFs

internal data class Metrics(
    val cpuAppPercent: Float    = 0f,
    val cpuSysPercent: Float    = 0f,
    val ramRssKb: Long          = 0L,
    val ramTotalKb: Long        = 0L,
    val threadCount: Int        = 0,
    val netRxBps: Long          = 0L,
    val netTxBps: Long          = 0L,
    val storageFreeBytes: Long  = 0L,
    val storageTotalBytes: Long = 0L,
    val fps: Float              = 0f,
)

internal class MetricCollector(
    private val context: Context,
    private val intervalMs: Long = 500L,
) {
    private external fun nativeInit()
    private external fun nativeGetMemInfo(): LongArray
    private external fun nativeGetCpuInfo(): IntArray
    private external fun nativeGetNetInfo(intervalMs: Long): LongArray

    companion object {
        init { System.loadLibrary("devhud") }
    }

    fun init() = nativeInit()

    fun collect(): Metrics {
        val mem     = nativeGetMemInfo()
        val cpu     = nativeGetCpuInfo()
        val net     = nativeGetNetInfo(intervalMs)
        val storage = getStorageInfo()

        return Metrics(
            cpuAppPercent     = cpu[0] / 100f,
            cpuSysPercent     = cpu[1] / 100f,
            ramRssKb          = mem[0],
            ramTotalKb        = mem[1],
            threadCount       = mem[3].toInt(),
            netRxBps          = net[0],
            netTxBps          = net[1],
            storageFreeBytes  = storage.first,
            storageTotalBytes = storage.second,
        )
    }

    private fun getStorageInfo(): Pair<Long, Long> {
        return try {
            val stat  = StatFs(context.filesDir.path)
            val free  = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            Pair(free, total)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
}
