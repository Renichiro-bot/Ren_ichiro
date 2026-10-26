# Premium Notes (Vellum)

*A privacy-first, tactile, precision-crafted notes and productivity workspace for Android.*

---

## Overview

**Vellum** is a native Android application built with modern Kotlin and Jetpack Compose. Engineered with strict data-integrity guarantees, an offline-first architecture, and a tactile Material Design 3 user interface, Vellum provides a private, reliable workspace for thoughts, checklists, tasks, reminders, and documents.

Vellum operates **100% locally and offline**. It has zero network permissions, contains no analytics, telemetry, or third-party trackers, and never transmits your personal data, notes, or attachments outside your device.

---

## Key Features

- **Rich Note Crafting & Organization:**
  - Fast, debounced autosave with instant persistence to SQLite.
  - Markdown formatting helpers (bold, italic, headings, lists, quotes, code).
  - Interactive checklists with progress tracking and reordering.
  - Hierarchical folders and multi-tag categorization.
  - Pinning, favorites, archive, and soft-delete Trash with full restore capabilities.
  - Note color palettes with automatic contrast adaptation.

- **Tasks, Reminders & Productivity:**
  - Standalone task checklists and reminder scheduling.
  - Exact alarms via Android `AlarmManager` with sanitized notifications.
  - Calendar agenda view mapping notes and reminders to dates.
  - Customizable note templates for meetings, daily journals, and checklists.

- **Attachments & On-Device OCR:**
  - Secure internal attachment storage for images and documents.
  - Android Photo Picker integration (zero broad storage permissions).
  - 100% on-device text recognition (Google ML Kit) with interactive review and preview before inserting.
  - Memory-safe image downsampling preventing out-of-memory errors on large photos.

- **System Integration:**
  - **Android Home-Screen Widgets:** Scrollable recent notes collection widget and compact 2x1 quick-action widget. Includes privacy toggle to hide note previews against shoulder-surfing.
  - **App Shortcuts & Deep Linking:** Quick launcher actions and deep linking (`vellum://note/{id}`, `vellum://new`, `vellum://search`, `vellum://favorites`) protected against path traversal and script injection.
  - **Share Sheet Receiver:** Receive text shared from any Android app directly into a draft note with memory safeguards (clamped to 500 KB).

- **Backup, Restore & Privacy:**
  - Full local backup export and import using standard ZIP archives.
  - Zip-Slip path-traversal protection.
  - Deterministic conflict resolution (newer timestamp wins) in merge mode.
  - Single-note Markdown import and export.
  - Private app sandbox isolation (`context.filesDir/attachments`).

- **Accessibility & Design:**
  - Fully compliant with WCAG 2.1 AA contrast and 48dp minimum touch target standards.
  - TalkBack screen reader support with explicit heading semantics.
  - Dynamic Color (Android 12+), AMOLED true-black dark theme, and light/dark theme modes.
  - Adaptive layouts supporting 2-column staggered grid and 1-column linear list.

---

## Technical Stack & Architecture

