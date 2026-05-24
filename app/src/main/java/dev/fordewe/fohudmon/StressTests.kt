package dev.fordewe.fohudmon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sqrt
import kotlin.random.Random

class StressTests {

    // ── CPU Single ────────────────────────────────────────────────────────────

    private var cpuSingleJob: Job? = null
    val isCpuSingleRunning get() = cpuSingleJob?.isActive == true

    fun startCpuSingle() {
        cpuSingleJob = CoroutineScope(Dispatchers.Default).launch {
            var n = 2L
            while (isActive) isPrime(n++)
        }
    }

    fun stopCpuSingle() { cpuSingleJob?.cancel() }

    // ── CPU Multi ─────────────────────────────────────────────────────────────

    private val cpuMultiJobs = mutableListOf<Job>()
    val isCpuMultiRunning get() = cpuMultiJobs.any { it.isActive }

    fun startCpuMulti() {
        val cores = Runtime.getRuntime().availableProcessors()
        repeat(cores) {
            cpuMultiJobs += CoroutineScope(Dispatchers.Default).launch {
                var n = 2L
                while (isActive) isPrime(n++)
            }
        }
    }

    fun stopCpuMulti() {
        cpuMultiJobs.forEach { it.cancel() }
        cpuMultiJobs.clear()
    }

    // ── RAM Alloc ─────────────────────────────────────────────────────────────

    private var ramAllocJob: Job? = null
    private val ramAllocChunks = mutableListOf<ByteArray>()
    val isRamAllocRunning get() = ramAllocJob?.isActive == true

    // onOom: called on main thread when allocation hits OOM (so UI can reset to Idle)
    fun startRamAlloc(onOom: (() -> Unit)? = null) {
        ramAllocJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                while (isActive) {
                    try {
                        // fill(0x42) forces physical page allocation — without this, ART's
                        // lazy mmap + Android KSM zero-page dedup keeps RSS unchanged.
                        val chunk = ByteArray(50 * 1024 * 1024).also { it.fill(0x42) }
                        // isActive re-checked: cancel() may have fired while fill() was running
                        if (isActive) synchronized(ramAllocChunks) { ramAllocChunks += chunk }
                    } catch (_: OutOfMemoryError) {
                        onOom?.let { Handler(Looper.getMainLooper()).post(it) }
                        break
                    }
                    delay(2000L)
                }
            } finally {
                // finally guarantees clear() runs after any in-progress fill() completes
                synchronized(ramAllocChunks) { ramAllocChunks.clear() }
                // Hint to ART to run GC immediately — without this, freed ByteArrays sit in
                // heap until next allocation triggers GC, so RSS stays elevated after stop.
                System.gc()
            }
        }
    }

    fun stopRamAlloc() {
        ramAllocJob?.cancel()
        // clear() is in the coroutine's finally block — runs after fill() finishes
    }

    // ── RAM Leak ──────────────────────────────────────────────────────────────

    private var ramLeakJob: Job? = null
    private val leakChunks = mutableListOf<ByteArray>()
    val isRamLeakRunning get() = ramLeakJob?.isActive == true

    fun startRamLeak(onOom: (() -> Unit)? = null) {
        ramLeakJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                while (isActive) {
                    try {
                        val chunk = ByteArray(10 * 1024 * 1024).also { it.fill(0x42) }
                        if (isActive) synchronized(leakChunks) { leakChunks += chunk }
                    } catch (_: OutOfMemoryError) {
                        onOom?.let { Handler(Looper.getMainLooper()).post(it) }
                        break
                    }
                    delay(500L)
                }
            } finally {
                synchronized(leakChunks) { leakChunks.clear() }
                System.gc()
            }
        }
    }

    fun stopRamLeak() {
        ramLeakJob?.cancel()
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private var networkJob: Job? = null
    val isNetworkRunning get() = networkJob?.isActive == true

    fun startNetwork() {
        networkJob = CoroutineScope(Dispatchers.IO).launch {
            val buf = ByteArray(8192)
            while (isActive) {
                try {
                    val conn = URL("https://speed.cloudflare.com/__down?bytes=524288")
                        .openConnection() as HttpURLConnection
                    conn.connectTimeout = 5_000
                    conn.readTimeout    = 10_000
                    conn.inputStream.use { stream ->
                        while (isActive && stream.read(buf) != -1) Unit
                    }
                    conn.disconnect()
                } catch (_: Exception) {
                    delay(1000L)
                }
            }
        }
    }

    fun stopNetwork() { networkJob?.cancel() }

    // ── Stop All ──────────────────────────────────────────────────────────────

    fun stopAll() {
        stopCpuSingle()
        stopCpuMulti()
        stopRamAlloc()
        stopRamLeak()
        stopNetwork()
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun isPrime(n: Long): Boolean {
        if (n < 2L) return false
        val limit = sqrt(n.toDouble()).toLong()
        for (i in 2L..limit) if (n % i == 0L) return false
        return true
    }
}

/**
 * Full-screen view that draws 600 random cubic bezier curves every frame.
 * Added/removed from the window's decor view by MainActivity to simulate
 * heavy rendering and cause measurable FPS drop on the HUD.
 */
class HeavyDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var active = false
        set(value) { field = value; if (value) invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 1.5f
        color       = Color.argb(120, 30, 144, 255)
    }
    private val path = Path()
    private val rng  = Random.Default

    override fun onDraw(canvas: Canvas) {
        if (!active) return
        val w = width.toFloat().takeIf  { it > 0f } ?: return
        val h = height.toFloat().takeIf { it > 0f } ?: return

        path.rewind()
        repeat(600) {
            path.moveTo(rng.nextFloat() * w, rng.nextFloat() * h)
            path.cubicTo(
                rng.nextFloat() * w, rng.nextFloat() * h,
                rng.nextFloat() * w, rng.nextFloat() * h,
                rng.nextFloat() * w, rng.nextFloat() * h,
            )
        }
        canvas.drawPath(path, paint)
        invalidate()
    }
}
