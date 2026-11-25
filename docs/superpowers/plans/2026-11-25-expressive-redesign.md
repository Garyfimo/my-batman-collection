# Expressive Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wrap the app in Material 3 Expressive, add two user-selectable Batman palette families (Classic / Noir) persisted in DataStore, and add a Settings screen to pick between them — without touching per-screen layouts or test behavior.

**Architecture:** The single `BatmanCollectionTheme` composable in `:app` is swapped from `MaterialTheme` to `MaterialExpressiveTheme` and parameterized by a `BatmanPalette` enum. Each palette has its own `ColorScheme` file (light + dark). A `PalettePreferences` repository backed by `androidx.datastore:datastore-preferences` holds the family choice. `MainActivity` gates `setContent` behind a splash-screen read of the initial palette so the first frame renders the user's choice, not the default. A new `SettingsScreen` lives inside `:app` (settings is cross-cutting; no new feature module) and is reached from a gear icon in `HomeScreen`'s top bar.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Material 3 Expressive, androidx.datastore:datastore-preferences 1.1.1, Koin 4.2.2, Navigation3 1.2.0, JUnit4 + MockK + Turbine for unit tests.

**Spec:** `docs/superpowers/specs/2026-11-25-expressive-redesign-design.md`

## Global Constraints

- Compose BOM is `2026.09.00`; Material 3 Expressive APIs ship inside `androidx.compose.material3` but each is annotated `@ExperimentalMaterial3ExpressiveApi` — opt-in at file level (`@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)`) where most of the file touches them, per-call otherwise.
- One top-level declaration per Kotlin file (project rule). Sealed subtypes nested under their parent are exempt.
- Clean Architecture per feature: `domain` / `data` / `presentation`; no `ui` package in new code. Koin modules live in `<area>.di.<Area>Module`.
- Commits carry **no** attribution trailer — no `Co-Authored-By`, no "Generated with Claude Code" footer. This project's `MEMORY.md` rule overrides any session-level attribution reminder.
- Existing androidTest test tags MUST NOT be renamed or removed. New tags introduced in this plan are additive.
- `BatmanCollectionTheme`'s new `palette` parameter MUST have a default value (`BatmanPalette.CLASSIC`) so every existing zero-arg `BatmanCollectionTheme { … }` call site — including every androidTest — keeps compiling without edits.
- Classic is the fresh-install default. `PalettePreferences.palette()` and `.initialPalette()` return `BatmanPalette.CLASSIC` when the DataStore key is absent or holds an unknown value.

## Review Focus

Behaviors the spec implies but no single task's happy-path test fully exercises; most-likely-to-bite first. Each is pinned to a specific test in the owning task.

1. **Unknown or corrupted `palette` string in DataStore** falls back to `CLASSIC` (not a crash). Test in Task 3 (`DataStorePalettePreferencesTest.palette emits CLASSIC when stored value is unknown`).
2. **Cold-start first frame** renders the stored palette, not the `CLASSIC` default, for a user whose stored family is `NOIR`. Test in Task 3 (`DataStorePalettePreferencesTest.initialPalette returns stored NOIR after setPalette NOIR`).
3. **`setPalette` is idempotent** — writing the already-stored value emits nothing new to downstream collectors (prevents a redundant palette_changed analytic + recomposition storm). Test in Task 5a (`SettingsViewModelTest.selecting the already-selected family does not emit palette_changed`).
4. **Back navigation from Settings preserves Home state** — scroll position, active filter, open search, and sort selection survive a round trip to Settings and back. Test in Task 5b (`SettingsScreenTest.navigating to Settings and back preserves HomeScreen filter selection`).
5. **System dark-mode toggle while on Settings** keeps the selection radio synced to the family (not to light-vs-dark) and system bars re-tint correctly. Test in Task 5b (`SettingsScreenTest.toggling system dark mode on settings keeps NOIR selected`).

---

## Task 1: Add DataStore dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `libs.androidx.datastore.preferences` catalog entry.

