# Expressive Redesign — Design

**Date:** 2026-11-25
**Branch:** `expressive-redesign` (cut from `main` at `5774799`)
**Author:** Gary Figuerola (with Claude)

## Intent

Switch the entire app over to **Material 3 Expressive** and give it a visual identity by introducing a **user-selectable Batman-flavored palette**. Today the theme is the default `lightColorScheme()` / `darkColorScheme()` with zero customization; this project replaces that with Material Expressive wrappers and two curated palette families the user picks between in Settings.

### Why

- The current theme is undifferentiated — the app looks like a Compose demo.
- Compose BOM `2026.09.00` is new enough to use Material Expressive, and the author wants to put the new theme / motion scheme / components into practice.
- A small palette-picker is itself a nice product feature for a collection app whose users have strong aesthetic opinions about the character.

## Goals

- All six screens render under `MaterialExpressiveTheme`.
- Two palette families — **Classic** (midnight-blue + bat-yellow) and **Noir** (monochrome + cold accent) — each with a light and a dark `ColorScheme`.
- End user picks the *family* in a Settings surface; the device's system dark-mode decides which of that family's two schemes applies.
- Pick is remembered across app launches.
- All existing behavior (navigation, MVI flow, Firestore interactions, analytics events, test tags) is preserved.
- Every tested behavior still passes without test changes beyond trivially re-targeting a widget type.

## Non-goals

- **No layout changes.** Same screens, same compositions, same navigation graph. We are not swapping `TopAppBar` for `FloatingToolbar`, not introducing `WideNavigationRail` or `FABMenu`, not reshaping Home's top bar.
- **No motion-system rewrite.** We adopt whatever Expressive's default `MotionScheme` gives us; we don't tune custom motion specs. The existing shared-element popup on Home stays as-is.
- **No new features.** No new screens besides the Settings surface that houses the picker.
- **No dependency upgrades** beyond what Expressive requires (which, at this BOM, is nothing — Expressive is in `androidx.compose.material3` already).

## Anti-goals (would make this a failure even if it technically works)

- A noticeably *worse*-looking app under either palette than today's defaults.
- Adding so many `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` annotations that files become noisy — we'll annotate at the file level where it applies to most of the file.
- Leaking the chosen palette across tests (persistence state bleeding between androidTest runs).
- Blowing the module layout with a special "theme" module nobody needs.

## Constraints

- Compose BOM is `2026.09.00`; Expressive APIs are available but still annotated experimental. Opt-in is per-file.
- Clean Architecture per feature (`domain` / `data` / `presentation`); the theme lives in `:app` since it crosses every feature.
- Koin for DI; new DataStore access needs a module in `:app`.
- One top-level declaration per Kotlin file (project rule).
- No Co-Authored-By / Claude Code footers on commits.
- Existing androidTest harness relies on test tags — none may change.

## Approaches considered

Scope decision: *pure component swap* (not a redesign). Within that, three sequencing approaches:

1. **Big-bang swap.** Single commit rewrites every screen + theme + DataStore + picker. Rejected — review becomes a slog; if one screen looks wrong, nothing to bisect.
2. **Layered: infra first, then screens.** ✓ **Chosen.** Commit 1 lands the theme + palette + DataStore + picker while every screen still uses its existing widgets, so the app runs. Commits 2–7 replace each screen's widgets one at a time. Each commit is a working binary.
3. **Screen-by-screen from day one.** Tangles palette bikeshedding with per-screen widget work. Rejected.

Palette-picker decision: between **X** (both families selectable) and **Y** (ship only Noir, no picker). Chose **X** — keeps both design directions live and makes the Settings surface itself a visible new feature.

## The how, as decided

### Palette definitions

Four `ColorScheme`s, hand-authored from the mockup hexes and then expanded through Material's tonal generator. These specific role values are the brand roles; all other roles come from tonal expansion:

