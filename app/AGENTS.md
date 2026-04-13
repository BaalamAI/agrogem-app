# AGENTS.md

## Project Overview

`app/` is the actual AgroGem product workspace. It is a Kotlin Multiplatform project built with Compose Multiplatform and Gradle Kotlin DSL.

Targets currently configured:

- Android
- iOS (`iosArm64`, `iosSimulatorArm64`)
- `wasmJs` for browser preview

The main application module is `composeApp`.

## Module Layout

- `build.gradle.kts` — top-level Gradle plugin declarations for the app workspace
- `settings.gradle.kts` — includes `:composeApp`
- `composeApp/build.gradle.kts` — KMP target configuration and dependencies
- `composeApp/src/commonMain` — shared app code
- `composeApp/src/commonTest` — shared unit tests
- `composeApp/src/androidMain` — Android-specific entry points and resources
- `composeApp/src/iosMain` — iOS-specific entry points
- `composeApp/src/wasmJsMain` — browser preview target
- `iosApp/` — Xcode host app

## Architecture Snapshot

This codebase is currently presentation-centric and lives mostly in `commonMain`.

- `App.kt` is the shared entry point.
- `ui/AppShell.kt` owns the app scaffold and `NavController`.
- `navigation/` contains route definitions and the shared nav host.
- `ui/screens/<feature>/` groups screen UI, `UiState`, and `ViewModel` per feature.
- `ui/components/` contains reusable presentation pieces.
- `theme/` contains shared design tokens and theme setup.

Current screens:

- Dashboard
- Camera
- Analysis
- Map Risk
- Report

## Setup Commands

Run commands from `app/`.

- List Gradle tasks: `./gradlew tasks --all`
- Run shared tests and aggregated target tests: `./gradlew :composeApp:allTests`
- Run Android lint: `./gradlew :composeApp:lint`
- Run Android debug unit tests: `./gradlew :composeApp:testDebugUnitTest`
- Run iOS simulator tests: `./gradlew :composeApp:iosSimulatorArm64Test`
- Run wasm tests: `./gradlew :composeApp:wasmJsTest`
- Assemble Android debug app: `./gradlew :composeApp:assembleDebug`
- Start wasm browser dev server: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`

For iOS app execution, open `iosApp/` in Xcode.

## Development Workflow

1. Make shared UI and state changes in `composeApp/src/commonMain` by default.
2. Move to platform source sets only when an API is platform-specific.
3. Keep route definitions centralized in `navigation/Routes.kt`.
4. Keep one screen package per feature under `ui/screens/`.
5. Add or update tests in `composeApp/src/commonTest` when changing shared behavior.

## Code Style and Conventions

These conventions are already visible in the codebase and should be preserved.

- Kotlin style is `official` (`gradle.properties`).
- Prefer immutable UI models annotated with `@Immutable` for screen state.
- Use `StateFlow` exposed as read-only state from `ViewModel` classes.
- Feature folders generally follow this pattern:
  - `FeatureScreen.kt`
  - `FeatureViewModel.kt`
  - `FeatureUiState.kt`
- Routes are modeled as a sealed interface with `data object` entries.
- Reusable UI pieces go in `ui/components/`, not inside unrelated screens.
- Shared theme tokens belong in `theme/`.

## Testing Instructions

Tests currently live in `composeApp/src/commonTest/kotlin` and use `kotlin.test`.

- Prefer focused test runs while iterating.
- For behavior in shared view models or navigation, add tests in `commonTest`.
- Existing examples include:
  - `navigation/RoutesTest.kt`
  - `ui/screens/*/*ViewModelTest.kt`

Recommended validation order:

1. `./gradlew :composeApp:allTests`
2. `./gradlew :composeApp:lint`

Avoid running broad `build` tasks unless explicitly necessary.

## Platform Guidance

- Android entry point: `composeApp/src/androidMain/kotlin/com/agrogem/app/MainActivity.kt`
- iOS entry point: `composeApp/src/iosMain/kotlin/com/agrogem/app/MainViewController.kt`
- Platform abstractions use `Platform.kt` plus platform-specific actual implementations.
- Camera placeholders are currently implemented per platform/source set rather than through a real camera integration.

## What To Avoid

- Do not edit generated folders: `.gradle/`, `.kotlin/`, `build/`
- Do not move shared code into platform source sets without a platform requirement
- Do not introduce a new architectural pattern for a single screen; stay consistent with the existing per-feature structure
- Do not document commands you did not verify against this project

## Documentation Rules

When you change developer workflow, architecture, or commands here, update both:

- `app/README.md` for humans
- `app/AGENTS.md` for coding agents