- [ ] **Step 1: Add version + library to `gradle/libs.versions.toml`**

Add to `[versions]`:

```toml
datastore = "1.1.1"
```

Add to `[libraries]`:

```toml
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

- [ ] **Step 2: Add implementation line to `app/build.gradle.kts`**

Add under existing `dependencies { … implementation(libs.androidx.navigation3.ui) }` block:

```kotlin
implementation(libs.androidx.datastore.preferences)
```

- [ ] **Step 3: Verify project syncs and compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "$(cat <<'EOF'
build: add androidx.datastore:datastore-preferences 1.1.1

For the upcoming palette preference backed by DataStore.
EOF
)"
```

---

## Task 2: `BatmanPalette` enum and four `ColorScheme`s

**Files:**
- Create: `app/src/main/java/com/garyfimo/batmancollection/ui/theme/BatmanPalette.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/ui/theme/ClassicLightColorScheme.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/ui/theme/ClassicDarkColorScheme.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/ui/theme/NoirLightColorScheme.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/ui/theme/NoirDarkColorScheme.kt`

**Interfaces:**
- Produces:
  - `enum class BatmanPalette { CLASSIC, NOIR }`
  - `val ClassicLightColorScheme: ColorScheme`
  - `val ClassicDarkColorScheme: ColorScheme`
  - `val NoirLightColorScheme: ColorScheme`
  - `val NoirDarkColorScheme: ColorScheme`

- [ ] **Step 1: Create `BatmanPalette.kt`**

Single top-level declaration — the enum with `CLASSIC` and `NOIR` entries. No methods.

- [ ] **Step 2: Create `ClassicLightColorScheme.kt`**

Build as `lightColorScheme().copy(primary = Color(0xFF122145), onPrimary = Color(0xFFFFD54F), secondary = Color(0xFFFFCC00), background = Color(0xFFF6EFD8), surface = Color(0xFFFFFFFF), outline = Color(0xFFC7B88A))`. Implementer MAY additionally override tonal roles (surfaceVariant, inversePrimary, etc.) if visual review demands it; the brand roles above are the spec-pinned values.

- [ ] **Step 3: Create `ClassicDarkColorScheme.kt`**

Build as `darkColorScheme().copy(primary = Color(0xFFFFCC00), onPrimary = Color(0xFF0B1226), secondary = Color(0xFFFFD54F), background = Color(0xFF0B1226), surface = Color(0xFF152042), outline = Color(0xFF2B3A66))`.

- [ ] **Step 4: Create `NoirLightColorScheme.kt`**

Build as `lightColorScheme().copy(primary = Color(0xFF1C1C1C), onPrimary = Color(0xFFFFFFFF), secondary = Color(0xFF4A6FA5), background = Color(0xFFEEEEEE), surface = Color(0xFFFFFFFF), outline = Color(0xFFBDBDBD))`.

- [ ] **Step 5: Create `NoirDarkColorScheme.kt`**

Build as `darkColorScheme().copy(primary = Color(0xFFE6E6E6), onPrimary = Color(0xFF000000), secondary = Color(0xFF7FB2E8), background = Color(0xFF000000), surface = Color(0xFF121212), outline = Color(0xFF2E2E2E))`.

- [ ] **Step 6: Verify everything compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/garyfimo/batmancollection/ui/theme/
git commit -m "$(cat <<'EOF'
feat(theme): add BatmanPalette enum and four palette ColorSchemes

