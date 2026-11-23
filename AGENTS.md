# AGENTS.md

## Branch

`app`: Android app only. The web page and the AWS stack live on branch `pages`
(the default). The two branches never merge. See `CLAUDE.md` for the architecture.

## Build

```bash
./gradlew assembleDebug    # needs Android SDK and JDK 17
./gradlew installDebug     # requires a connected device

# On this machine use JDK 17 (Gradle 8.7 cannot run on the default Java 25) and the system SDK
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk ./gradlew installDebug
```

## Release

Tag `v*` on `app` and push the tag: `.github/workflows/release.yml` builds a signed
release APK and attaches it to a GitHub Release. Signing secrets:
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

## Project

- **App**: `app/` (Kotlin + Gradle), package `com.webpoema.app`.
- **Poem of the day**: the only poem source is the public `today.json`
  (`{date, id, title, author, text}`) published by the stack on `pages`. It is fetched at
  launch and when a notification fires, and cached for offline use. No poems are bundled.
  The URL comes from `todayJsonUrl` in `gradle.properties`; override with `-PtodayJsonUrl=...`.
- **Notifications**: 09:00 local time (daily, or Sundays for weekly), re-armed after each run and after reboot.
- **Features**: WebView display, "Ajustes" screen (⚙ button) with notification frequency (daily/weekly/never) and theme (Claro/Oscuro; follows the system until chosen).
- **WebView**: the page is served by `WebViewAssetLoader` from `https://appassets.androidplatform.net/assets/`, not `file://`.
- **Dark theme**: `res/values-night/themes.xml` (native screens) and `prefers-color-scheme` CSS in `assets/index.html` (mirrors `web/index.html` on `pages`).
