# AGENTS.md

## Environment

This project uses Python with `uv` for virtual environment management.

```bash
uv venv
source .venv/bin/activate
python app.py
```

## CLI

```bash
./webpoem start      # Start the server
./webpoem stop       # Stop the server
./webpoem restart    # Restart the server
./webpoem status     # Show server status
```

## Project

Simple static poetry website that shows the shared "poem of the day" and falls
back to a random poem from `Poemas/` when `today.json` is unreachable.

- **Server**: `app.py` — Python built-in HTTP server, no dependencies needed
- **Poem of the day**: `today.json` published by `infra/` (override the URL with `WEBPOEMA_TODAY_URL`)
- **CLI**: `webpoem` — manages the server process (start/stop/restart/status)
- **Poems**: `Poemas/` — local fallback collection; add or remove `.md` files
- **Run**: `./webpoem start` → open `http://localhost:8000`

## Android App

Android app that displays poems in a WebView with scheduled notifications.

```bash
# Build (requires Android SDK/NDK)
./gradlew assembleDebug

# On this machine use JDK 17 (Gradle 8.7 cannot run on the default Java 25) and the system SDK
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk ./gradlew installDebug
```

- **App**: `app/` — Android app built with Kotlin + Gradle
- **Poems**: `app/src/main/assets/poems.json` — bundled poems (auto-generated from `Poemas/`), used as offline fallback
- **Poem of the day**: fetched from `today.json` at launch and when a notification fires (cached for offline use). The URL comes from `todayJsonUrl` in `gradle.properties`; override with `-PtodayJsonUrl=...`
- **Notifications**: scheduled for 09:00 local time (daily, or Sundays for weekly), re-armed after each run and after reboot; the poem is resolved at fire time
- **Features**: WebView display, "Ajustes" screen (⚙ button) with notification frequency (daily/weekly/never) and theme (Claro/Oscuro; follows the system until chosen)
- **WebView**: the page is served by `WebViewAssetLoader` from `https://appassets.androidplatform.net/assets/`, not `file://` (otherwise `fetch('poems.json')` fails)
- **Dark theme**: `res/values-night/themes.xml` (native screens) and `prefers-color-scheme` CSS in `assets/index.html` and `app.py` (keep both in sync)
- **Run**: `./gradlew installDebug` (requires connected device)

## Infra (`infra/`)

AWS CDK (Python) stack that publishes one shared "poem of the day" as `today.json`
(Lambda + EventBridge Scheduler at 00:05 Europe/Madrid, public-read `today.json`
only). See `infra/README.md`.

- Deploy with the `personal` AWS profile (us-east-1): `cdk deploy -c poemsBucket=<name>` from `infra/`. Deploys will eventually run from GitHub Actions, so keep account/region environment-driven (no hardcoded profile or account).
- Lambda code is `infra/lambda/poem_of_the_day.py` (boto3 only); tests: `python -m unittest discover infra/tests`.
- `today.json` contract: `{date, id, title, author, text}`. Clients (`app.py`, Android app) fetch it and fall back to local poems.