Hand-authored brand roles for Classic (midnight + bat-yellow) and
Noir (monochrome + cold accent), each with light and dark variants.
EOF
)"
```

---

## Task 3: `PalettePreferences` repository backed by DataStore

**Files:**
- Create: `app/src/main/java/com/garyfimo/batmancollection/preferences/domain/PalettePreferences.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/preferences/data/DataStorePalettePreferences.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/preferences/di/PreferencesModule.kt`
- Create: `app/src/test/java/com/garyfimo/batmancollection/preferences/data/DataStorePalettePreferencesTest.kt`
- Modify: `app/src/main/java/com/garyfimo/batmancollection/BatmanApp.kt` (register `preferencesModule`)
- Modify: `app/build.gradle.kts` (add unit-test dependencies)

**Interfaces:**
- Consumes: `BatmanPalette` (Task 2), `androidx.datastore:datastore-preferences` (Task 1).
- Produces:
  - ```kotlin
    interface PalettePreferences {
        fun palette(): Flow<BatmanPalette>
        suspend fun setPalette(palette: BatmanPalette)
        suspend fun initialPalette(): BatmanPalette
    }
    ```
  - `val preferencesModule: Module` (Koin), binds `PalettePreferences` to `DataStorePalettePreferences`.

- [ ] **Step 1: Add unit-test dependencies to `app/build.gradle.kts`**

Append to `dependencies { … }`:

```kotlin
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.turbine)
testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 2: Create `PalettePreferences` interface**

File `preferences/domain/PalettePreferences.kt` — single top-level interface with the three members in the Produces block above, exactly those signatures. KDoc one-liner describing each.

- [ ] **Step 3: Write failing test `DataStorePalettePreferencesTest.palette emits CLASSIC on fresh install`**

Fixture: build a `DataStore<Preferences>` with `PreferenceDataStoreFactory.create(scope = testScope, produceFile = { tempFolder.newFile("theme-prefs.preferences_pb") })` (use JUnit `@get:Rule val tempFolder = TemporaryFolder()`). Construct `DataStorePalettePreferences(dataStore)`. Assert `preferences.palette().first() == BatmanPalette.CLASSIC`.

- [ ] **Step 4: Write failing test `DataStorePalettePreferencesTest.setPalette NOIR then palette emits NOIR`**

Same fixture. `preferences.setPalette(BatmanPalette.NOIR)`, then `preferences.palette().first() == BatmanPalette.NOIR`.

- [ ] **Step 5: Write failing test `DataStorePalettePreferencesTest.initialPalette returns stored NOIR after setPalette NOIR`**

Covers Review Focus #2. Same fixture. After `setPalette(NOIR)`, `preferences.initialPalette() == BatmanPalette.NOIR`.

- [ ] **Step 6: Write failing test `DataStorePalettePreferencesTest.palette emits CLASSIC when stored value is unknown`**

Covers Review Focus #1. Bypass `setPalette` and write a raw string into the DataStore directly: `dataStore.edit { it[stringPreferencesKey("palette")] = "WONDERWOMAN" }`, then assert `palette().first() == BatmanPalette.CLASSIC` and `initialPalette() == BatmanPalette.CLASSIC`.

- [ ] **Step 7: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*DataStorePalettePreferencesTest*"`
Expected: FAIL — unresolved reference `DataStorePalettePreferences`.

- [ ] **Step 8: Implement `DataStorePalettePreferences`**

File `preferences/data/DataStorePalettePreferences.kt`. Single top-level class with constructor `(private val dataStore: DataStore<Preferences>)`. Use `stringPreferencesKey("palette")`. For `palette()`: map the DataStore `data` flow, `BatmanPalette.entries.firstOrNull { it.name == stored } ?: BatmanPalette.CLASSIC`. For `setPalette`: `dataStore.edit { it[key] = palette.name }`. For `initialPalette`: `palette().first()` (suspending).

- [ ] **Step 9: Create `preferencesModule`**

File `preferences/di/PreferencesModule.kt`. Single top-level `val preferencesModule = module { … }`. Inside:

```kotlin
single<DataStore<Preferences>> {
    PreferenceDataStoreFactory.create(
        produceFile = { androidContext().preferencesDataStoreFile(THEME_PREFS_NAME) },
    )
}
single<PalettePreferences> { DataStorePalettePreferences(get()) }
```

The constant `THEME_PREFS_NAME = "theme-prefs"` is private to this file.

- [ ] **Step 10: Register `preferencesModule` in `BatmanApp`**