| Role       | Classic Light | Classic Dark | Noir Light | Noir Dark  |
|------------|---------------|--------------|------------|------------|
| primary    | `#122145`     | `#FFCC00`    | `#1C1C1C`  | `#E6E6E6`  |
| onPrimary  | `#FFD54F`     | `#0B1226`    | `#FFFFFF`  | `#000000`  |
| secondary  | `#FFCC00`     | `#FFD54F`    | `#4A6FA5`  | `#7FB2E8`  |
| background | `#F6EFD8`     | `#0B1226`    | `#EEEEEE`  | `#000000`  |
| surface    | `#FFFFFF`     | `#152042`    | `#FFFFFF`  | `#121212`  |
| outline    | `#C7B88A`     | `#2B3A66`    | `#BDBDBD`  | `#2E2E2E`  |

Each full `ColorScheme` is built by seeding Material's tonal expansion with `primary` + `secondary`, then overwriting the roles above with the brand values.

### Theme model

- `BatmanPalette` enum in `:app` (`CLASSIC`, `NOIR`) — the user-selectable family.
- `BatmanCollectionTheme(palette: BatmanPalette = BatmanPalette.CLASSIC, content: @Composable () -> Unit)` wraps `MaterialExpressiveTheme`. Picks light-vs-dark internally from `isSystemInDarkTheme()` as today. **The default value on `palette` is deliberate**: every existing `androidTest` calls `BatmanCollectionTheme { ... }` with no args, and the goal commits to "no test changes beyond trivially re-targeting a widget type." A default keeps those call sites compiling.
- Each palette has its own `ColorScheme` pair in its own file (one top-level declaration per Kotlin file): `ClassicLightColorScheme.kt`, `ClassicDarkColorScheme.kt`, `NoirLightColorScheme.kt`, `NoirDarkColorScheme.kt`.
- `BatmanCollectionTheme` also drives system-bar styling (status + navigation bars) via `enableEdgeToEdge(…)`-compatible `SystemBarStyle` values derived from the resolved `ColorScheme.surface` / `ColorScheme.background`. Without this, dark-mode palettes like Classic-Dark (`#0B1226`) and Noir-Dark (`#000`) risk invisible status-bar icons under the system default. Implemented inside the theme composable so each screen inherits correct bars automatically.

### Persistence

- `androidx.datastore:datastore-preferences` (adds one version-ref + one library entry to `libs.versions.toml`).
- Preferences DataStore named `theme-prefs` with a single string key `palette`.
- Repository interface `PalettePreferences` in `:app`, Koin-wired:
  - `fun palette(): Flow<BatmanPalette>` (defaults to `CLASSIC` for fresh installs).
  - `suspend fun setPalette(p: BatmanPalette)`.
  - `suspend fun initialPalette(): BatmanPalette` — a one-shot read used at app start to avoid a first-frame flash.
- Consumed in `MainActivity`: the activity's `onCreate` suspends briefly (inside `lifecycleScope.launch { ... setContent { ... } }` or via `installSplashScreen().setKeepOnScreenCondition { !ready }`) to read `initialPalette()` before `setContent`. After that, the live `Flow` is collected via `collectAsStateWithLifecycle(initialValue = initialPalette)` so subsequent picker changes animate in without re-reading.
- Rationale: a naïve `collectAsStateWithLifecycle()` would emit its seed (`CLASSIC`) for one frame before the real stored value arrives. A Noir user would see a Classic flash on every cold start. Prefetching the initial value before `setContent` eliminates it; a splash gate is the clean Android idiom.

### Picker surface

A dedicated `SettingsScreen` reachable from a gear icon in `HomeScreen`'s `TopAppBar`. Lives in `:app` (not a new feature module) because settings is a cross-cutting app concern and the palette state already lives in `:app`; a whole new feature module for a single row of UI would be excessive. Package layout, following the project's Clean Architecture convention:

- `app/.../settings/presentation/SettingsScreen.kt`
- `app/.../settings/presentation/SettingsViewModel.kt`
- `app/.../settings/presentation/SettingsUiState.kt`
- `app/.../settings/presentation/SettingsIntent.kt`
- `app/.../settings/di/SettingsModule.kt` (binds `SettingsViewModel`; `PalettePreferences` already in the `AppModule`)

