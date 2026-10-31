# my-batman-collection

Android app template.

## Stack

- Android Gradle Plugin 9.2.1 (Gradle 9.8.2, bundled Kotlin)
- Jetpack Compose (BOM 2026.09) + Material 3
- Navigation 3 `1.2.0` (`androidx.navigation3`)
- Koin `4.2.x` (DI) with Compose integration
- Ktor `3.6.x` client (OkHttp engine) + kotlinx.serialization
- Unit tests: JUnit 4, MockK, Turbine, coroutines-test
- UI tests: Compose UI Test, Espresso, MockK

Versions are centralized in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Build & test

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest   # needs a device/emulator
```

## Layout

```
app/src/main/java/com/garyfimo/batmancollection/
├── BatmanApp.kt            # Application + Koin bootstrap
├── MainActivity.kt
├── di/                     # Koin modules (app, network)
├── navigation/             # Nav3 keys + NavDisplay wiring
├── data/                   # Ktor API + repository
└── ui/
    ├── home/               # List screen + ViewModel
    ├── details/            # Detail screen
    └── theme/
```

## Notes

- With AGP 9, the `org.jetbrains.kotlin.android` plugin is no longer applied —
  AGP bundles Kotlin. The JVM target is set via
  `android { kotlin { compilerOptions { jvmTarget = ... } } }`.
- Material Icons is not pulled in. If you want icons, add
  `androidx.compose.material:material-icons-extended` to `app/build.gradle.kts`.