Modify `BatmanApp.kt` — add `import com.garyfimo.batmancollection.preferences.di.preferencesModule` and add `preferencesModule` to the `modules(…)` call.

- [ ] **Step 11: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*DataStorePalettePreferencesTest*"`
Expected: 4 tests PASS.

- [ ] **Step 12: Commit**

```bash
git add app/build.gradle.kts \
        app/src/main/java/com/garyfimo/batmancollection/preferences/ \
        app/src/test/java/com/garyfimo/batmancollection/preferences/ \
        app/src/main/java/com/garyfimo/batmancollection/BatmanApp.kt
git commit -m "$(cat <<'EOF'
feat(preferences): add PalettePreferences backed by DataStore

New PalettePreferences repo returns CLASSIC when no value is stored
or the stored string doesn't match a known palette family.
EOF
)"
```

---

## Task 4: Rewrite `BatmanCollectionTheme` and gate `MainActivity`

**Files:**
- Modify: `app/src/main/java/com/garyfimo/batmancollection/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/garyfimo/batmancollection/MainActivity.kt`
- Modify: `app/build.gradle.kts` (add core-splashscreen dep if not already present)
- Modify: `gradle/libs.versions.toml` (add core-splashscreen catalog entry)

**Interfaces:**
- Consumes: `BatmanPalette`, the four `*ColorScheme` values (Task 2), `PalettePreferences` (Task 3).
- Produces: `@Composable fun BatmanCollectionTheme(palette: BatmanPalette = BatmanPalette.CLASSIC, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`.

- [ ] **Step 1: Add core-splashscreen to catalog + build**

Add to `libs.versions.toml` `[versions]`:

```toml
coreSplashscreen = "1.0.1"
```

Add to `[libraries]`:

```toml
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }
```

Add to `app/build.gradle.kts` dependencies:

```kotlin
implementation(libs.androidx.core.splashscreen)
```

- [ ] **Step 2: Rewrite `Theme.kt`**

Replace the file's body. Add `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)` at the top. Single top-level composable `BatmanCollectionTheme`. Body:

1. Resolve `colorScheme` from `(palette, darkTheme)` via a `when`:
   - `CLASSIC, false → ClassicLightColorScheme`
   - `CLASSIC, true  → ClassicDarkColorScheme`
   - `NOIR, false    → NoirLightColorScheme`
   - `Noir, true     → NoirDarkColorScheme`
2. Side-effect: inside a `SideEffect { }`, cast `LocalView.current.context` to `Activity` and call `enableEdgeToEdge(statusBarStyle = …, navigationBarStyle = …)` built from `SystemBarStyle.auto(lightScrim = …, darkScrim = …, detectDarkMode = { darkTheme })` using `colorScheme.surface.toArgb()`-derived scrims. Rationale: pulls bar-icon tinting into the theme so dark palettes don't leave invisible icons.
3. Wrap content in `MaterialExpressiveTheme(colorScheme = colorScheme, content = content)`.

- [ ] **Step 3: Rewrite `MainActivity`**

Replace body. New flow in `onCreate`:

```kotlin
val splash = installSplashScreen()
super.onCreate(savedInstanceState)

val preferences: PalettePreferences by inject()  // via org.koin.android.ext.android.inject
var initialPalette: BatmanPalette? = null
splash.setKeepOnScreenCondition { initialPalette == null }

lifecycleScope.launch {
    initialPalette = preferences.initialPalette()
    setContent {
        val palette by preferences.palette()
            .collectAsStateWithLifecycle(initialValue = initialPalette!!)
        BatmanCollectionTheme(palette = palette) {
            BatmanNavDisplay()
        }
    }
}
```

The `enableEdgeToEdge()` call is no longer in `MainActivity` — the theme owns it now.

- [ ] **Step 4: Verify app still builds and all existing tests pass**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing unit tests PASS.

Then run the existing instrumented smoke:

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*BatmanNavDisplayTest*"`
Expected: PASS (zero-arg `BatmanCollectionTheme { }` calls in tests still compile thanks to the parameter default).

