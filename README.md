# AgroGem App

AgroGem is a hackathon project for farmers in rural Guatemala: an offline-first mobile assistant that helps diagnose crop pests and health problems from images and natural-language descriptions.

The goal is simple: reduce the time between “my crop has a problem” and “I know what to do next.” Today, that process can take nearly 10 days between field visits, diagnosis, supplier coordination, and product delivery. AgroGem shortens that loop by combining on-device Gemma inference, Spanish-first guidance, and agronomic tools that can enrich answers when connectivity is available.

The repository root is a lightweight workspace. The actual Kotlin Multiplatform product code lives in `app/`.

## Hackathon context

AgroGem was built around a real agricultural problem observed in Guatemala’s agroindustrial sector:

- Small farmers often lack immediate access to expert diagnosis.
- Rural connectivity is unreliable, so offline behavior is not optional.
- Misdiagnosis or delayed action can mean crop loss, debt pressure, and repeated misuse of chemical products.
- Biological pest-control alternatives exist, but farmers need trustworthy guidance to adopt them confidently.

The product focuses on Spanish-language, field-ready support for farmers and agricultural engineers. It avoids assuming constant internet access and treats local inference as the default path.

For the full narrative, motivation, challenges, and roadmap, read [`WRITEUP.md`](./WRITEUP.md).

## Solution approach

AgroGem is being developed in two phases:

1. **Diagnosis** — detect pests and crop health problems from images and descriptions.
2. **Recommendations** — suggest concrete biological products and action plans based on the diagnosis.

The hackathon implementation centers on phase one while laying the foundation for the full diagnosis-to-recommendation loop.

### Online mode

When connectivity exists, Gemma can use function-calling tools exposed by the backend to reason over live agronomic data such as weather, soil conditions, pest and disease risk, irrigation windows, and harvest timing.

### Offline mode

When connectivity is unavailable, the app runs local inference on-device. This lets farmers receive an immediate Spanish diagnosis, affected-zone guidance, and suggested next steps without waiting for a network connection.

## Current app scope

The app is no longer the default starter template. The shared application currently includes:

- A dashboard flow with recent analysis cards and health stats
- A camera-inspired capture screen
- An analysis progress screen
- A map/risk overview screen
- A report screen for diagnosis details
- Shared navigation and screen-specific view models in `commonMain`

## Repository structure

- `app/` — main Kotlin Multiplatform application
- `app/composeApp/src/commonMain` — shared UI, navigation, state, and presentation logic
- `app/composeApp/src/androidMain` — Android-specific entry point and platform code
- `app/composeApp/src/iosMain` — iOS-specific entry point and platform code
- `app/composeApp/src/wasmJsMain` — WebAssembly preview target
- `app/iosApp` — Xcode host application for iOS

## Tech stack

- Kotlin Multiplatform
- Compose Multiplatform
- Android Gradle Plugin
- Gradle Kotlin DSL
- AndroidX Navigation Compose
- Kotlin `StateFlow` + `ViewModel` for screen state
- LiteRT for on-device Gemma inference
- FastAPI backend with agronomic function-calling endpoints
- SQLDelight for local persistence and offline-first storage
- MongoDB and Redis in the backend
- External agronomic data from Open-Meteo, NASA POWER, and ISRIC SoilGrids

## Model and offline strategy

The app is designed for mid-range Android devices while keeping future iOS compatibility through Kotlin Multiplatform.

The model is distributed separately from the APK because multi-gigabyte model files are not practical to bundle directly. The app installs as a lightweight base and downloads the `.litertlm` model when local Gemma capabilities are needed.

The intended download flow preserves partial bytes, resumes with HTTP Range requests, writes to a temporary `.tmp` file, and only promotes the file to `.litertlm` after a clean transfer. If the local model is unavailable or the request exceeds local capabilities, the app can fall back to the remote backend so the farmer still gets an answer.

## Challenges captured during the hackathon

- **Dataset availability:** Guatemala-specific labeled crop disease images are scarce, so synthetic data was generated as a starting point while field data is collected.
- **Fine-tuning constraints:** Local hardware was not enough for the full workload, so training used Kaggle GPU quota with Unsloth.
- **Model export:** LiteRT conversion for multimodal Gemma 4 is still limited, so the production app currently uses the official Gemma 4 bundle for image support while the fine-tuned model path matures.
- **Rural distribution:** Large model downloads must be resumable and fault-tolerant because unreliable connectivity is part of the target environment.
- **Language:** Spanish is a core product feature, not a translation layer. Farmers should receive clear guidance without unnecessary technical jargon.

## What is next

- Harden resumable model downloads and error handling.
- Replace synthetic training examples with ground-truth Guatemalan field data.
- Explore regional pest and disease retrieval using multimodal embeddings or future fine-tuning.
- Connect the mobile diagnosis flow with the existing WhatsApp and Messenger agent so cloud recommendations can suggest specific biological products when signal returns.

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
