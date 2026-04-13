# AgroGem App

A simple Kotlin Multiplatform mobile project (Android + iOS) built with Compose Multiplatform.

## What is in the repository right now

- A root project setup with a mobile app module in `app/`
- Shared UI and business code in:
  - `app/composeApp/src/commonMain`
- Android-specific entry point in:
  - `app/composeApp/src/androidMain`
- iOS-specific entry point in:
  - `app/composeApp/src/iosMain`
- iOS host application in:
  - `app/iosApp`

## Current app status

At the moment, the app contains the default starter screen:

- A **"Click me!"** button
- A simple animated section that appears/disappears
- A greeting message that changes based on the platform (Android or iOS)

## Tech stack

- Kotlin Multiplatform
- Compose Multiplatform
- Gradle (Kotlin DSL)

## Run the project

From the `app/` directory:

### Android (debug build)

- macOS / Linux:
  `./gradlew :composeApp:assembleDebug`
- Windows:
  `.\gradlew.bat :composeApp:assembleDebug`

### Web (development)

- macOS / Linux:
  `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- Windows:
  `\.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun`

When the task starts, open the URL printed in the terminal (usually `http://localhost:8080`).

### iOS

Open `app/iosApp` in Xcode and run the app from there.

## Notes

This README is intentionally simple and reflects the project as it currently exists. It can be expanded later with architecture, setup requirements, environment config, and release steps.