# Todo List Android demo

A simple Jetpack Compose app using MVVM, a repository, Retrofit, and the backend in `../../visual-programming-backend-2026`.

## Project structure

- `views/` contains the Compose screens, shared UI components, and Navigation Compose graph.
- `navigation/` defines the serializable, type-safe screen destinations.
- `viewmodels/` contains one ViewModel per feature: `TodoViewModel` serves list, detail, add, and edit screens, sharing the list back-stack entry's instance. An app-scoped `AuthViewModel` owns login, registration, logout, and authentication messages; `AppViewModel` owns the token and session version, restored from app-private Android `SharedPreferences`. `RequestViewModel` provides shared request handling.
- ViewModels expose UI state as read-only `StateFlow`s backed by private mutable flows; screens collect them with lifecycle awareness and update form fields through ViewModel methods.
- `models/` contains the API request and response DTOs.
- `services/` defines separate Retrofit authentication and todo endpoints; `repositories/` provides those operations to the ViewModels.

## Run

1. Set up and run the backend as described in its `docs/api.md` (PostgreSQL, migrations, and `pnpm dev`).
2. Open this project in Android Studio and run the debug app on an Android emulator. The default API address is `http://10.0.2.2:3000/`, which reaches the development server on the emulator's host computer.
3. Register with an email and a password of at least 9 characters, or log in with an existing account. Open a todo to see its details, edit or delete it; use **Add todo** to create one.
After a successful add, edit, or delete, the previous screen shows the server's result immediately and refreshes in the background.

For a physical device, start the backend on an address reachable from the device, connect both to the same network, and supply the computer's LAN address when building, including the trailing slash:

```sh
./gradlew :app:assembleDebug -PapiBaseUrl=http://192.168.1.10:3000/
```

Replace the example address with the server's actual address. The debug app permits HTTP for local development; use an HTTPS backend URL for a release build. HTTP exposes credentials and tokens to the network, so use a trusted development network and demo credentials.

The token and session version survive app restarts. A restored session opens the todo list directly. Login and registration save both values together; logout or a server-reported expired session removes the saved token. Passwords are never persisted. Tokens still expire after one hour: persistence does not renew them, and a backend 401 returns the app to login. There is no offline mode: todo operations require the backend.

## Configuration and session storage

- **API URL:** Gradle's `apiBaseUrl` property generates `BuildConfig.API_BASE_URL`. `BuildConfig` is Android's standard, typed bridge from build configuration into Kotlin, supports build variants, and needs no runtime file parser. Use `-PapiBaseUrl=...` as above, or `apiBaseUrl=...` in your user-level `~/.gradle/gradle.properties`.
- **Why not `.env`?** Android does not automatically load dotenv files. A Gradle plugin or custom loader would have to read the file and expose its values to the app, often through `BuildConfig` anyway. A `.env` file can be a build input, not a replacement for runtime session storage. Bundling it as an asset does not protect its contents.
- **Runtime session:** `AppViewModel.Factory` opens `auth_session` preferences using the application context and `MODE_PRIVATE`; no Activity context is retained. This is a small, native key-value store requiring no additional runtime dependency. `apply()` updates memory immediately and schedules disk writes asynchronously. The session version remains after logout so subsequent logins keep advancing it. Both Android backup rule formats exclude `auth_session.xml`, including cloud backup and device transfer.
- **Security and best fit:** `SharedPreferences` is deliberately the lightweight choice for this demo, not encrypted credential storage. Neither `.env` nor `BuildConfig` keeps packaged values secret; keep backend secrets on the server. For production credentials requiring encryption at rest, encrypt the token with an Android Keystore-backed key and persist only ciphertext. For a new general-purpose asynchronous preferences store, Android recommends DataStore over `SharedPreferences`; DataStore alone does not encrypt tokens.

References: [Android shared preferences and DataStore guidance](https://developer.android.com/training/data-storage/shared-preferences), [custom BuildConfig fields](https://developer.android.com/build/gradle-tips#share-custom-fields-and-resource-values-with-your-app-code), [Android Keystore](https://developer.android.com/privacy-and-security/keystore).

Run the local API contract tests with `./gradlew :app:testDebugUnitTest` (`gradlew.bat` on Windows). The ViewModel tests use Robolectric to exercise session restoration, replacement, and removal on logout or expiry. `NavigationTest` also covers opening a persisted session directly in the todo list; run it on a device with compatible AndroidX Test tooling using `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.todolistproject.NavigationTest`.
