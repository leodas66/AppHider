# Repo notes

- `plan.md` is the source of truth for scope: "App Hider", a rooted-Android app. Phases 0–5 are the MVP; "Later extras" are out of scope.
- Single-module Kotlin/Compose Android app in `app/` (namespace `com.vishwa.apphider`), Gradle Kotlin DSL. `plan.md` is docs only, not generated from code.
- Build command (verified): `./gradlew :app:assembleDebug`. No custom lint/typecheck/test scripts exist; use Gradle tasks directly.
- Toolchain in this sandbox: JDK 21, Android SDK at `/data/home/Android/Sdk` (platform 35, build-tools 35.0.0, platform-tools), Gradle 8.9 wrapper. `local.properties` is machine-specific and gitignored.
- Constraints from the plan: root via `pm hide` / `pm unhide`; `QUERY_ALL_PACKAGES` needed to list apps; Room is the source of truth because hidden apps drop out of normal package queries; keep a strict protected-package list; prefer shell commands over reflection (non-SDK restrictions).
- `AppContainer` is the manual DI root; `MainViewModel` drives all screens. `shouldBeHidden` in Room is user intent; actual hide state is on the device via `RootManager`.
- Not a git repository — no commit/PR workflow to follow.
