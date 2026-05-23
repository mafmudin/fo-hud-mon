# DevHUD — Developer Resource Monitor Overlay
> Real-time CPU, RAM, Network, Storage & FPS overlay for Android development

---

## 1. Requirements

### 1.1 Development Environment

| Requirement | Minimum | Recommended |
|---|---|---|
| Android Studio | Hedgehog (2023.1.1) | Meerkat (2024.3.1)+ |
| NDK Version | 25.x | 27.x (LTS) |
| CMake | 3.22.1 | 3.28+ |
| Kotlin | 1.9.x | 2.0+ |
| AGP (Android Gradle Plugin) | 8.0 | 8.4+ |
| JDK | 17 | 17 |

### 1.2 Install NDK & CMake

Android Studio → **SDK Manager** → **SDK Tools** tab → centang:
- ✅ NDK (Side by side) — versi 27.x
- ✅ CMake — versi 3.28+

Atau via `local.properties` / `build.gradle.kts`:
```kotlin
android {
    ndkVersion = "27.0.12077973"
}
```

### 1.3 Target Device / Emulator

| Requirement | Keterangan |
|---|---|
| Min SDK | API 26 (Android 8.0) — untuk `TYPE_APPLICATION_OVERLAY` |
| Target SDK | API 35 |
| Permission | `SYSTEM_ALERT_WINDOW` — wajib di-grant manual saat pertama run |
| `/proc` filesystem | Tersedia di semua device Android (tidak perlu root) |

> ⚠️ **Emulator vs Physical Device**: `/proc/net` untuk network monitoring lebih akurat di physical device. Emulator bisa dipakai untuk CPU & RAM monitoring.

### 1.4 Permission yang Dibutuhkan

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Untuk network state -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Untuk foreground service (opsional, untuk background monitoring) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

> `SYSTEM_ALERT_WINDOW` **tidak bisa di-request via `requestPermissions()`** — harus redirect user ke Settings. Library akan handle ini otomatis.

### 1.5 Knowledge Prerequisites

Tidak wajib expert, tapi familiar dengan:
- **Kotlin Coroutines** — untuk polling metric secara async
- **JNI dasar** — library akan generate boilerplate, tapi perlu paham `external fun`
- **CMakeLists.txt** — hanya untuk konfigurasi, tidak perlu expert

### 1.6 Gradle Setup (Consumer App)

```kotlin
// settings.gradle.kts — pastikan ada JitPack (untuk fase distribusi)
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // untuk distribusi via JitPack nanti
    }
}
```

---

## 2. Arsitektur

```
┌─────────────────────────────────────────────────┐
│                  Consumer App                    │
│         (cukup tambah 1 dependency)              │
└────────────────────┬────────────────────────────┘
                     │ debugImplementation
          ┌──────────┴──────────┐
          │                     │
   ┌──────▼──────┐       ┌──────▼──────┐
   │  hud-core   │       │  hud-noop   │
   │ (debug only)│       │(release only│
   └──────┬──────┘       └─────────────┘
          │
   ┌──────▼──────────────────────────┐
   │         hud-core internals       │
   │                                  │
   │  ┌─────────────┐  ┌───────────┐ │
   │  │ HudOverlay  │  │ HudService│ │
   │  │  (Kotlin)   │  │ (Kotlin)  │ │
   │  └──────┬──────┘  └─────┬─────┘ │
   │         └──────┬─────────┘       │
   │          ┌─────▼──────┐          │
   │          │MetricCollect│          │
   │          │  (Kotlin)  │          │
   │          └─────┬──────┘          │
   │          ┌─────▼──────┐          │
   │          │  JNI Bridge │          │
   │          └─────┬──────┘          │
   │          ┌─────▼──────┐          │
   │          │  C++ Core  │          │
   │          │ /proc reader│          │
   │          └────────────┘          │
   └──────────────────────────────────┘
```

### 2.1 Module Structure

