# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Two independent front-ends that show a random poem, sharing one poem collection:

- **Web server** (`app.py`, `webpoem`): Python stdlib only, no dependencies, no tests, no linter.
- **Android app** (`app/`): Kotlin + Gradle, WebView plus scheduled notifications. No tests.

## Commands

```bash
./webpoem start|stop|restart|status   # manage the server (PID in .webpoem/pid, gitignored)
python app.py --port 8000             # run in the foreground (default port 8000, binds 0.0.0.0)
./gradlew assembleDebug               # build the APK (needs Android SDK)
./gradlew installDebug                # install on a connected device
```

On this machine the default Java is 25, which Gradle 8.7/AGP 8.5 cannot use, and `ANDROID_HOME` is unset. Prefix Gradle commands with the system JDK 17 and SDK:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk ./gradlew installDebug
```

(Alternatively create a gitignored `local.properties` with `sdk.dir=/usr/lib/android-sdk` and set only `JAVA_HOME`.)

Python env is managed with `uv` (`uv venv && source .venv/bin/activate`), but nothing needs installing.

## Web architecture

- `app.py` globs `Poemas/**/*.md` **once at import time** into `POEMS`. Added or removed poems only show up after `./webpoem restart`.
- Only `GET /` is custom: it picks a random poem and renders it through the inline HTML template in `render_poem`. Every other path falls through to `SimpleHTTPRequestHandler`, which serves static files from the repo root, so the whole repo (including `.git`) is reachable over HTTP.
- The inline CSS has a `prefers-color-scheme: dark` block (black background, white text), so the page follows the browser/OS theme. Keep it in sync with `app/src/main/assets/index.html`.
- Poem text is interpolated into the HTML unescaped. The markdown is not parsed, and CSS `white-space: pre-line` preserves the line breaks.
- Author and title come from the path. The title is the filename without `.md`. The author is every directory component below `Poemas/`. The files are currently nested as `Poemas/Poemas/<Author>/<Title>.md`, so the "author" shown is literally `Poemas/<Author>`.

## Android architecture (`app/`, package `com.webpoema.app`)

- **Poem data**: `app/src/main/assets/poems.json` is an array of `{title, author, text}`. It is a committed snapshot of `Poemas/`. AGENTS.md calls it "auto-generated", but **no generator script exists in the repo**. After changing `Poemas/`, regenerate it by hand or write the script. Note that the JSON `author` field has no `Poemas/` prefix, unlike the web server's.
- **Display**: `MainActivity` is a WebView that loads `https://appassets.androidplatform.net/assets/index.html`, served from `assets/` by `WebViewAssetLoader` (`androidx.webkit`). Do not switch back to `file:///android_asset/`: WebView blocks `fetch()` on `file://` pages, which shows "No se pudieron cargar los poemas." The page fetches `poems.json` and picks a random poem in JS. It also reads `title`/`author`/`text` from the query string if present. Its HTML and CSS are a duplicate of `render_poem` in `app.py`, so keep the two in sync.
- **JS bridge**: `MainActivity` exposes `Android.openSettings()` (`@JavascriptInterface`) to the page. The ⚙ button in `index.html` calls it to start `SettingsActivity`.
- **Theme**: `Theme.WebPoema` is a Material3 DayNight theme (`res/values/themes.xml`; night variant in `res/values-night/themes.xml`: black background, white primary, black status bar). `WebPoemaApp` (the `Application` class, set in the manifest) applies the saved choice at startup via `AppCompatDelegate.setDefaultNightMode`, and `SettingsActivity` re-applies it on save. The WebView reports `prefers-color-scheme` from the app's night mode, so the `@media (prefers-color-scheme: dark)` block in `index.html` styles the poem page. If the user never chose a theme, the app follows the system.
- **Settings ("Ajustes")**: `SettingsActivity` has two sections, "Notificaciones" (frequency `never`/`daily`/`weekly`) and "Tema" (`light`/`dark`). Both are stored via `PreferenceManager` (SharedPreferences) and saved together by the Guardar button.
- **Notifications**: saving calls `NotificationScheduler.schedule`. That cancels the existing alarm, picks a random poem via `PoemLoader`, and sets a single **one-shot** exact alarm (`AlarmManager.setExactAndAllowWhileIdle`) carrying the poem in the intent extras. `PoemNotificationReceiver` posts the notification when it fires.
- Build config: AGP 8.5.0, Kotlin 1.9.22, compile/target SDK 34, min SDK 26, Java 17. The root `build.gradle.kts` uses the legacy `buildscript` classpath style.

### Known gaps in the Android code

These explain behavior that looks broken. Check them before assuming a new bug:

- The alarm is never re-armed after it fires and there is no `BOOT_COMPLETED` receiver, so notifications stop after the first one.
- `MainActivity` ignores the `show_poem`/`title`/`author`/`text` extras that the notification's PendingIntent sends.
- `app/src/AndroidManifest.xml` is an empty stub. The real manifest is `app/src/main/AndroidManifest.xml`.
