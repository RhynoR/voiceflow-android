# VoiceFlow Android

On-device voice dictation overlay for Android. Tap the floating bubble, speak, and your words are injected into any focused text field — no cloud, no internet required.

## Architecture

```
Main process (com.voiceflow.app)
├── FlowService              — foreground service, mic + Whisper, WindowManager overlay bubble
├── MainServiceWatchdog      — BroadcastReceiver on BOOT_COMPLETED, restarts FlowService
└── FlowRecoveryJobService   — scheduled JobService recovery

Accessibility process (:accessibility)
├── FlowAccessibilityService — text injection via ACTION_SET_TEXT + clipboard fallback
└── AccessibilityBridgeService — Messenger-based IPC bridge to main process
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material3 |
| DI | Hilt 2.52 |
| Local DB | Room 2.6.1 |
| Inference | whisper.cpp (NDK/JNI) — `ggml-tiny.en.bin` downloaded on first launch |
| Build | AGP 8.5.2 + Gradle 8.7 + Kotlin DSL |

## Setup

### Prerequisites

- Android Studio Hedgehog or later
- NDK 26.1.10909125 (install via SDK Manager → SDK Tools)
- CMake 3.22.1 (install via SDK Manager → SDK Tools)
- API 29+ device or emulator

### Whisper.cpp submodule

```bash
git submodule add https://github.com/ggerganov/whisper.cpp app/src/main/cpp/whisper.cpp
git submodule update --init --recursive
```

### Build

```bash
./gradlew assembleDebug
```

### Tests

```bash
# Unit tests (JVM, no device needed)
./gradlew test

# Instrumented tests (device/emulator required)
./gradlew connectedAndroidTest
```

### Lint

```bash
./gradlew lint
```

## Permissions required at runtime

| Permission | Purpose |
|---|---|
| `RECORD_AUDIO` | Microphone access |
| `SYSTEM_ALERT_WINDOW` | Floating overlay bubble |
| `Accessibility Service` | Text injection into other apps |

## First launch

1. Launch the app → grant microphone permission
2. Tap **Grant Overlay Permission** → allow in system settings
3. Tap **Enable Accessibility Service** → find VoiceFlow and enable it
4. The bubble appears on screen
5. Tap and hold the bubble, speak, release — text appears in any focused field

## Privacy

All audio processing is performed on-device using `whisper.cpp`. No audio, text, or identifiers are transmitted to any server. The `INTERNET` permission is used only to download the initial `ggml-tiny.en.bin` model file (~39 MB) from HuggingFace on first launch.