```
devhud/
├── hud-core/
│   ├── src/main/
│   │   ├── cpp/
│   │   │   ├── CMakeLists.txt
│   │   │   ├── hud_jni.cpp           ← JNI entry point
│   │   │   ├── proc_reader.cpp       ← /proc parser utilities
│   │   │   ├── cpu_monitor.cpp       ← CPU usage calculation
│   │   │   ├── mem_monitor.cpp       ← RAM / heap info
│   │   │   ├── net_monitor.cpp       ← Network RX/TX delta
│   │   │   └── include/
│   │   │       ├── proc_reader.h
│   │   │       ├── cpu_monitor.h
│   │   │       ├── mem_monitor.h
│   │   │       └── net_monitor.h
│   │   │
│   │   └── kotlin/com/devhud/
│   │       ├── DevHud.kt             ← Public API entry point
│   │       ├── HudConfig.kt          ← DSL config
│   │       ├── HudOverlay.kt         ← WindowManager overlay
│   │       ├── HudView.kt            ← Custom View (UI)
│   │       ├── HudService.kt         ← Foreground service
│   │       ├── MetricCollector.kt    ← Koordinator JNI + Kotlin
│   │       ├── FpsMonitor.kt         ← Choreographer-based FPS
│   │       ├── StorageMonitor.kt     ← App storage usage
│   │       └── internal/
│   │           └── HudInitProvider.kt ← Auto-init ContentProvider
│   │
│   └── build.gradle.kts
│
├── hud-noop/
│   └── src/main/kotlin/com/devhud/
│       ├── DevHud.kt                 ← Semua method kosong
│       └── HudConfig.kt
│
└── sample-app/
    ├── src/main/
    │   └── ...
    └── build.gradle.kts
```

---

## 3. C++ Core Layer

### 3.1 CMakeLists.txt

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("devhud")

add_library(
    devhud
    SHARED
    hud_jni.cpp
    proc_reader.cpp
    cpu_monitor.cpp
    mem_monitor.cpp
    net_monitor.cpp
)

target_include_directories(devhud PRIVATE include/)

find_library(log-lib log)

target_link_libraries(devhud ${log-lib})
```

### 3.2 proc_reader.h / proc_reader.cpp

```cpp
// include/proc_reader.h
#pragma once
#include <string>
#include <map>

namespace devhud {
    // Baca seluruh isi file dari /proc sebagai string
    std::string readProcFile(const std::string& path);

    // Parse key-value dari /proc/self/status
    // contoh output: {"VmRSS": "14520", "Threads": "12", ...}
    std::map<std::string, std::string> parseProcStatus();
}
```

```cpp
// proc_reader.cpp
#include "proc_reader.h"
#include <fstream>
#include <sstream>

namespace devhud {

std::string readProcFile(const std::string& path) {
    std::ifstream file(path);
    if (!file.is_open()) return "";
    std::stringstream buffer;
    buffer << file.rdbuf();
    return buffer.str();
}

std::map<std::string, std::string> parseProcStatus() {
    std::map<std::string, std::string> result;
    std::ifstream file("/proc/self/status");
    std::string line;

    while (std::getline(file, line)) {
        size_t colonPos = line.find(':');
        if (colonPos == std::string::npos) continue;

        std::string key = line.substr(0, colonPos);
        std::string value = line.substr(colonPos + 1);

        // Trim whitespace
        size_t start = value.find_first_not_of(" \t");
        if (start != std::string::npos)
            value = value.substr(start);

        result[key] = value;
    }
    return result;
}

} // namespace devhud
```

### 3.3 mem_monitor.h / mem_monitor.cpp

```cpp
// include/mem_monitor.h
#pragma once

namespace devhud {
    struct MemInfo {
        long vmRssKb;     // Physical RAM dipakai proses (KB)
        long vmSizeKb;    // Virtual memory total (KB)
        long vmPeakKb;    // Peak virtual memory (KB)
        int  threadCount; // Jumlah thread aktif
    };

    MemInfo getMemInfo();
}
```

```cpp
// mem_monitor.cpp
#include "mem_monitor.h"
#include "proc_reader.h"
#include <string>

