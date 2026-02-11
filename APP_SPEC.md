# Insight Camera - App Specification

## Overview

| Item | Value |
|------|-------|
| App Name | Insight Camera |
| Package | `com.harmonic.insight.camera` |
| Platform | Android |
| Min SDK | 28 (Android 9 Pie) |
| Target SDK | 35 (Android 15) |
| Language | Kotlin 2.1 |
| UI Framework | Jetpack Compose + Material 3 |
| Camera Engine | CameraX 1.4 + Extensions |
| Architecture | Single-Activity, Composable-driven |
| License | Proprietary (HarmonicInsight) |

## Purpose

Samsung Galaxy Fold 7 をはじめとする Android 端末の標準カメラアプリは機能が多すぎて使いづらい。
Insight Camera は**「難しいことを考えなくても綺麗な写真が撮れる」**をコンセプトに、
本当に必要な機能だけを前面に出したシンプルなカメラアプリである。

### Core Value Proposition

1. **常時ライト点灯** - 撮影構図を確認しながらフラッシュライトを常時 ON にできる
2. **OEM 画質の自動適用** - CameraX Extensions により端末メーカーの画像処理を自動活用
3. **ワンタップ操作** - フォーカス、撮影、録画がすべてワンタップで完結

---

## Features

### Photo

| Feature | Description | Status |
|---------|-------------|--------|
| Photo Capture | JPEG 最高画質で MediaStore に保存 | Done |
| Tap to Focus | タップ位置にオートフォーカス + 自動露出調整 (AF/AE) | Done |
| Flash Mode | OFF / ON / AUTO の 3 モード切替 | Done |
| Flashlight (Torch) | 撮影中もトーチを常時点灯できるトグル | Done |
| Timer | OFF / 3 秒 / 10 秒のセルフタイマー | Done |
| Aspect Ratio | 4:3 / 16:9 の切替 | Done |
| CameraX Extensions | AUTO / HDR / Night / Bokeh / Beauty の自動検出・適用 | Done |

### Video

| Feature | Description | Status |
|---------|-------------|--------|
| Video Recording | MP4 最高画質で MediaStore に保存 | Done |
| Audio Recording | RECORD_AUDIO 許可時のみ音声録音 (未許可時は映像のみ) | Done |
| Recording Indicator | 録画中の赤ドット + 経過時間表示 | Done |

### Camera Control

| Feature | Description | Status |
|---------|-------------|--------|
| Pinch to Zoom | ピンチジェスチャーによるスムーズズーム | Done |
| Zoom Presets | 端末の光学性能に応じた動的プリセット (0.5x〜10x) | Done |
| Camera Switch | フロント/バック切替 (単カメラ端末では非表示) | Done |
| Zoom Indicator | ピンチ中に現在倍率をオーバーレイ表示 | Done |

### Cross-Device Safety

| Feature | Description | Status |
|---------|-------------|--------|
| Multi-camera Detection | 起動時にカメラ数を検出、UIを動的に適応 | Done |
| Extension Fallback | AUTO → HDR → NONE の自動フォールバック | Done |
| Bind Failure Recovery | カメラバインド失敗時に Extensions なしで再試行 | Done |
| Rotation Support | `fullSensor` で縦横・フォルダブル対応 | Done |
| Permission Safety | 各パーミッション未許可でもクラッシュしない | Done |

---

## UI Structure

```
┌──────────────────────────────┐
│  CameraTopBar                │  Flash | Timer | Ratio | Switch
│  [Extensions Badge]         │  "AUTO" / "HDR" etc.
├──────────────────────────────┤
│                              │
│       Camera Preview         │  PreviewView (FILL_CENTER)
│       + Focus Ring Overlay   │  Tap to focus animation
│       + Zoom Indicator       │  Shows during pinch
│                              │
├──────────────────────────────┤
│  Light Toggle                │  LIGHT ON / OFF (back camera only)
│  Zoom Preset Bar             │  1x  2x  3x  5x ...
│  Mode Selector               │  PHOTO | VIDEO
│  [Thumbnail] [Shutter] [ ]  │  Gallery / Capture / Balance
│  [Recording Indicator]       │  ● 0:12 (video mode)
│  [Timer Countdown]           │  3... 2... 1... (fullscreen)
└──────────────────────────────┘
```

---

## Tech Stack

### Dependencies

