# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Branch `app`: the Android app only (Kotlin + Gradle, WebView plus scheduled notifications, no tests).
It shows the shared "poem of the day" from the public `today.json` published by the AWS stack on
branch `pages` (which also holds the web page). The two branches never merge; their only shared
contract is `today.json` (`{date, id, title, author, text}`) and its URL. The APK bundles **no poems**.

## Commands

```bash
./gradlew assembleDebug               # build the debug APK (needs Android SDK)
./gradlew installDebug                # install on a connected device
```

On this machine the default Java is 25, which Gradle 8.7/AGP 8.5 cannot use, and `ANDROID_HOME` is unset. Prefix Gradle commands with the system JDK 17 and SDK:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk ./gradlew installDebug
```

(Alternatively create a gitignored `local.properties` with `sdk.dir=/usr/lib/android-sdk` and set only `JAVA_HOME`.)

## Release

`.github/workflows/release.yml` runs on tags `v*` (cut from `app`): it builds `assembleRelease`
signed with the release keystore, verifies it with `apksigner`, and attaches it to a GitHub Release.

- `git tag v1.2.0 app && git push origin v1.2.0`. `versionName` = tag without `v`; `versionCode` = the workflow run number.
- Secrets: `ANDROID_KEYSTORE_BASE64` (`base64 -w0 release.jks`), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- `app/build.gradle.kts` reads the same names from the environment (`ANDROID_KEYSTORE_PATH` instead of the base64). Without them `assembleRelease` produces an unsigned APK.
- The keystore must never change: a different key means installed apps cannot update.

## Architecture (`app/`, package `com.webpoema.app`)

- **Poem source**: `TodayRepository.fetchBlocking` GETs `BuildConfig.TODAY_JSON_URL` (from `todayJsonUrl` in `gradle.properties`, override with `-PtodayJsonUrl=...`), caches the last good body in SharedPreferences, and falls back to that cache. Returns `null` when there is neither (fresh install offline). Blocking: run it on `TodayRepository.executor`.
- **Display**: `MainActivity` is a WebView that loads `https://appassets.androidplatform.net/assets/index.html`, served from `assets/` by `WebViewAssetLoader` (`androidx.webkit`). Do not switch back to `file:///android_asset/`, which WebView restricts. The page shows «Cargando…» until `MainActivity` calls `window.showPoem(poem)` or `window.showError()` via `evaluateJavascript` (queued in `pendingScript` until the page has loaded). The notification's extras (`show_poem`/`title`/`author`/`text`) are shown directly without fetching.
- **JS bridge**: `MainActivity` exposes `Android.openSettings()` (`@JavascriptInterface`) to the page. The ⚙ button in `index.html` calls it to start `SettingsActivity`.
- **Theme**: `Theme.WebPoema` is a Material3 DayNight theme (`res/values/themes.xml`; night variant in `res/values-night/themes.xml`). `WebPoemaApp` (the `Application` class) applies the saved choice at startup via `AppCompatDelegate.setDefaultNightMode`, and `SettingsActivity` re-applies it on save. The WebView reports `prefers-color-scheme` from the app's night mode, so the `@media (prefers-color-scheme: dark)` block in `index.html` styles the poem page. The CSS mirrors `web/index.html` on branch `pages`.
- **Settings ("Ajustes")**: `SettingsActivity` has "Notificaciones" (frequency `never`/`daily`/`weekly`) and "Tema" (`light`/`dark`), stored via `PreferenceManager` (SharedPreferences) and saved together by Guardar.
- **Notifications**: `NotificationScheduler.schedule` sets a one-shot exact alarm for the next 09:00 local time (Sundays for weekly). `PoemNotificationReceiver` resolves the poem at fire time with `fetchBlocking` (skips the notification if there is none) and always re-arms the next alarm. `BootReceiver` re-arms after reboot, and `MainActivity` reconciles the schedule at launch.
- Build config: AGP 8.5.0, Kotlin 1.9.22, compile/target SDK 34, min SDK 26, Java 17. The root `build.gradle.kts` uses the legacy `buildscript` classpath style.
- `app/src/AndroidManifest.xml` is an empty stub. The real manifest is `app/src/main/AndroidManifest.xml`.
