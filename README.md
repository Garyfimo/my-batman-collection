# my-batman-collection

Android app template.

## Stack

- Jetpack Compose + Material 3
- Navigation 3 (`androidx.navigation3`)
- Koin (DI) with Compose integration
- Ktor client (OkHttp engine) + kotlinx.serialization
- Unit tests: JUnit 4, MockK, Turbine, coroutines-test
- UI tests: Compose UI Test, Espresso, MockK

## First-time setup

The `gradle-wrapper.jar` binary is intentionally not committed. Generate it with
either of:

- Open the project in Android Studio — it will populate the wrapper on sync.
- From a shell with Gradle installed:
  ```
  gradle wrapper --gradle-version 8.11.1
  ```

After that, build and test with:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
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
