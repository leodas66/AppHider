# Repo notes

- `plan.md` is the source of truth for scope: "App Hider", a rooted-Android app. Phases 0–5 are the MVP; "Later extras" are out of scope.
- Single-module Kotlin/Compose Android app in `app/` (namespace `com.vishwa.apphider`), Gradle Kotlin DSL. `plan.md` is docs only, not generated from code.
- Build command (verified): `./gradlew :app:assembleDebug`. No custom lint/typecheck/test scripts exist; use Gradle tasks directly.
- Toolchain in this sandbox: JDK 21, Android SDK at `/data/home/Android/Sdk` (platform 35, build-tools 35.0.0, platform-tools), Gradle 8.9 wrapper. `local.properties` is machine-specific and gitignored.
- Constraints from the plan: root via `pm hide` / `pm unhide`; `QUERY_ALL_PACKAGES` needed to list apps; Room is the source of truth because hidden apps drop out of normal package queries; keep a strict protected-package list; prefer shell commands over reflection (non-SDK restrictions).
- `AppContainer` is the manual DI root; `MainViewModel` drives all screens. `shouldBeHidden` in Room is user intent; actual hide state is on the device via `RootManager`.
- Git repo at `https://github.com/leodas66/AppHider` (private), default branch `main`. Commits are authored as `leodas66` via per-command `-c user.name/-c user.email`; no global git identity is configured.
- `.github/workflows/release.yml` runs on `v*` tags and publishes ABI-split **debug and signed release** APKs (arm64-v8a, armeabi-v7a, x86, x86_64, universal) to a GitHub Release.
- Release signing uses repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The PKCS12 keystore lives outside the repo at `/data/workspace/apphider-release.jks`; passwords at `/data/workspace/apphider-release.env`. Store and key password must match (PKCS12 ignores a separate `-keypass`).
- `MainActivity` must be a `FragmentActivity` for `BiometricPrompt`. Don't use `ActivityResultContracts` for permissions there — its generated request codes exceed 16 bits and crash on startup; use `ActivityCompat.requestPermissions` with a fixed small code.