Navigation wiring:

- Add `SettingsNavKey` to `app/.../navigation/NavKeys.kt`.
- Add a branch for it in `BatmanNavDisplay.kt`.
- Add an `onSettings` callback to `HomeScreen`, and a gear `IconButton` in its `TopAppBar` that calls it.

The screen itself is small — `TopAppBar` with back button and title "Settings", a section header "Appearance", and two selectable rows (Classic / Noir) using `ListItem` with a trailing `RadioButton`. Room to grow later if more preferences appear.

### Component swap matrix

| Current widget             | Expressive action                                                                                 |
|----------------------------|---------------------------------------------------------------------------------------------------|
| `MaterialTheme { … }`      | → `MaterialExpressiveTheme { … }` (the one meaningful call-site change)                           |
| `TopAppBar`                | Keep. Expressive provides new defaults automatically through the theme.                           |
| `Scaffold`                 | Keep.                                                                                             |
| `FilterChip`               | Keep. (`ToggleButton` is tempting but changes layout rhythm — out of scope here.)                 |
| `FloatingActionButton`     | Keep. (`FABMenu` is a layout change — out of scope.)                                              |
| `Button`, `TextButton`     | Keep.                                                                                             |
| `OutlinedTextField`, `TextField` | Keep.                                                                                       |
| `DropdownMenu`, `DropdownMenuItem` | Keep.                                                                                     |
| `ListItem`                 | Keep.                                                                                             |
| `SnackbarHost`             | Keep.                                                                                             |
| `CircularProgressIndicator`| Keep. (Expressive has a new wavy variant — out of scope; add later if desired.)                   |

So the actual *import/call-site* change per screen is small: `MaterialTheme` → `MaterialExpressiveTheme` at the root, plus `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` on files that end up calling an Expressive-only API. The *visible* change comes from the Expressive theme's new defaults — shape, type scale, motion — flowing through the existing widgets.

### Commit plan (approach B, 7 commits)

1. **Theme infra.** New palette files, `BatmanCollectionTheme` rewritten for Expressive, `PalettePreferences` + DataStore + Koin module, `MainActivity` wired. App still renders old widgets but under new theme. All existing tests green.
2. **Settings screen + picker wiring.** New `SettingsScreen` (VM, state, intent, DI module), `SettingsNavKey`, `BatmanNavDisplay` branch, gear icon in `HomeScreen` top bar wired to `onSettings`. Analytic event `palette_changed` added via existing `:integration:analytics`.
3. **LoginScreen** expressive swap.
4. **HomeScreen** expressive swap.
5. **DetailsScreen** expressive swap.
6. **AddFigureScreen** expressive swap.
7. **WishlistScreen + AddWishScreen** expressive swap (bundled — they share patterns).

Each commit leaves the app runnable and the test suite green. PR opens after commit 7.

## What's left to the builder

- Exact tonal expansion for the four palettes: run each `ColorScheme`'s primary+secondary through Material's tonal palette generator to fill the ~30 non-brand roles. Builder decides whether to inline the generated values or compute them at build time.
- Icon choice for the palette picker action (my proposal: `Icons.Filled.Palette`, override if there's a better glyph).
- Copy on the picker sheet: title ("Theme"? "Palette"? "Appearance"?) and family labels ("Classic" / "Noir" vs more descriptive).
- Whether `palette_changed` analytic takes `from` + `to` params or just `to`.

## Builder-check calls made by Claude (please verify while reading)

- `BatmanCollectionTheme` gets a default `palette = BatmanPalette.CLASSIC` so existing zero-arg test call sites keep compiling without edits.
- First-frame flash prevented by prefetching the stored palette via `initialPalette()` before `setContent` (using the system splash screen as a brief gate), rather than letting `collectAsStateWithLifecycle` flash the default.
- System-bar styling moved inside `BatmanCollectionTheme` so each resolved `ColorScheme` drives correct status-bar / navigation-bar icon tinting.
