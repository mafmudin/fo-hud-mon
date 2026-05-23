# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Assemble debug APK (sample app)
./gradlew :app:assembleDebug

# Assemble release APK
./gradlew :app:assembleRelease

# Build hud-core library only
./gradlew :hud-core:assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

> On Windows, use `gradlew.bat` instead of `./gradlew`.

## Module Structure

```
app/          → sample app, minSdk 26
hud-core/     → Android Library, NDK + Kotlin, debug only
hud-noop/     → Android Library, Kotlin stubs, release only
```

`app` wires them via:
```kotlin
debugImplementation(project(":hud-core"))
releaseImplementation(project(":hud-noop"))
```

## Key Architecture Decisions

**AGP 9.x — no explicit Kotlin plugin.** AGP 9.3.0-alpha05 bundles Kotlin support. Do NOT add `kotlin-android` plugin to any module's `plugins {}` block — it will crash with "extension 'kotlin' already registered". `kotlinOptions {}` DSL is also unavailable for the same reason.

**Package namespace:** All Kotlin source files use `package dev.fordewe.fohudmon`. Module `namespace` (for R class) differs:
- `hud-core` → `dev.fordewe.fohudmon.core`
- `hud-noop` → `dev.fordewe.fohudmon.noop`

**JNI naming:** The native library is named `devhud`. JNI function names must match the Kotlin package exactly — prefix is `Java_dev_fordewe_fohudmon_MetricCollector_*`. Any rename of `MetricCollector` or its package requires updating `hud_jni.cpp`.

**Auto-init:** `HudInitProvider` (a `ContentProvider`) runs before `Application.onCreate()`, so calling `DevHud.install()` manually is optional. The provider is declared in `hud-core`'s `AndroidManifest.xml` and merged automatically.

## C++ Layer

Files live in `hud-core/src/main/cpp/`. Each monitor reads from `/proc`:

| File | `/proc` source |
|---|---|
| `cpu_monitor.cpp` | `/proc/self/stat`, `/proc/stat` |
| `mem_monitor.cpp` | `/proc/self/status` |
| `net_monitor.cpp` | `/proc/net/dev` |

CPU and network monitors use **delta-based calculation** — they require a prior sample (`initCpuSampling()` / `initNetSampling()`) and must be called from a single thread consistently. All static state in C++ is safe only when polled from a single IO coroutine.

`net_monitor.cpp` reads **all interfaces except loopback** — it is device-wide traffic, not per-app. For per-app network, switch to `TrafficStats.getUidRxBytes(uid)` on the Kotlin side.

## Overlay Lifecycle

`HudOverlay` checks `Settings.canDrawOverlays()` before adding the view. If permission is missing, it redirects to the system Settings screen — the overlay will not show until the user grants and re-opens the app. `SYSTEM_ALERT_WINDOW` is declared in `hud-core`'s manifest and merged into the app automatically.

## Post-MVP Roadmap

```
v1.1  Draggable overlay
v1.2  Tap to expand/collapse
v1.3  Alert thresholds (RAM > X MB, FPS < 30)
v1.4  Export metrics to logcat / file
v2.0  Publish to Maven Central / JitPack
```
