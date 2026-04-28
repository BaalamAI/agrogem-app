#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APP_MODULE=":composeApp"

usage() {
  cat <<'EOF'
Usage: ./scripts/android-dev.sh <command>

Commands:
  open        Open workspace in Android Studio
  sync        Warm Gradle/resources for Android Studio sync-like setup
  build       Build debug APK
  install     Install debug APK on connected device/emulator
  run         Build + install debug APK
  test-chat   Run focused chat unit tests
  logcat      Stream adb logcat

Also see Makefile for iOS helpers:
  make ios-open
  make ios-sim-list
  make ios-run IOS_SIMULATOR="iPhone 16"
EOF
}

command="${1:-}"

case "$command" in
  open)
    open -a "Android Studio" "$ROOT_DIR"
    ;;
  sync)
    ./gradlew "$APP_MODULE:prepareComposeResourcesTaskForCommonMain" "$APP_MODULE:generateComposeResClass" "$APP_MODULE:kmpPartiallyResolvedDependenciesChecker"
    ;;
  build)
    ./gradlew "$APP_MODULE:assembleDebug"
    ;;
  install)
    ./gradlew "$APP_MODULE:installDebug"
    ;;
  run)
    ./gradlew "$APP_MODULE:assembleDebug" "$APP_MODULE:installDebug"
    ;;
  test-chat)
    ./gradlew "$APP_MODULE:testDebugUnitTest" --tests "com.agrogem.app.ui.screens.chat.*"
    ;;
  logcat)
    adb logcat
    ;;
  *)
    usage
    exit 1
    ;;
esac
