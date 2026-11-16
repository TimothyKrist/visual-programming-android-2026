# Todo List Android demo

A simple Jetpack Compose app using MVVM, a repository, Retrofit, and the backend in `../../visual-programming-backend-2026`.

## Project structure

- `views/` contains the Compose screens, shared UI components, and Navigation Compose graph.
- `navigation/` defines the serializable, type-safe screen destinations.
- `viewmodels/` contains one ViewModel per feature: `TodoViewModel` serves list, detail, add, and edit screens; `AuthViewModel` serves login and registration. Todo screens share the list back-stack entry's instance, and authentication screens share the login entry's instance, so leaving a feature clears its state. `AppViewModel` owns the session, while `RequestViewModel` provides shared request handling.
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

Tokens live only in memory and expire after one hour. Sign in again after reopening the app or when the session expires. There is no offline mode: todo operations require the backend.

Run the local API contract tests with `./gradlew :app:testDebugUnitTest`.