namespace devhud {

MemInfo getMemInfo() {
    MemInfo info = {0, 0, 0, 0};
    auto status = parseProcStatus();

    auto parseKb = [&](const std::string& key) -> long {
        auto it = status.find(key);
        if (it == status.end()) return 0L;
        // value format: "14520 kB" → ambil angkanya saja
        return std::stol(it->second);
    };

    info.vmRssKb     = parseKb("VmRSS");
    info.vmSizeKb    = parseKb("VmSize");
    info.vmPeakKb    = parseKb("VmPeak");

    auto threadIt = status.find("Threads");
    if (threadIt != status.end())
        info.threadCount = std::stoi(threadIt->second);

    return info;
}

} // namespace devhud
```

### 3.4 cpu_monitor.h / cpu_monitor.cpp

```cpp
// include/cpu_monitor.h
#pragma once

namespace devhud {
    struct CpuInfo {
        float usagePercent;  // CPU usage proses ini (0.0 - 100.0)
        float systemPercent; // CPU usage system-wide (0.0 - 100.0)
    };

    // Harus dipanggil 2x dengan interval → hitung delta
    // Panggil initCpuSampling() saat init, getCpuInfo() tiap polling
    void  initCpuSampling();
    CpuInfo getCpuInfo();
}
```

```cpp
// cpu_monitor.cpp
#include "cpu_monitor.h"
#include "proc_reader.h"
#include <fstream>
#include <sstream>
#include <cstring>

namespace devhud {

// --- State untuk delta calculation ---
static long prevProcUtime = 0, prevProcStime = 0;
static long prevTotalCpu  = 0, prevIdleCpu   = 0;

static long readProcCpuTicks() {
    // /proc/self/stat field ke-14 (utime) dan ke-15 (stime)
    std::string stat = readProcFile("/proc/self/stat");
    if (stat.empty()) return 0;

    // Field dipisah spasi; utime ada di index 13, stime di 14
    std::istringstream ss(stat);
    std::string token;
    long utime = 0, stime = 0;
    for (int i = 0; i < 15; i++) {
        ss >> token;
        if (i == 13) utime = std::stol(token);
        if (i == 14) stime = std::stol(token);
    }
    return utime + stime;
}

struct SystemCpuTicks { long total; long idle; };

static SystemCpuTicks readSystemCpuTicks() {
    std::ifstream file("/proc/stat");
    std::string line;
    std::getline(file, line); // baris pertama = "cpu  ..."

    long user, nice, system, idle, iowait, irq, softirq;
    sscanf(line.c_str(), "cpu %ld %ld %ld %ld %ld %ld %ld",
           &user, &nice, &system, &idle, &iowait, &irq, &softirq);

    long total = user + nice + system + idle + iowait + irq + softirq;
    return {total, idle};
}

void initCpuSampling() {
    prevProcUtime = readProcCpuTicks();
    auto sys = readSystemCpuTicks();
    prevTotalCpu = sys.total;
    prevIdleCpu  = sys.idle;
}

CpuInfo getCpuInfo() {
    long curProcTicks = readProcCpuTicks();
    auto curSys       = readSystemCpuTicks();

    long deltaProcTicks = curProcTicks - prevProcUtime;
    long deltaTotalCpu  = curSys.total - prevTotalCpu;
    long deltaIdleCpu   = curSys.idle  - prevIdleCpu;

    CpuInfo info = {0.0f, 0.0f};

    if (deltaTotalCpu > 0) {
        info.systemPercent = 100.0f * (1.0f - (float)deltaIdleCpu / deltaTotalCpu);
        info.usagePercent  = 100.0f * (float)deltaProcTicks / deltaTotalCpu;
    }

    // Update previous state
    prevProcUtime = curProcTicks;
    prevTotalCpu  = curSys.total;
    prevIdleCpu   = curSys.idle;

    return info;
}

} // namespace devhud
```

### 3.5 net_monitor.h / net_monitor.cpp

```cpp
// include/net_monitor.h
#pragma once

