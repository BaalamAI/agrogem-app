# AgroGem App

AgroGem is a Kotlin Multiplatform application built with Compose Multiplatform. The repository root is a lightweight workspace, while the actual product code lives in `app/`.

## Repository structure

- `app/` — main Kotlin Multiplatform application
- `app/composeApp/src/commonMain` — shared UI, navigation, state, and presentation logic
- `app/composeApp/src/androidMain` — Android-specific entry point and platform code
- `app/composeApp/src/iosMain` — iOS-specific entry point and platform code
- `app/composeApp/src/wasmJsMain` — WebAssembly preview target
- `app/iosApp` — Xcode host application for iOS

## Current app scope

The app is no longer the default starter template. The shared application currently includes:

- A dashboard flow with recent analysis cards and health stats
- A camera-inspired capture screen
- An analysis progress screen
- A map/risk overview screen
- A report screen for diagnosis details
- Shared navigation and screen-specific view models in `commonMain`

## Tech stack

- Kotlin Multiplatform
- Compose Multiplatform
- Android Gradle Plugin
- Gradle Kotlin DSL
- AndroidX Navigation Compose
- Kotlin `StateFlow` + `ViewModel` for screen state

## Where to start

- Humans: read `app/README.md`
- Coding agents: read `AGENTS.md`, then `app/AGENTS.md`

## Running the app

All commands below are executed from `app/`.

### Android

```sh
./gradlew :composeApp:assembleDebug
```

### WebAssembly preview

```sh
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

When the dev server starts, open the URL printed in the terminal.

### iOS

Open `app/iosApp` in Xcode and run the host app from there.

## Verification commands

From `app/`:

```sh
./gradlew :composeApp:allTests
./gradlew :composeApp:lint
```

Use targeted tasks when possible instead of broad builds.

## Agent notes

- Root-level agent guidance lives in `AGENTS.md`
- App-level agent guidance lives in `app/AGENTS.md`
- The closest `AGENTS.md` file should be treated as the source of truth for the current directory