- **Language:** Kotlin 100%
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** Clean Architecture & MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Persistence:** AndroidX Room SQLite (Database Version 4) with explicit non-destructive migrations (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`)
- **Settings:** AndroidX Jetpack DataStore Preferences
- **OCR:** Google ML Kit Text Recognition (local on-device inference)
- **System Integration:** Android `AppWidgetProvider`, `RemoteViews`, `AlarmManager`, `NotificationManager`, `ShortcutManagerCompat`
- **Testing:** JUnit 4, Robolectric, Kotlin Coroutines Test

---

## Building and Installing

### 1. Prerequisites
- **JDK:** Java 17 or newer
- **Android SDK:** Compile SDK 35, Min SDK 26, Target SDK 35
- **Build System:** Gradle (Kotlin DSL)

### 2. Assemble Debug APK
To compile the application and assemble the verified standalone debug APK:
```bash
gradle :app:assembleDebug
```
The resulting installable APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Run Automated Test Suite
To execute all local JVM and Robolectric tests across data persistence, migrations, security, and UI invariants:
```bash
gradle :app:testDebugUnitTest
```
HTML test reports are generated at:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

### 4. Device Installation Instructions

#### Method A: Direct ADB Installation (Recommended for Developers)
1. Enable **Developer Options** and **USB Debugging** on your Android device (Settings > About Phone > Tap "Build Number" 7 times).
2. Connect your device to your computer via USB.
3. Verify connection:
   ```bash
   adb devices
   ```
4. Install the debug APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

#### Method B: Direct Device Installation (Manual / Sideloading)
1. Copy `app/build/outputs/apk/debug/app-debug.apk` to your phone via USB cable, Google Drive, or local file sharing.
2. On your device, open your File Manager and locate `app-debug.apk`.
3. Tap the APK file to install.
4. If prompted with *"Install unknown apps"*, allow your file manager permission to install.
5. Tap **Install** to complete installation.

---

## Production Release Signing

The debug APK uses the default Android development keystore (`debug.keystore`). For official distribution (e.g., Google Play Store or F-Droid):

1. Generate a dedicated release keystore:
   ```bash
   keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
   ```
2. Store `my-release-key.jks` in a secure offline location. Never commit keystores, passwords, or credentials into version control.
3. Configure the signing environment variables prior to running the release assemble task:
   ```bash
   export KEYSTORE_PATH="/path/to/my-release-key.jks"
   export STORE_PASSWORD="your-keystore-password"
   export KEY_PASSWORD="your-key-password"
   gradle :app:assembleRelease
   ```

---

## Project Structure

```
.
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   ├── backup/          # ZIP backup, Zip-Slip defense, merge engine
│   │   │   │   │   ├── db/              # Room database, v1->v4 migrations, DAOs, entities
│   │   │   │   │   ├── model/           # Core domain models (Note, Task, Attachment, etc.)
│   │   │   │   │   ├── repository/     # Repository implementations & state management
│   │   │   │   │   └── storage/        # Attachment file storage sandboxing
│   │   │   │   ├── receiver/            # Reminder alarms & widget broadcast receivers
│   │   │   │   ├── shortcuts/           # Shortcut manager & deep link sanitization
│   │   │   │   ├── ui/                  # Jetpack Compose screens, ViewModels, theme
│   │   │   │   ├── widget/              # RemoteViews widget providers & factory services
│   │   │   │   ├── MainActivity.kt      # Main entry point & deep link dispatcher
│   │   │   │   └── VellumApplication.kt # Lazy dependency container & application lifecycle
│   │   │   └── res/                     # Vector drawables, strings, widget layouts, shortcuts
│   │   └── test/                        # Robolectric & unit test suites (Levels 5-9)
│   ├── build.gradle.kts                 # App module Gradle configuration
│   └── proguard-rules.pro               # ProGuard/R8 obfuscation & keep rules
├── CHANGELOG.md                         # Detailed version changelog
├── DESIGN_SYSTEM.md                     # Color tokens, typography, component standards
├── FEATURE_ROADMAP.md                   # Milestone tracking & dependency matrix
├── IMPLEMENTATION_STATUS.md             # Current level status & technical verification
├── PROJECT_BLUEPRINT.md                 # Core architecture specifications
├── README.md                            # Project overview & build guide
├── TESTING_AND_RELEASE.md               # Quality gates, testing strategy & release guide
└── settings.gradle.kts                  # Root project settings
```

---

## Privacy & Security Guarantees

- **Zero Network Permissions:** The app manifest contains no `android.permission.INTERNET`. The app cannot physically transmit data across the internet.
- **Logcat Sanitization:** Reminder notifications and note metadata never leak user-authored text into Android system logs.
- **Strict File Sandboxing:** Attachments and database files reside exclusively in protected app-internal directories (`context.filesDir`).
- **Zip-Slip Protection:** Backup archives are verified before extraction; any paths containing directory traversal characters (`..`) are rejected.
- **Deep-Link Defense:** URL parameters are strictly validated and sanitized to prevent SQL injection, XSS, and directory traversal.
