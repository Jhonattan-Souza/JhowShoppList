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
- `scripts/deploy-phone.sh` - installs debug builds as in-place phone updates

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
./scripts/deploy-phone.sh --launch
```

## Phone deploy behavior

- debug installs use the package `com.jhow.shopplist.debug`
- production keeps the package `com.jhow.shopplist`
- instrumented tests target the debug app only, so they do not touch production data
- `scripts/deploy-phone.sh` installs with `adb install -r`, which updates the debug app without clearing its database or app data

## adb validation hooks

```bash
adb shell am broadcast -a com.jhow.shopplist.debug.RESET_DB -n com.jhow.shopplist.debug/com.jhow.shopplist.debug.DebugCommandReceiver
adb shell am broadcast -a com.jhow.shopplist.debug.SEED_SAMPLE -n com.jhow.shopplist.debug/com.jhow.shopplist.debug.DebugCommandReceiver
adb shell am broadcast -a com.jhow.shopplist.debug.DUMP_STATE -n com.jhow.shopplist.debug/com.jhow.shopplist.debug.DebugCommandReceiver
```