- [ ] **Step 5: Manual verification**

Launch app on a device/emulator. The first visible frame renders under one of the four palettes (Classic Light by default on a light-system device). Status bar icons are legible on every system-mode toggle.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
        app/src/main/java/com/garyfimo/batmancollection/ui/theme/Theme.kt \
        app/src/main/java/com/garyfimo/batmancollection/MainActivity.kt
git commit -m "$(cat <<'EOF'
feat(theme): wrap app in MaterialExpressiveTheme with palette parameter

BatmanCollectionTheme now takes a BatmanPalette with a CLASSIC default
so existing call sites and androidTests keep compiling. MainActivity
reads the stored palette behind the system splash screen so the first
frame renders the user's choice, not the default. System-bar styling
moved inside the theme so every ColorScheme gets correct bar-icon tinting.
EOF
)"
```

---

## Task 5a: `SettingsViewModel` + MVI types + analytics + DI

**Files:**
- Create: `app/src/main/java/com/garyfimo/batmancollection/settings/presentation/SettingsUiState.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/settings/presentation/SettingsIntent.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/settings/presentation/SettingsViewModel.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/settings/presentation/SettingsAnalyticEvents.kt`
- Create: `app/src/main/java/com/garyfimo/batmancollection/settings/di/SettingsModule.kt`
- Create: `app/src/test/java/com/garyfimo/batmancollection/settings/presentation/SettingsViewModelTest.kt`
- Modify: `app/src/main/java/com/garyfimo/batmancollection/BatmanApp.kt` (register `settingsModule`)
- Modify: `integration/analytics/src/main/java/com/garyfimo/batmancollection/analytics/domain/Events.kt`
- Modify: `integration/analytics/src/main/java/com/garyfimo/batmancollection/analytics/domain/Params.kt`

**Interfaces:**
- Consumes: `PalettePreferences`, `BatmanPalette`, `Analytics`, `Events`, `Params`.
- Produces:
  - `data class SettingsUiState(val selected: BatmanPalette = BatmanPalette.CLASSIC)`
  - `sealed interface SettingsIntent { data class PaletteSelected(val palette: BatmanPalette) : SettingsIntent }`
  - `class SettingsViewModel(preferences: PalettePreferences, analyticEvents: SettingsAnalyticEvents) : ViewModel` with `val state: StateFlow<SettingsUiState>` and `fun onIntent(intent: SettingsIntent)`.
  - `class SettingsAnalyticEvents(analytics: Analytics) { fun onPaletteChanged(from: BatmanPalette, to: BatmanPalette) }`
  - `val settingsModule: Module` (binds `SettingsAnalyticEvents` as factory, `SettingsViewModel` via `viewModelOf`).

- [ ] **Step 1: Add analytic event + param constants**

Modify `Events.kt` — add `const val PALETTE_CHANGED = "palette_changed"`.
Modify `Params.kt` — add `const val PALETTE_FAMILY_FROM = "palette_family_from"` and `const val PALETTE_FAMILY_TO = "palette_family_to"`.

- [ ] **Step 2: Create `SettingsUiState.kt`**

One top-level data class. `selected: BatmanPalette = BatmanPalette.CLASSIC`.

- [ ] **Step 3: Create `SettingsIntent.kt`**

One top-level `sealed interface SettingsIntent`. Nested `data class PaletteSelected(val palette: BatmanPalette) : SettingsIntent`. Nested subtypes stay in the file per the project rule.

- [ ] **Step 4: Create `SettingsAnalyticEvents.kt`**

One top-level class. Single public method `fun onPaletteChanged(from: BatmanPalette, to: BatmanPalette)` that calls `analytics.track(Events.PALETTE_CHANGED, mapOf(Params.PALETTE_FAMILY_FROM to from.name.lowercase(), Params.PALETTE_FAMILY_TO to to.name.lowercase(), Params.SCREEN_NAME to SCREEN_NAME))`. Companion `const val SCREEN_NAME = "settings"`.

- [ ] **Step 5: Write failing test `SettingsViewModelTest.state mirrors stored palette`**

Fixture: `preferences = mockk<PalettePreferences>()`, `every { preferences.palette() } returns flowOf(BatmanPalette.NOIR)`. Construct `SettingsViewModel(preferences, mockk(relaxed = true))`. Advance dispatcher. Assert `vm.state.value.selected == BatmanPalette.NOIR`.

- [ ] **Step 6: Write failing test `SettingsViewModelTest.PaletteSelected writes to preferences and tracks analytics`**

`every { preferences.palette() } returns flowOf(BatmanPalette.CLASSIC)`. `coEvery { preferences.setPalette(any()) } returns Unit`. Call `vm.onIntent(SettingsIntent.PaletteSelected(BatmanPalette.NOIR))`. `advanceUntilIdle()`. Verify `coVerify { preferences.setPalette(BatmanPalette.NOIR) }` and `verify { analyticEvents.onPaletteChanged(BatmanPalette.CLASSIC, BatmanPalette.NOIR) }`.

- [ ] **Step 7: Write failing test `SettingsViewModelTest.selecting the already-selected family does not emit palette_changed`**

Covers Review Focus #3. `every { preferences.palette() } returns flowOf(BatmanPalette.CLASSIC)`. Call `vm.onIntent(SettingsIntent.PaletteSelected(BatmanPalette.CLASSIC))`. `advanceUntilIdle()`. Verify `coVerify(exactly = 0) { preferences.setPalette(any()) }` and `verify(exactly = 0) { analyticEvents.onPaletteChanged(any(), any()) }`.

- [ ] **Step 8: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*SettingsViewModelTest*"`
Expected: FAIL — unresolved reference `SettingsViewModel`.

