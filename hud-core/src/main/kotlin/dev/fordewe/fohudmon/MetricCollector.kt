package dev.fordewe.fohudmon

import android.content.Context
import android.net.TrafficStats
import android.os.Process
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

    companion object {
        init { System.loadLibrary("devhud") }
    }

    private val uid = Process.myUid()
    private var prevRxBytes   = 0L
    private var prevTxBytes   = 0L
    private var prevNetTimeMs = 0L

    fun init() {
        nativeInit()
        prevRxBytes   = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0L)
        prevTxBytes   = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0L)
        prevNetTimeMs = System.currentTimeMillis()
    }

    fun collect(): Metrics {
        val mem     = nativeGetMemInfo()
        val cpu     = nativeGetCpuInfo()
        val storage = getStorageInfo()
        val net     = getNetInfo()

        return Metrics(
            cpuAppPercent     = cpu[0] / 100f,
            cpuSysPercent     = cpu[1] / 100f,
            ramRssKb          = mem[0],
            ramTotalKb        = mem[1],
            threadCount       = mem[3].toInt(),
            netRxBps          = net.first,
            netTxBps          = net.second,
            storageFreeBytes  = storage.first,
            storageTotalBytes = storage.second,
        )
    }

    // TrafficStats replaces /proc/net/dev which is blocked on Android 10+.
    // getUidRxBytes gives per-app traffic, no permission required.
    private fun getNetInfo(): Pair<Long, Long> {
        val curRx    = TrafficStats.getUidRxBytes(uid)
        val curTx    = TrafficStats.getUidTxBytes(uid)
        val nowMs    = System.currentTimeMillis()
        val elapsed  = nowMs - prevNetTimeMs

        if (curRx == TrafficStats.UNSUPPORTED.toLong() || elapsed <= 0L) {
            return Pair(0L, 0L)
        }

        val rxBps = ((curRx - prevRxBytes) * 1000L) / elapsed
        val txBps = ((curTx - prevTxBytes) * 1000L) / elapsed

        prevRxBytes   = curRx
        prevTxBytes   = curTx
        prevNetTimeMs = nowMs

        return Pair(rxBps.coerceAtLeast(0L), txBps.coerceAtLeast(0L))
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