namespace devhud {
    struct NetInfo {
        long rxBytesPerSec; // Download bytes/detik
        long txBytesPerSec; // Upload bytes/detik
    };

    void    initNetSampling();
    NetInfo getNetInfo(long intervalMs);
}
```

```cpp
// net_monitor.cpp
#include "net_monitor.h"
#include "proc_reader.h"
#include <fstream>
#include <sstream>
#include <string>

namespace devhud {

static long prevRxBytes = 0;
static long prevTxBytes = 0;

// Baca total RX/TX dari /proc/net/dev
// Akumulasi semua interface kecuali "lo" (loopback)
static void readNetBytes(long& outRx, long& outTx) {
    outRx = 0; outTx = 0;
    std::ifstream file("/proc/net/dev");
    std::string line;

    // Skip 2 header lines
    std::getline(file, line);
    std::getline(file, line);

    while (std::getline(file, line)) {
        std::istringstream ss(line);
        std::string iface;
        ss >> iface;

        // Skip loopback
        if (iface == "lo:") continue;

        long rx, tx;
        long dummy;
        // Format: iface: rx_bytes packets errs drop ... tx_bytes ...
        ss >> rx >> dummy >> dummy >> dummy >> dummy >> dummy >> dummy >> dummy >> tx;
        outRx += rx;
        outTx += tx;
    }
}

void initNetSampling() {
    readNetBytes(prevRxBytes, prevTxBytes);
}

NetInfo getNetInfo(long intervalMs) {
    long curRx, curTx;
    readNetBytes(curRx, curTx);

    float seconds = intervalMs / 1000.0f;
    NetInfo info = {
        (long)((curRx - prevRxBytes) / seconds),
        (long)((curTx - prevTxBytes) / seconds)
    };

    prevRxBytes = curRx;
    prevTxBytes = curTx;

    return info;
}

} // namespace devhud
```

### 3.6 hud_jni.cpp — JNI Bridge

```cpp
// hud_jni.cpp
#include <jni.h>
#include "cpu_monitor.h"
#include "mem_monitor.h"
#include "net_monitor.h"

extern "C" {

// ─── Init ───────────────────────────────────────────────────────────────────

JNIEXPORT void JNICALL
Java_com_devhud_MetricCollector_nativeInit(JNIEnv*, jobject) {
    devhud::initCpuSampling();
    devhud::initNetSampling();
}

// ─── Memory ─────────────────────────────────────────────────────────────────
// Returns: [vmRssKb, vmSizeKb, vmPeakKb, threadCount]

JNIEXPORT jlongArray JNICALL
Java_com_devhud_MetricCollector_nativeGetMemInfo(JNIEnv* env, jobject) {
    devhud::MemInfo info = devhud::getMemInfo();
    jlong values[] = {info.vmRssKb, info.vmSizeKb, info.vmPeakKb, info.threadCount};
    jlongArray result = env->NewLongArray(4);
    env->SetLongArrayRegion(result, 0, 4, values);
    return result;
}

// ─── CPU ────────────────────────────────────────────────────────────────────
// Returns: [usagePercent * 100, systemPercent * 100] as int (avoid float JNI)

JNIEXPORT jintArray JNICALL
Java_com_devhud_MetricCollector_nativeGetCpuInfo(JNIEnv* env, jobject) {
    devhud::CpuInfo info = devhud::getCpuInfo();
    jint values[] = {
        (jint)(info.usagePercent * 100),
        (jint)(info.systemPercent * 100)
    };
    jintArray result = env->NewIntArray(2);
    env->SetIntArrayRegion(result, 0, 2, values);
    return result;
}

// ─── Network ─────────────────────────────────────────────────────────────────
// Returns: [rxBytesPerSec, txBytesPerSec]

JNIEXPORT jlongArray JNICALL
Java_com_devhud_MetricCollector_nativeGetNetInfo(JNIEnv* env, jobject, jlong intervalMs) {
    devhud::NetInfo info = devhud::getNetInfo(intervalMs);
    jlong values[] = {info.rxBytesPerSec, info.txBytesPerSec};
    jlongArray result = env->NewLongArray(2);
    env->SetLongArrayRegion(result, 0, 2, values);
    return result;
}

} // extern "C"
```

---

## 4. Kotlin Layer

### 4.1 HudConfig.kt

```kotlin
// HudConfig.kt
package com.devhud

