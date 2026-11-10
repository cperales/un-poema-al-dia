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

Simple static poetry website that serves a random poem from `Poemas/` on each page load.

- **Server**: `app.py` — Python built-in HTTP server, no dependencies needed
- **CLI**: `webpoem` — manages the server process (start/stop/restart/status)
- **Poems**: `Poemas/` — add or remove `.md` files to update the collection
- **Run**: `./webpoem start` → open `http://localhost:8000`

## Android App

Android app that displays poems in a WebView with scheduled notifications.

```bash
# Build (requires Android SDK/NDK)
./gradlew assembleDebug
```

- **App**: `app/` — Android app built with Kotlin + Gradle
- **Poems**: `app/src/main/assets/poems.json` — bundled poems (auto-generated from `Poemas/`)
- **Features**: WebView display, daily/weekly/never notification settings
- **Run**: `./gradlew installDebug` (requires connected device)
