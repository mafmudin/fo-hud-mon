package dev.fordewe.fohudmon

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class MetricLogger(private val capacity: Int) {

    private val records = ArrayDeque<MetricRecord>(capacity)

    @Synchronized
    fun append(metrics: Metrics) {
        if (records.size >= capacity) records.removeFirst()
        records.addLast(MetricRecord(System.currentTimeMillis(), metrics))
    }

    @Synchronized
    fun toCsv(): String {
        if (records.isEmpty()) return ""
        val origin = records.first().timestampMs
        val sb = StringBuilder()
        sb.appendLine("elapsed_ms,timestamp_ms,fps,cpu_app_%,cpu_sys_%,ram_rss_mb,ram_total_mb,threads,net_rx_bps,net_tx_bps,storage_free_mb,storage_total_mb")
        records.forEach { r ->
            val m = r.metrics
            sb.appendLine(
                "${r.timestampMs - origin},${r.timestampMs}," +
                "%.1f,%.1f,%.1f,".format(m.fps, m.cpuAppPercent, m.cpuSysPercent) +
                "${m.ramRssKb / 1024},${m.ramTotalKb / 1024}," +
                "${m.threadCount}," +
                "${m.netRxBps},${m.netTxBps}," +
                "${m.storageFreeBytes / (1024 * 1024)},${m.storageTotalBytes / (1024 * 1024)}"
            )
        }
        return sb.toString()
    }

    @Synchronized
    fun exportToFile(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "devhud_$ts.csv")
        file.writeText(toCsv())
        return file
    }

    @Synchronized
    fun clear() = records.clear()

    val size: Int @Synchronized get() = records.size
}

internal data class MetricRecord(
    val timestampMs: Long,
    val metrics: Metrics,
)