data class HudConfig(
    val showCpu: Boolean      = true,
    val showMemory: Boolean   = true,
    val showNetwork: Boolean  = true,
    val showStorage: Boolean  = true,
    val showFps: Boolean      = true,
    val showThreads: Boolean  = false,
    val updateIntervalMs: Long = 500L,
    val position: HudPosition  = HudPosition.TOP_END,
    val opacity: Float         = 0.85f
)

enum class HudPosition {
    TOP_START, TOP_END, BOTTOM_START, BOTTOM_END
}
```

### 4.2 MetricCollector.kt

```kotlin
// MetricCollector.kt
package com.devhud

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs

data class Metrics(
    val cpuAppPercent: Float    = 0f,  // CPU usage app ini
    val cpuSysPercent: Float    = 0f,  // CPU usage system
    val ramRssKb: Long          = 0L,  // RAM physical (RSS)
    val ramTotalKb: Long        = 0L,  // Virtual memory
    val threadCount: Int        = 0,
    val netRxBps: Long          = 0L,  // Download bytes/sec
    val netTxBps: Long          = 0L,  // Upload bytes/sec
    val storageFreeBytes: Long  = 0L,  // Storage tersedia
    val storageTotalBytes: Long = 0L,  // Storage total
    val fps: Float              = 0f
)

class MetricCollector(
    private val context: Context,
    private val intervalMs: Long = 500L
) {

    // ─── JNI declarations ───────────────────────────────────────────────────
    private external fun nativeInit()
    private external fun nativeGetMemInfo(): LongArray   // [rss, size, peak, threads]
    private external fun nativeGetCpuInfo(): IntArray    // [appCpu*100, sysCpu*100]
    private external fun nativeGetNetInfo(intervalMs: Long): LongArray // [rx, tx]

    companion object {
        init { System.loadLibrary("devhud") }
    }

    fun init() = nativeInit()

    fun collect(): Metrics {
        val mem = nativeGetMemInfo()
        val cpu = nativeGetCpuInfo()
        val net = nativeGetNetInfo(intervalMs)
        val storage = getStorageInfo()

        return Metrics(
            cpuAppPercent   = cpu[0] / 100f,
            cpuSysPercent   = cpu[1] / 100f,
            ramRssKb        = mem[0],
            ramTotalKb      = mem[1],
            threadCount     = mem[3].toInt(),
            netRxBps        = net[0],
            netTxBps        = net[1],
            storageFreeBytes  = storage.first,
            storageTotalBytes = storage.second,
            // FPS diisi dari FpsMonitor via callback terpisah
        )
    }

    private fun getStorageInfo(): Pair<Long, Long> {
        return try {
            val stat = StatFs(context.filesDir.path)
            val free  = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            Pair(free, total)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
}
```

### 4.3 FpsMonitor.kt

```kotlin
// FpsMonitor.kt
package com.devhud

import android.view.Choreographer

class FpsMonitor {
    private var lastFrameTimeNs: Long = 0L
    private var frameCount: Int = 0
    private var currentFps: Float = 0f
    private var isRunning = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            frameCount++
            if (lastFrameTimeNs != 0L) {
                val deltaMs = (frameTimeNanos - lastFrameTimeNs) / 1_000_000f
                if (deltaMs > 0) currentFps = 1000f / deltaMs
            }
            lastFrameTimeNs = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        isRunning = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun getFps(): Float = currentFps
}
```

### 4.4 DevHud.kt — Public API

```kotlin
// DevHud.kt
package com.devhud

import android.app.Application

object DevHud {

    private var overlay: HudOverlay? = null

    fun install(app: Application, config: HudConfig = HudConfig()) {
        overlay = HudOverlay(app, config).also { it.show() }
    }

    // DSL version
    fun install(app: Application, block: HudConfig.() -> HudConfig) {
        install(app, HudConfig().block())
    }

    fun dismiss() = overlay?.hide()
}
```

### 4.5 HudInitProvider.kt — Auto Init

```kotlin
// internal/HudInitProvider.kt
package com.devhud.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.devhud.DevHud
import com.devhud.HudConfig

internal class HudInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        // Dipanggil otomatis sebelum Application.onCreate()
        context?.let { ctx ->
            DevHud.install(ctx.applicationContext as android.app.Application)
        }
        return true
    }

    // Required overrides — tidak dipakai
    override fun query(uri: Uri, p: Array<String>?, s: String?, sA: Array<String>?, so: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, sA: Array<String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, sA: Array<String>?): Int = 0
}
```

```xml
<!-- AndroidManifest.xml di hud-core -->
<provider
    android:name=".internal.HudInitProvider"
    android:authorities="${applicationId}.devhud-init"
    android:exported="false" />
