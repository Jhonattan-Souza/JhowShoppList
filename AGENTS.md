# AGENTS.md — JhowShoppList

Android Kotlin shopping-list app built with Jetpack Compose, Room, Hilt, and Clean Architecture.

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

## Architecture & Patterns

- **Clean Architecture**: `data` → `domain` → `presentation` dependency direction.
- **Use Cases**: One class per operation. Injected into ViewModels via constructor.
- **Repository**: Interface in `domain/repository`, implementation in `data/repository`.
- **DI**: Hilt with `@HiltViewModel`, `@Module`, `@InstallIn(SingletonComponent::class)`.
- **State**: `StateFlow<UiState>` exposed from ViewModel, collected with `collectAsStateWithLifecycle()`.
- **Navigation**: Single-activity (`MainActivity`) with Compose navigation.
- **Database**: Room with KSP annotation processing.

## Code Style

- **Kotlin style**: `kotlin.code.style=official` (set in `gradle.properties`).
- **Formatting**: 4-space indentation; no wildcard imports; Android Studio default ordering (androidx → third-party → project).
- **Naming**:
  - Classes/Interfaces: PascalCase (`ShoppingListViewModel`, `ShoppingListRepository`)
  - Functions/variables: camelCase (`observePendingItems`, `inputValue`)
  - Use cases: verb phrase (`AddShoppingItemUseCase`, `MarkSelectedItemsPurchasedUseCase`)
  - Test method names: backtick strings with spaces (`\`adding an item trims input\``)
- **Types**: Explicit types on public API (interface methods, constructor params); inferred types for local vars and private members.
- **Nullability**: Non-nullable by default; use `?` only when a value is genuinely optional.
- **Coroutines**: `suspend` for one-shot operations; `Flow` for reactive streams. Inject `@IoDispatcher` for IO-bound work.
- **Compose**: Stateful wrapper (`XxxRoute`) calls stateless `XxxScreen`; `modifier` as last default parameter; `testTag` on interactive elements.
- **Error handling**: Guard clauses with early returns (`if (currentValue.isBlank()) return`). No checked exceptions wrapping.

## Testing Conventions

- **Framework**: JUnit 4 with `@Test`, `@Before`, `@Rule`.
- **Coroutines**: `runTest { }` for suspend tests; `MainDispatcherRule` to swap `Dispatchers.Main`.
- **Flow testing**: Turbine (`flow.test { awaitItem() }`) for asserting emitted values.
- **Fakes over mocks**: `FakeShoppingListRepository` in `testing/` package implements the repository interface in-memory.
- **Compose UI tests**: `createAndroidComposeRule<MainActivity>()` with `testTag` selectors and `waitUntil` for async state.
- **Room DAO tests**: Instrumented (`androidTest`), use real in-memory Room database.

## Key Libraries (managed via `gradle/libs.versions.toml`)

Compose BOM `2024.09.00` · Room `2.7.2` · Hilt `2.57` · KSP `2.2.10-2.0.2` · Coroutines `1.10.2` · Turbine `1.2.1` · JaCoCo `0.8.13`

## Adding New Features

1. Define the domain model in `domain/model/`.
2. Add repository method signature in `domain/repository/ShoppingListRepository.kt`.
3. Implement in `data/repository/ShoppingListRepositoryImpl.kt`; add Room migration if schema changes.
4. Create a use case in `domain/usecase/` — one class, single `operator fun invoke()`.
5. Add the use case to `ShoppingListViewModel` constructor; wire action method.
6. Update `ShoppingListUiState` if new UI fields are needed.
7. Add Compose UI in `ShoppingListScreen.kt`; use `testTag` on interactive elements.
8. Write unit tests for use case and ViewModel; add Compose UI test if user-facing.

## Dependency Injection Rules

- Bind repository implementations in `di/AppModule.kt` using `@Binds` within an abstract `AppBindModule`.
- Provide concrete dependencies (database, DAOs, dispatchers) in the `object AppModule` using `@Provides`.
- ViewModels receive use cases through `@Inject constructor` and are annotated `@HiltViewModel`.
- Custom qualifiers (e.g. `@IoDispatcher`) live in `core/dispatchers/`.

## Room Database Rules

- Entity classes in `data/local/entity/` use the `Room` annotations.
- DAOs in `data/local/dao/` return `Flow` for reactive queries, `suspend` for mutations.
- Mapping between domain models and Room entities is in `data/repository/ShoppingItemMappings.kt`.
- Database class is in `data/local/db/AppDatabase.kt`.

## Compose UI Guidelines

- Compose screens live in `presentation/shoppinglist/`.
- `ShoppingListRoute` is the stateful wrapper; `ShoppingListScreen` is stateless.
- Interactive elements must have `testTag` using constants from `ShoppingListTestTags`.
- Use `collectAsStateWithLifecycle()` to observe ViewModel state flows.
- `modifier` parameter goes last with a `Modifier = Modifier` default.
- Use `stringResource()` for all user-visible strings.

## Testing Requirements

- Unit test coverage must stay >= 85% (enforced by `verifyDebugCoverage`).
- Use `FakeShoppingListRepository` for unit tests, not real implementations.
- Compose UI tests use `createAndroidComposeRule<MainActivity>()` with `testTag` lookups.
- DAO tests are instrumented (`androidTest`) and use in-memory Room databases.
- Always run `./gradlew lintDebug` after making changes.
