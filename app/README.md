# AgroGem app workspace

This directory contains the actual AgroGem application. It is a Kotlin Multiplatform project powered by Compose Multiplatform, with shared UI in `composeApp` and an Xcode host app in `iosApp`.

## Project layout

- [`composeApp`](./composeApp/) — main multiplatform application module
- [`composeApp/src/commonMain`](./composeApp/src/commonMain/kotlin) — shared UI, navigation, state, and presentation logic
- [`composeApp/src/commonTest`](./composeApp/src/commonTest/kotlin) — shared tests for navigation and view models
- [`composeApp/src/androidMain`](./composeApp/src/androidMain/kotlin) — Android-specific entry points
- [`composeApp/src/iosMain`](./composeApp/src/iosMain/kotlin) — iOS-specific entry points
- [`composeApp/src/wasmJsMain`](./composeApp/src/wasmJsMain/kotlin) — browser preview target
- [`iosApp`](./iosApp/) — Xcode host application for running on iOS

## Current product surface

The shared app currently contains:

- A dashboard with health indicators and recent analyses
- A camera capture flow
- An analysis progress screen
- A map/risk overview screen
- A diagnosis/report screen
- Shared navigation driven from `commonMain`

## Core architecture

- `App.kt` is the shared app entry point.
- `ui/AppShell.kt` owns the scaffold and navigation controller.
- `navigation/` centralizes routes and the nav host.
- `ui/screens/<feature>/` groups UI, `UiState`, and `ViewModel` for each feature.
- `ui/components/` holds reusable UI building blocks.
- `theme/` holds shared design tokens and Compose theme setup.

## Commands

Run all commands from this `app/` directory.

### Explore available tasks

```sh
./gradlew tasks --all
```

### Run verification

```sh
./gradlew :composeApp:allTests
./gradlew :composeApp:lint
```

### Android

```sh
./gradlew :composeApp:assembleDebug
```

### WebAssembly preview

```sh
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### iOS

Open [`iosApp`](./iosApp/) in Xcode and run from there.

## Testing notes

- Shared tests use `kotlin.test`
- Existing tests focus on shared navigation and screen view models
- Add tests in `composeApp/src/commonTest` when changing shared behavior

## Agent-specific documentation

If you are using a coding agent, also read [`AGENTS.md`](./AGENTS.md) in this directory. It contains the operational guidance, conventions, and validated commands for this module.