- [ ] **Step 9: Implement `SettingsViewModel`**

Collect `preferences.palette()` into `state: StateFlow<SettingsUiState>` via `.map { SettingsUiState(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())`. `onIntent` branches on `PaletteSelected`: if the requested palette equals `state.value.selected`, return early; otherwise `viewModelScope.launch { preferences.setPalette(intent.palette) }` and call `analyticEvents.onPaletteChanged(from = state.value.selected, to = intent.palette)`.

- [ ] **Step 10: Create `settings/di/SettingsModule.kt`**

Single top-level `val settingsModule = module { factoryOf(::SettingsAnalyticEvents); viewModelOf(::SettingsViewModel) }`.

- [ ] **Step 11: Register `settingsModule` in `BatmanApp`**

Add `import com.garyfimo.batmancollection.settings.di.settingsModule` and add `settingsModule` to the `modules(…)` call.

- [ ] **Step 12: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*SettingsViewModelTest*"`
Expected: 3 tests PASS.

- [ ] **Step 13: Commit**

```bash
git add integration/analytics/ \
        app/src/main/java/com/garyfimo/batmancollection/settings/ \
        app/src/test/java/com/garyfimo/batmancollection/settings/ \
        app/src/main/java/com/garyfimo/batmancollection/BatmanApp.kt
git commit -m "$(cat <<'EOF'
feat(settings): add SettingsViewModel and palette_changed analytics

Selecting the currently-selected family is a no-op — no write, no
analytic — so the picker can't produce spurious events.
EOF
)"
```

---

## Task 5b: `SettingsScreen` composable + navigation + Home gear icon

**Files:**
- Create: `app/src/main/java/com/garyfimo/batmancollection/settings/presentation/SettingsScreen.kt`
- Create: `app/src/androidTest/java/com/garyfimo/batmancollection/SettingsScreenTest.kt`
- Modify: `app/src/main/java/com/garyfimo/batmancollection/navigation/NavKeys.kt`
- Modify: `app/src/main/java/com/garyfimo/batmancollection/navigation/BatmanNavDisplay.kt`
- Modify: `feature/figure/src/main/java/com/garyfimo/batmancollection/figure/presentation/home/HomeScreen.kt` (add `onSettings` param + gear `IconButton`)
- Modify: `app/src/androidTest/java/com/garyfimo/batmancollection/HomeScreenTest.kt` (only if a tag collision needs re-anchoring; otherwise leave alone)