| Category | Library | Version |
|----------|---------|---------|
| Core | `androidx.core:core-ktx` | 1.15.0 |
| Lifecycle | `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.7 |
| Activity | `androidx.activity:activity-compose` | 1.9.3 |
| Compose BOM | `androidx.compose:compose-bom` | 2024.12.01 |
| Compose UI | `ui`, `ui-graphics`, `material3`, `material-icons-extended` | BOM |
| CameraX | `camera-core`, `camera2`, `lifecycle`, `view`, `video`, `extensions` | 1.4.1 |
| Permissions | `accompanist-permissions` | 0.36.0 |
| Image Loading | `coil-compose` | 2.7.0 |

### Build Configuration

| Setting | Value |
|---------|-------|
| AGP | 8.7.3 |
| Kotlin | 2.1.0 |
| JVM Target | 17 |
| Compose | Kotlin Compiler Plugin |
| minSdk | 28 |
| targetSdk | 35 |
| compileSdk | 35 |
| Debug Suffix | `.debug` |
| Release | R8 minify + resource shrink |

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── kotlin/com/harmonic/insight/camera/
│   ├── InsightCameraApp.kt          # Application class
│   ├── MainActivity.kt              # Entry point + permission gate
│   ├── camera/
│   │   └── CameraController.kt      # CameraX + Extensions engine
│   └── ui/
│       ├── CameraScreen.kt          # Main camera composable
│       ├── components/
│       │   ├── AspectRatioControl.kt # 4:3 / 16:9 toggle
│       │   ├── CameraTopBar.kt      # Top control bar
│       │   ├── FlashControl.kt      # Flash mode + light toggle
│       │   ├── FocusRing.kt         # Tap-to-focus animation
│       │   ├── ModeSelector.kt      # PHOTO / VIDEO switcher
│       │   ├── RecordButton.kt      # Video record button
│       │   ├── ShutterButton.kt     # Photo shutter button
│       │   ├── TimerControl.kt      # Timer button + countdown overlay
│       │   └── ZoomControl.kt       # Zoom presets + indicator
│       └── theme/
│           ├── Color.kt             # Brand color palette
│           ├── Theme.kt             # Material 3 dark theme
│           └── Type.kt              # Typography
├── res/
│   ├── drawable/
│   │   ├── ic_launcher_background.xml   # Gradient background
│   │   ├── ic_launcher_foreground.xml   # Lens + light beam
│   │   └── ic_launcher_monochrome.xml   # Android 13+ themed icon
│   ├── mipmap-anydpi-v26/
│   │   └── ic_launcher.xml              # Adaptive icon definition
│   ├── values/
│   │   ├── colors.xml
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── xml/
│       └── file_paths.xml
```

---

## Permissions

| Permission | Required | Purpose |
|------------|----------|---------|
| `CAMERA` | Yes (runtime) | Photo/video capture |
| `RECORD_AUDIO` | No (runtime) | Video audio recording. Denied = silent video |

### Hardware Features (all `required="false"`)

- `android.hardware.camera`
- `android.hardware.camera.autofocus`
- `android.hardware.camera.flash`
- `android.hardware.microphone`

---

## CameraX Extensions Strategy

```
Startup
  │
  ├─ ExtensionsManager.getInstanceAsync()
  │
  ├─ Detect available modes:
  │   AUTO, HDR, NIGHT, BOKEH, FACE_RETOUCH
  │
  ├─ Select best mode:
  │   AUTO (preferred) → HDR (fallback) → NONE (base)
  │
  ├─ Apply to CameraSelector (photo mode only)
  │
  └─ On bind failure:
      ├─ Retry without extensions
      └─ On second failure: camera = null (graceful degradation)

Camera Switch
  │
  ├─ Re-detect extensions for new lens facing
  ├─ Reselect best available mode
  └─ On failure: revert to previous lens
```

---

## Design Language

### Color Palette

| Name | Hex | Usage |
|------|-----|-------|
| InsightBlack | `#0A0A0A` | Background |
| InsightAccent | `#3D7BF7` | Primary actions, focus ring, selected state |
| InsightWhite | `#FAFAFA` | Text, icons |
| FlashYellow | `#FFD54F` | Light ON indicator |
| InsightError | `#EF5350` | Recording dot |

### Design Principles

1. **Dark-first** - カメラアプリは常にダークテーマ
2. **Minimal chrome** - コントロールは半透明背景で控えめに配置
3. **Progressive disclosure** - 撮影モードに応じて関連コントロールのみ表示
4. **Adaptive layout** - 端末の能力に応じて UI 要素を動的に表示/非表示

---

## CI/CD

### GitHub Actions (`build.yml`)

| Trigger | Job | Output |
|---------|-----|--------|
| Push to `main`, `claude/**` | `build` | Debug APK (30-day retention) |
| PR to `main` | `build` | Debug APK (30-day retention) |
| Push to `main` | `release` | Release APK (90-day retention) |

Pipeline: Checkout → JDK 17 → Android SDK → Gradle Cache → Build → Upload Artifact

---

## Supported Devices

| Category | Support Level |
|----------|--------------|
| Samsung Galaxy Fold series | Primary target. Extensions (AUTO/HDR) expected |
| Samsung Galaxy S series | Full support. Rich Extensions |
| Google Pixel | Full support. Extensions available |
| Other OEMs (Xiaomi, OPPO, etc.) | Core features work. Extensions vary |
| Single-camera devices | Supported. Switch button hidden |
| Tablets | Supported. fullSensor rotation |
| Foldables | Supported. configChanges handles fold/unfold |

---

## Known Limitations

1. **Extensions are photo-only** - CameraX Extensions API does not apply to video recording
2. **No manual controls** - ISO, shutter speed, white balance are not exposed (by design)
3. **No gallery screen** - Tapping the thumbnail opens the system gallery/viewer
4. **No cloud sync** - Photos/videos saved to local MediaStore only
5. **No image editing** - Post-capture editing is not in scope

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02 | Initial release: photo, video, flashlight, zoom, timer, aspect ratio, CameraX Extensions, cross-device safety |
