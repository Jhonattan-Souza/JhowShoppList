# JhowShoppList

Android shopping list app built with Kotlin, Jetpack Compose, Room, Hilt, Coroutines, and Flow.

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

## Release signing

- Prefer exporting the matching `JHOW_SHOPPLIST_RELEASE_*` environment variables for release signing.
- If you copy `keystore.properties.example` to `keystore.properties`, keep it local only, store the keystore outside this repository, and delete the file before sharing or archiving the repository directory.
- `scripts/deploy-release.sh` refuses to run unless all release signing values are present.
- `./gradlew assembleRelease` can be used without signing secrets for local release-build verification; signed release deployment still requires release signing values.
- Use a dedicated release keystore for `com.jhow.shopplist`; do not sign the production app with the debug keystore.
- `./scripts/backup-release-keystore.sh DESTINATION_DIR` creates a timestamped backup containing both the release keystore and `keystore.properties`.

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