**Interfaces:**
- Consumes: `SettingsViewModel` (Task 5a), `BatmanPalette`, `SettingsIntent`.
- Produces:
  - `@Composable fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel())`
  - `@Serializable data object SettingsKey : NavKey`
  - `HomeScreen(..., onSettings: () -> Unit = {})` — new last parameter, with default `{}` so existing tests that don't pass it stay compiling.
  - New test tags on `HomeScreen`: `const val TAG_HOME_SETTINGS = "home.settings"`.
  - New test tags on `SettingsScreen`: `TAG_SETTINGS_ROOT`, `TAG_SETTINGS_BACK`, `TAG_SETTINGS_ROW_CLASSIC`, `TAG_SETTINGS_ROW_NOIR`, `TAG_SETTINGS_RADIO_CLASSIC`, `TAG_SETTINGS_RADIO_NOIR`.

- [ ] **Step 1: Add `SettingsKey` to `NavKeys.kt`**

Append:

```kotlin
@Serializable
data object SettingsKey : NavKey
```

- [ ] **Step 2: Add `onSettings` param + gear icon to `HomeScreen`**

Edit `HomeScreen.kt` function signature to add `onSettings: () -> Unit = {}` as the last parameter. In `TopAppBar`'s `actions` lambda, inside the `if (!isSearching) { … }` block, add an `IconButton(onClick = onSettings, modifier = Modifier.testTag(TAG_HOME_SETTINGS))` with `Icons.Filled.Settings` icon, contentDescription "Settings". Place it between the sort icon block and the sign-in/out block. Add `const val TAG_HOME_SETTINGS = "home.settings"` at file bottom alongside the other `TAG_HOME_*` constants.

- [ ] **Step 3: Wire `SettingsKey` into `BatmanNavDisplay`**

In `HomeScreen(...)` call-site, add `onSettings = { backStack.add(SettingsKey) }`. Below the existing `entry<AddWishKey>` block, add:

```kotlin
entry<SettingsKey> {
    SettingsScreen(
        onBack = { backStack.removeLastOrNull() },
    )
}
```

Add imports for `SettingsKey` and `SettingsScreen`.

- [ ] **Step 4: Implement `SettingsScreen`**

Single top-level composable. Collect `viewModel.state` via `collectAsStateWithLifecycle()`. Scaffold with `TopAppBar` (title `"Settings"`, back `IconButton` using `Icons.AutoMirrored.Filled.ArrowBack`, tagged `TAG_SETTINGS_BACK`). In the content column: a section header `Text("Appearance", style = MaterialTheme.typography.labelLarge)` and two `ListItem`s. Each `ListItem`:

- Headline: `"Classic"` or `"Noir"`.
- Supporting: `"Midnight blue and bat-yellow"` for Classic, `"Monochrome with a cold accent"` for Noir.
- Trailing: a `RadioButton(selected = state.selected == PALETTE, onClick = null)` tagged `TAG_SETTINGS_RADIO_CLASSIC` / `TAG_SETTINGS_RADIO_NOIR`.
- Modifier: `Modifier.clickable { viewModel.onIntent(SettingsIntent.PaletteSelected(PALETTE)) }.testTag(TAG_SETTINGS_ROW_CLASSIC)` etc.

Root column gets `Modifier.padding(scaffoldPadding).testTag(TAG_SETTINGS_ROOT)`. All `TAG_SETTINGS_*` constants declared at file bottom.

- [ ] **Step 5: Write failing test `SettingsScreenTest.shows both palette options`**

Launch `SettingsScreen` via `ComposeTestRule` with a test Koin module binding `PalettePreferences` to a fake returning `flowOf(CLASSIC)`. Assert both `TAG_SETTINGS_ROW_CLASSIC` and `TAG_SETTINGS_ROW_NOIR` are displayed; the Classic radio is `isSelected()` and the Noir radio `isNotSelected()`.

