# App Hider (Rooted Android) – Build Plan

*Nov 21, 2026 · @Vishwa*

## Goal and approach

Build a rooted-Android app that fully hides chosen apps with `pm hide`, and opens them behind a PIN or biometric lock.

Full hide is the strongest option: a hidden package is unavailable, almost like uninstalled, but its data and package file stay on the device. It disappears from the launcher, Settings and other apps' queries. The cost is that a hidden app can't run, so it gets no notifications or background sync until you unhide it.

Why not the alternatives:

- Suspend or disable: the app stays visible or greyed out.
- Icon-only hide (disable the launcher activity): the app keeps running, but it still shows in Settings > Apps.
- Private Space (Android 15+) or a work profile (Shelter): a separate copy with its own data, and Private Space isn't fully hidden from package search.
- Hide My Applist (LSPosed): hides apps from specific apps' queries only, and its license forbids modifying or redistributing it.

## Stack

- Kotlin, Jetpack Compose (Material 3), Room, WorkManager
- libsu (core module, service module later) for the root shell
- BiometricPrompt plus a hashed PIN stored with the Android Keystore
- minSdk 26, targetSdk 35 or latest
- Later, optionally: Shizuku/Sui for faster binder calls instead of shell commands

## UI and design

Clean Material 3 with dynamic color, following the system light/dark theme. Single-activity Compose app with a bottom nav: Apps and Hidden.

- **Apps screen:** adaptive icon grid with labels and a search bar. Tap an app to open a bottom sheet with Hide (with confirm). Protected apps (launcher, SystemUI, Settings, dialer, Play Services, your app) show a lock badge and can't be hidden.
- **Hidden screen:** same grid, dimmed. Tap an app, pass the lock, then unhide, launch and re-hide. Long-press for Unhide permanently or Remove from list.
- **Settings screen:** auto re-hide timeout (1, 5 or 15 minutes, or on screen off), change PIN, biometric on/off, Unhide all, root status and Android version.

## Lock and panic button

- First run: set a PIN of 4 to 6 digits, stored hashed with a salt in the Android Keystore.
- Biometric is a shortcut with PIN fallback.
- The app locks when it goes to the background or after the timeout.
- The lock is required to open the Hidden tab and to unhide anything.
- Panic hide button: a floating button on the Hidden screen that instantly re-hides everything currently unhidden. A persistent notification action, "Hide all now", shows while any app is temporarily unhidden.

## Build phases

1. **Phase 0, manual proof (30 min):** run `su -c "pm hide <pkg>"` and `su -c "pm unhide <pkg>"` on a test app. Check the launcher, Settings > Apps, recents, notifications and shortcuts. Write down your recovery command.
2. **Phase 1, core:** request root with `Shell.getShell()` on a splash screen. Build a RootManager with `hide`, `unhide`, `isHidden` and `listAll`. Use `pm list packages -u` to include hidden packages. Room table: package, label, hidden flag, last unlocked time.
3. **Phase 2, Apps grid:** icon grid, search, hide flow with confirm dialog, protected list.
4. **Phase 3, lock:** PIN setup, biometric, lock-on-background.
5. **Phase 4, Hidden tab:** unhide, launch, re-hide after the timeout or on Lock again.
6. **Phase 5, safety:** panic button, notification action, Unhide all, boot-time check of Room against actual state, handling of root denied or package missing.

Phases 0 to 5 make the MVP.

## Later extras

- Quick Settings tile
- Hide your own icon and open it with a dialer secret code (Evil Hide does this)
- Scheduled hide with WorkManager
- Hide groups (work, private)
- Suspend and icon-only modes as per-app options
- Shizuku/Sui backend for faster calls

## Risks, gotchas and testing

- Hidden apps drop out of normal package queries, so Room is your source of truth.
- Listing all apps needs the `QUERY_ALL_PACKAGES` permission. It's fine when sideloading; Google Play needs approval.
- Android's non-SDK interface restrictions change each version (Android 16 updated the lists). Prefer shell commands over reflection.
- Apps hidden in one mode must be unhidden through the same mode.
- Some ROMs kill background processes aggressively, so test on your daily-driver ROM.
- Hiding system or critical apps can break the phone. Keep the protected list strict.
- Unhide everything before uninstalling your own app.
- Test on Android 14, 15 and 16 if possible, with Magisk or KernelSU.

## References

- Hail README: root-mode hide and suspend
- libsu: root shell and root services
- Shizuku API: binder calls with root or ADB identity
- Evil Hide: hide-own-icon and dialer-code launch
- Package visibility
- Android 16 non-SDK changes
- Private space
- Amarok