```

---

## 5. Overlay UI

### 5.1 HudOverlay.kt

```kotlin
// HudOverlay.kt
package com.devhud

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import kotlinx.coroutines.*

class HudOverlay(
    private val context: Context,
    private val config: HudConfig
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val collector = MetricCollector(context, config.updateIntervalMs)
    private val fpsMonitor = FpsMonitor()
    private lateinit var hudView: HudView
    private var pollingJob: Job? = null

    fun show() {
        if (!android.provider.Settings.canDrawOverlays(context)) {
            redirectToPermissionSettings()
            return
        }

        collector.init()
        fpsMonitor.start()

        hudView = HudView(context, config)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = config.position.toGravity()
        }

        windowManager.addView(hudView, params)
        startPolling()
    }

    private fun startPolling() {
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val metrics = collector.collect().copy(fps = fpsMonitor.getFps())
                withContext(Dispatchers.Main) {
                    if (::hudView.isInitialized) hudView.update(metrics)
                }
                delay(config.updateIntervalMs)
            }
        }
    }

    fun hide() {
        pollingJob?.cancel()
        fpsMonitor.stop()
        if (::hudView.isInitialized) windowManager.removeView(hudView)
    }

    private fun redirectToPermissionSettings() {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun HudPosition.toGravity() = when (this) {
    HudPosition.TOP_START    -> Gravity.TOP or Gravity.START
    HudPosition.TOP_END      -> Gravity.TOP or Gravity.END
    HudPosition.BOTTOM_START -> Gravity.BOTTOM or Gravity.START
    HudPosition.BOTTOM_END   -> Gravity.BOTTOM or Gravity.END
}
```

### 5.2 HudView.kt — UI Komponen

```kotlin
// HudView.kt
package com.devhud

import android.content.Context
import android.graphics.*
import android.view.View

class HudView(context: Context, private val config: HudConfig) : View(context) {

    private val rows = mutableListOf<Pair<String, String>>() // label → value

    private val bgPaint = Paint().apply {
        color = Color.argb((config.opacity * 255).toInt(), 0, 0, 0)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 28f
        typeface = Typeface.MONOSPACE
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val padding = 24f
    private val lineHeight = 38f

    fun update(metrics: Metrics) {
        rows.clear()
        if (config.showFps)     rows += "FPS" to "%.0f".format(metrics.fps)
        if (config.showCpu)     rows += "CPU" to "%.1f%%".format(metrics.cpuAppPercent)
        if (config.showMemory)  rows += "RAM" to "${metrics.ramRssKb / 1024} MB"
        if (config.showNetwork) rows += "↓" to formatBytes(metrics.netRxBps) + "/s"
        if (config.showNetwork) rows += "↑" to formatBytes(metrics.netTxBps) + "/s"
        if (config.showStorage) rows += "DSK" to formatBytes(metrics.storageFreeBytes) + " free"
        if (config.showThreads) rows += "THR" to "${metrics.threadCount}"
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = 260f
        val h = padding * 2 + rows.size * lineHeight
        setMeasuredDimension(w.toInt(), h.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        // Background
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, 12f, 12f, bgPaint)

        // Rows
        rows.forEachIndexed { i, (label, value) ->
            val y = padding + (i + 1) * lineHeight
            canvas.drawText(label, padding, y, labelPaint)
            canvas.drawText(value, 120f, y, valuePaint)
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000f)
        bytes >= 1_000     -> "%.0f KB".format(bytes / 1_000f)
        else               -> "$bytes B"
    }
}
```

---

## 6. hud-noop Module

```kotlin
// hud-noop/DevHud.kt
package com.devhud

import android.app.Application

object DevHud {
    fun install(app: Application, config: HudConfig = HudConfig()) { /* no-op */ }
    fun install(app: Application, block: HudConfig.() -> HudConfig) { /* no-op */ }
    fun dismiss() { /* no-op */ }
}
```

```kotlin
// hud-noop/HudConfig.kt
package com.devhud

data class HudConfig(
    val showCpu: Boolean = true,
    val showMemory: Boolean = true,
    val showNetwork: Boolean = true,
    val showStorage: Boolean = true,
    val showFps: Boolean = true,
    val showThreads: Boolean = false,
    val updateIntervalMs: Long = 500L,
    val position: HudPosition = HudPosition.TOP_END,
    val opacity: Float = 0.85f
)

enum class HudPosition { TOP_START, TOP_END, BOTTOM_START, BOTTOM_END }
```

---

## 7. Cara Pakai di Consumer App

### 7.1 build.gradle.kts

```kotlin
dependencies {
    debugImplementation(project(":hud-core"))
    releaseImplementation(project(":hud-noop"))
}
```

### 7.2 Zero Config (Auto-init)

```kotlin
// Tidak perlu menulis kode apapun.
// Overlay muncul otomatis saat debug build dijalankan.
```

### 7.3 Custom Config

```kotlin
// Application.kt
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DevHud.install(this, HudConfig(
            showFps         = true,
            showCpu         = true,
            showMemory      = true,
            showNetwork     = true,
            showStorage     = true,
            showThreads     = false,
            updateIntervalMs = 500L,
            position        = HudPosition.TOP_END,
            opacity         = 0.85f
        ))
    }
}
```

---

## 8. MVP Milestone & Roadmap

```
v0.1  ✅ Setup project structure + CMakeLists.txt
v0.2  ✅ C++ /proc reader (mem + cpu)
v0.3  ✅ JNI bridge + MetricCollector
v0.4  ✅ WindowManager overlay + HudView
v0.5  ✅ Network monitor (RX/TX delta)
v0.6  ✅ FPS via Choreographer
v0.7  ✅ Storage info
v1.0  ✅ Auto-init via ContentProvider
      ✅ hud-noop module
      ✅ Sample app

── Post-MVP ──────────────────────────────────────
v1.1  Draggable overlay position
v1.2  Tap to expand / collapse detail
v1.3  Alert threshold (warn jika RAM > X MB, FPS < 30)
v1.4  Export snapshot metric ke logcat / file
v2.0  Publish ke Maven Central / JitPack
```

---

## 9. Known Limitations & Catatan

| Topik | Catatan |
|---|---|
| `/proc/net/dev` | Baca **semua** traffic device, bukan hanya app ini. Untuk per-app network, butuh `TrafficStats.getUidRxBytes(uid)` di Kotlin side |
| CPU calculation | Nilai akurat hanya jika polling interval konsisten. Jangan panggil terlalu cepat (< 200ms) |
| `SYSTEM_ALERT_WINDOW` | Android 10+ permission granted by default untuk debug app dari ADB. Physical device mungkin perlu grant manual |
| Emulator | Network & storage info bisa berbeda dari physical device |
| Thread safety | C++ state (prev values) menggunakan `static` — aman selama dipanggil dari single thread (IO coroutine) |

---

*DevHUD — built with C++ NDK + Kotlin. Zero overhead pada release build.*