- [ ] **Step 6: Write failing test `SettingsScreenTest.tapping Noir row selects it`**

Same fixture; a `mockk<PalettePreferences>(relaxed = true)` captures `setPalette`. `onNodeWithTag(TAG_SETTINGS_ROW_NOIR).performClick()`. Verify `coVerify { preferences.setPalette(BatmanPalette.NOIR) }`.

- [ ] **Step 7: Write failing test `SettingsScreenTest.navigating to Settings and back preserves HomeScreen filter selection`**

Covers Review Focus #4. Launch full `BatmanNavDisplay` in the test. Apply a type filter on Home (click `TAG_HOME_FILTER_PREFIX + <some type>`). Click `TAG_HOME_SETTINGS`. Click the back button in Settings (`TAG_SETTINGS_BACK`). Assert the previously-applied filter chip is still selected on Home.

- [ ] **Step 8: Write failing test `SettingsScreenTest.toggling system dark mode on settings keeps NOIR selected`**

Covers Review Focus #5. Launch `SettingsScreen` wrapped in `BatmanCollectionTheme(palette = NOIR, darkTheme = false) { … }`. Assert Noir radio selected. Recompose wrapping with `darkTheme = true`. Assert Noir radio still selected (family-state survives light/dark toggle). Device-level dark-mode is simulated by the `darkTheme` parameter swap.

- [ ] **Step 9: Run tests to verify they fail**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*SettingsScreenTest*"`
Expected: FAIL — `SettingsScreen` or test tags unresolved.

- [ ] **Step 10: Iterate on screen implementation until tests pass**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*SettingsScreenTest*"`
Expected: 4 tests PASS.

- [ ] **Step 11: Verify existing tests all still pass**

Run: `./gradlew :app:connectedDebugAndroidTest :app:testDebugUnitTest` plus the per-feature tests: `./gradlew :feature:login:test :feature:figure:test :feature:wishlist:test`.
Expected: all green. `HomeScreenTest` works unchanged because `onSettings` has a default.

- [ ] **Step 12: Manual verification**

Launch the app, tap the new gear icon, pick Noir, back out, confirm the UI is now in Noir. Kill and relaunch — Noir persists across cold start. Toggle system dark mode — Noir stays, bars re-tint.

- [ ] **Step 13: Commit**

```bash
git add app/src/main/java/com/garyfimo/batmancollection/navigation/ \
        app/src/main/java/com/garyfimo/batmancollection/settings/presentation/SettingsScreen.kt \
        app/src/androidTest/java/com/garyfimo/batmancollection/SettingsScreenTest.kt \
        feature/figure/src/main/java/com/garyfimo/batmancollection/figure/presentation/home/HomeScreen.kt
git commit -m "$(cat <<'EOF'
feat(settings): add SettingsScreen with palette picker, reachable from Home

Gear icon on HomeScreen pushes a new SettingsKey. The Settings screen
renders two ListItems (Classic / Noir), each with a radio, and selecting
one writes through PalettePreferences so the whole app recolors.
EOF
)"
```

---

## Per-screen Expressive passes — intentionally omitted

The spec's commit plan originally listed per-screen commits 3–7 (`LoginScreen`, `HomeScreen`, `DetailsScreen`, `AddFigureScreen`, `WishlistScreen + AddWishScreen`). The spec's own **Component swap matrix** also marks every widget on those screens as "Keep" — the only call-site change is `MaterialTheme → MaterialExpressiveTheme`, which happens inside `Theme.kt` in Task 4 and takes effect everywhere automatically.

Those per-screen commits would therefore be no-op diffs. **Not included in this plan.**

If, after visual review, a specific screen looks wrong under Expressive (a shape, a type scale, or a motion default that clashes), add a focused follow-up task scoped to that single fix — not a whole-screen rewrite.
