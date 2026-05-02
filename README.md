# JhowShoppList

[![Release](https://github.com/Jhonattan-Souza/JhowShoppList/actions/workflows/release.yml/badge.svg)](https://github.com/Jhonattan-Souza/JhowShoppList/actions/workflows/release.yml)

Android shopping list app built with Kotlin, Jetpack Compose, Room, Hilt, Coroutines, and Flow.

## Installation

[![Get it on GitHub Releases](.github/assets/get-it-on-github.svg)](https://github.com/Jhonattan-Souza/JhowShoppList/releases)
[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.jhow.shopplist%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FJhonattan-Souza%2FJhowShoppList%22%2C%22author%22%3A%22Jhonattan-Souza%22%2C%22name%22%3A%22JhowShoppList%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22verifyLatestTag%5C%22%3Atrue%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22JhowShoppList%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22refreshBeforeDownload%5C%22%3Atrue%7D%22%7D)

GitHub Releases is the canonical distribution channel for release APKs.

- Requires Android 13 (API 33) or newer.
- Fast install: open the releases page above and download the latest signed APK.
- Obtainium: click the badge above or add `https://github.com/Jhonattan-Souza/JhowShoppList` as a GitHub source to install and receive updates from GitHub Releases.
- AppVerifier: use the package name below and the SHA-256 fingerprint before installing a downloaded APK.

Verification info:

- Package ID: `com.jhow.shopplist`
- SHA-256 hash of signing certificate: `E1:61:7B:4A:4A:98:B1:CE:33:7D:CF:23:69:EB:99:E7:18:46:E4:B0:D0:6E:37:5D:3E:42:F7:73:40:6E:DC:0D`
    - Note: This signature is valid for all GitHub Release APKs.
- Verify a downloaded APK with `apksigner verify --print-certs app-release.apk`

## What it does

- keeps shopping items in a local offline-first database
- separates pending and purchased items on a single screen
- supports multi-select purchase confirmation
- tracks purchase frequency for database-driven ordering
- includes debug hooks for validation through `adb`
- keeps debug and production installs isolated on-device

## Architecture

- Clean Architecture inside a single app module
- MVVM with unidirectional data flow
- Room as the local source of truth
- Hilt for dependency injection

## Project structure

- `app/src/main/java/com/jhow/shopplist/data` - Room entities, DAO, repository implementation
- `app/src/main/java/com/jhow/shopplist/domain` - models, repository contract, use cases
- `app/src/main/java/com/jhow/shopplist/presentation` - ViewModel and Compose UI
- `app/src/debug` - debug-only adb validation receiver
- `scripts/deploy-debug.sh` - installs debug builds as in-place phone updates
- `scripts/deploy-release.sh` - installs signed release builds as in-place phone updates

## Quality gates

- unit tests and instrumented tests are both required
- combined coverage is enforced at `85%` or higher

## Useful commands

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew verifyDebugCoverage
./gradlew lintDebug
./scripts/deploy-debug.sh --launch
./scripts/deploy-release.sh --launch
```

## Phone deploy behavior

- debug installs use the package `com.jhow.shopplist.debug`
- production keeps the package `com.jhow.shopplist`
- instrumented tests target the debug app only, so they do not touch production data
- `scripts/deploy-debug.sh` installs `com.jhow.shopplist.debug` with `adb install -r`, which updates the debug app without clearing its database or app data
- `scripts/deploy-release.sh` installs `com.jhow.shopplist` with `adb install -r`, which updates the release app without clearing its database or app data

## Open-source safety

- Do not publish `keystore.properties`, `local.properties`, `google-services.json`, keystores, certificates, `.env` files, APKs, AABs, build outputs, IDE state, or generated agent/session files.
- `android:allowBackup` is disabled and backup rules exclude app databases, shared preferences, files, and external files so shopping-list and sync data is not exported by Android backup mechanisms.
- The debug-only adb receiver can reset, seed, sync, and dump local app state; do not distribute debug builds to end users.

## adb validation hooks

```bash
adb shell am broadcast -a com.jhow.shopplist.debug.RESET_DB -n com.jhow.shopplist.debug/com.jhow.shopplist.debug.DebugCommandReceiver
adb shell am broadcast -a com.jhow.shopplist.debug.SEED_SAMPLE -n com.jhow.shopplist.debug/com.jhow.shopplist.debug.DebugCommandReceiver
adb shell am broadcast -a com.jhow.shopplist.debug.DUMP_STATE -n com.jhow.shopplist.debug/com.jhow.shopplist.debug.DebugCommandReceiver
```
