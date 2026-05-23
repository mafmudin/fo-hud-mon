# DevHUD

Real-time developer overlay for Android — CPU, RAM, Network, Storage, and FPS displayed as a floating HUD on top of any app.

> Debug builds only. Zero overhead on release — the `hud-noop` module replaces all calls with no-ops.

---

## Preview

```
┌─────────────────┐
│ FPS   60        │
│ CPU   3.2%      │
│ RAM   142 MB    │
│  ↓   1.2 KB/s  │
│  ↑   0 B/s     │
│ DSK   4.8 GB    │
└─────────────────┘
```

---

## Requirements

- Min SDK: **26** (Android 8.0)
- NDK: **27.x** — install via Android Studio SDK Manager → SDK Tools
- CMake: **3.28+**
- Permission: `SYSTEM_ALERT_WINDOW` — prompted automatically on first launch

---

## Installation

### 1. Add the modules to your project

Copy `hud-core/` and `hud-noop/` into your project root, then register them in `settings.gradle.kts`:

```kotlin
include(":hud-core", ":hud-noop")
```

### 2. Add dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation(project(":hud-core"))
    releaseImplementation(project(":hud-noop"))
}
```

### 3. Handle the overlay permission

Add to your launcher `Activity`:

```kotlin
override fun onResume() {
    super.onResume()
    if (!Settings.canDrawOverlays(this)) {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    } else {
        DevHud.install(application)
    }
}
```

That's it. The overlay appears automatically on every subsequent launch once permission is granted.

> `DevHud.install()` is idempotent — safe to call on every `onResume()`.

---

## Configuration

Pass a `HudConfig` to customize:

```kotlin
DevHud.install(application, HudConfig(
    showFps          = true,
    showCpu          = true,
    showMemory       = true,
    showNetwork      = true,
    showStorage      = true,
    showThreads      = false,
    updateIntervalMs = 500L,
    position         = HudPosition.TOP_END,
    opacity          = 0.85f,
))
```

### HudPosition

| Value | Location |
|---|---|
| `TOP_END` *(default)* | Top-right |
| `TOP_START` | Top-left |
| `BOTTOM_END` | Bottom-right |
| `BOTTOM_START` | Bottom-left |

---

## Metrics

| Label | Source | Notes |
|---|---|---|
| FPS | `Choreographer.FrameCallback` | Per-frame delta |
| CPU | `/proc/self/stat` + `/proc/stat` | App process % of system total |
| RAM | `/proc/self/status` (VmRSS) | Physical RAM used by this process |
| ↓ / ↑ | `/proc/net/dev` | Device-wide traffic, not per-app |
| DSK | `StatFs` | Free space in app's internal storage partition |
| THR | `/proc/self/status` (Threads) | Hidden by default — enable via `showThreads = true` |

---

## Dismiss

```kotlin
DevHud.dismiss()
```

---

## Roadmap

- **v1.1** — Draggable overlay
- **v1.2** — Tap to expand/collapse detail view
- **v1.3** — Alert thresholds (e.g. warn when FPS < 30 or RAM > 300 MB)
- **v1.4** — Export metric snapshot to logcat / file
- **v2.0** — Publish to Maven Central / JitPack
