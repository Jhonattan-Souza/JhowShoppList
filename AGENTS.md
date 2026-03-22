# AGENTS.md — JhowShoppList

Android Kotlin shopping-list app built with Jetpack Compose, Room, Hilt, and Clean Architecture. Proposing sweeping changes that improve long-term maintainability is encouraged.

## Build / Lint / Test Commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Build release APK | `./gradlew assembleRelease` |
| Run all unit tests | `./gradlew testDebugUnitTest` |
| Run a single unit test class | `./gradlew testDebugUnitTest --tests "com.jhow.shopplist.domain.usecase.AddShoppingItemUseCaseTest"` |
| Run a single unit test method | `./gradlew testDebugUnitTest --tests "com.jhow.shopplist.domain.usecase.AddShoppingItemUseCaseTest.trimmed names are stored"` |
| Run instrumented tests (needs emulator) | `./gradlew connectedDebugAndroidTest` |
| Full coverage report (unit + instrumented) | `./gradlew jacocoFullReport` |
| Verify coverage threshold (>= 85%) | `./gradlew verifyDebugCoverage` |
| Lint check | `./gradlew lintDebug` |
| Deploy debug app to phone | `./scripts/deploy-debug.sh --launch` |
| Deploy release app to phone | `./scripts/deploy-release.sh --launch` |

## Project Structure

```
app/src/
  main/java/com/jhow/shopplist/
    core/          - Cross-cutting concerns (dispatcher qualifiers)
    data/          - Room entities, DAOs, repository implementations, mappings
    di/            - Hilt modules (@Module, @Binds, @Provides)
    domain/        - Models, repository interfaces, use cases
    presentation/  - ViewModels, Compose screens, UI state
    ui/theme/      - Color, Typography, Theme
  test/java/       - JUnit 4 unit tests
  androidTest/java/- Compose UI + Room DAO instrumented tests
```

## Core Priorities

1. Performance first.
2. Reliability first.
3. Keep behavior predictable under load and during failures (session restarts, reconnects, partial streams).

If a tradeoff is required, choose correctness and robustness over short-term convenience.

## Maintainability

Long term maintainability is a core priority. If you add new functionality, first check if there is shared logic that can be extracted to a separate module. Duplicate logic across multiple files is a code smell and should be avoided. Don't be afraid to change existing code. Don't take shortcuts by just adding local logic to solve a problem.

## Architecture & Patterns

- **Clean Architecture**: `data` → `domain` → `presentation` dependency direction.
- **Use Cases**: One class per operation. Injected into ViewModels via constructor.
- **Repository**: Interface in `domain/repository`, implementation in `data/repository`.
- **DI**: Hilt with `@HiltViewModel`, `@Module`, `@InstallIn(SingletonComponent::class)`.
- **State**: `StateFlow<UiState>` exposed from ViewModel, collected with `collectAsStateWithLifecycle()`.
- **Navigation**: Single-activity (`MainActivity`) with Compose navigation.
- **Database**: Room with KSP annotation processing.

## Adding New Features

1. Create a new branch for the feature based on the main
2. Create atomic commits for each change with a clear description of the changes
3. Create a pull request with a clear description of the changes

## Testing Requirements

- Unit test coverage must stay >= 85% (enforced by `verifyDebugCoverage`).
- Always run `./gradlew lintDebug` after making changes.

## Deployment Notes

- Production app package: `com.jhow.shopplist`.
- Debug app package: `com.jhow.shopplist.debug`.
- Daily-use install is the production app `com.jhow.shopplist`.
- Debug install is isolated for local testing, adb hooks, and instrumented tests.
- `./scripts/deploy-debug.sh` always runs `assembleDebug` first, then installs with `adb install -r` so the debug app updates in place without clearing database or app data.
- `./scripts/deploy-release.sh` always runs `assembleRelease` first, then installs with `adb install -r` so the release app updates in place without clearing database or app data.
- `./scripts/deploy-phone.sh` intentionally exits with instructions so deploys must go through an explicit debug or release script.
- `./scripts/backup-release-keystore.sh DESTINATION_DIR` creates a secure local backup of the release keystore plus `keystore.properties`.
- The debug launcher label is `JhowShoppList Debug` and uses a different icon color to distinguish it from production.
- `connectedDebugAndroidTest` targets the debug app only and should not affect the production install.
