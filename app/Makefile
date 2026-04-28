APP_MODULE := :composeApp
ANDROID_STUDIO_APP := Android Studio
IOS_PROJECT := iosApp/iosApp.xcodeproj
IOS_SCHEME := iosApp
IOS_SIMULATOR ?= iPhone 16e
IOS_DERIVED_DATA := build/ios-derived-data
IOS_APP_PATH := $(IOS_DERIVED_DATA)/Build/Products/Debug-iphonesimulator/iosApp.app

.PHONY: help android-open android-sync android-build android-install android-run android-test-chat android-logcat ios-open ios-sim-list ios-build ios-boot ios-install ios-run

help:
	@printf "Available targets:\n"
	@printf "  make android-open       Open this workspace in Android Studio\n"
	@printf "  make android-sync       Warm Gradle and Android resources\n"
	@printf "  make android-build      Build Android debug APK\n"
	@printf "  make android-install    Install debug app on connected device/emulator\n"
	@printf "  make android-run        Build + install debug app\n"
	@printf "  make android-test-chat  Run focused chat unit tests\n"
	@printf "  make android-logcat     Show adb logcat\n"
	@printf "  make ios-open           Open iOS project in Xcode\n"
	@printf "  make ios-sim-list       List available iOS simulators\n"
	@printf "  make ios-build          Build iosApp for simulator\n"
	@printf "  make ios-boot           Boot configured simulator ($(IOS_SIMULATOR))\n"
	@printf "  make ios-install        Install app in booted simulator\n"
	@printf "  make ios-run            Build + boot simulator + install + launch\n"

android-open:
	open -a "$(ANDROID_STUDIO_APP)" "$(CURDIR)"

android-sync:
	./gradlew $(APP_MODULE):prepareComposeResourcesTaskForCommonMain $(APP_MODULE):generateComposeResClass $(APP_MODULE):kmpPartiallyResolvedDependenciesChecker

android-build:
	./gradlew $(APP_MODULE):assembleDebug

android-install:
	./gradlew $(APP_MODULE):installDebug

android-run:
	./gradlew $(APP_MODULE):assembleDebug $(APP_MODULE):installDebug

android-test-chat:
	./gradlew $(APP_MODULE):testDebugUnitTest --tests "com.agrogem.app.ui.screens.chat.*"

android-logcat:
	adb logcat

ios-open:
	open "$(IOS_PROJECT)"

ios-sim-list:
	xcrun simctl list devices available

ios-build:
	xcodebuild -project "$(IOS_PROJECT)" -scheme "$(IOS_SCHEME)" -configuration Debug -sdk iphonesimulator -destination "platform=iOS Simulator,name=$(IOS_SIMULATOR)" -derivedDataPath "$(IOS_DERIVED_DATA)" build

ios-boot:
	xcrun simctl boot "$(IOS_SIMULATOR)" || true
	open -a Simulator

ios-install:
	xcrun simctl install booted "$(IOS_APP_PATH)"

ios-run:
	$(MAKE) ios-build
	$(MAKE) ios-boot
	$(MAKE) ios-install
	xcrun simctl launch booted com.agrogem.app
