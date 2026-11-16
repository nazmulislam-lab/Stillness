# Stillness

Track mobile screen time, usage patterns, and receive weekly AI-powered digital well-being suggestions.

Stillness is Android-only.

## Install from the latest release

On your phone, open the [latest release](https://github.com/nazmulislam-lab/Stillness/releases/latest) and tap **Stillness.apk** (or use [this direct link](https://github.com/nazmulislam-lab/Stillness/releases/latest/download/Stillness.apk)).

If Android blocks the install, allow installation from unknown sources for the app you downloaded it with (Chrome, Files, or Drive), then open the APK again.

Stillness uses the `PACKAGE_USAGE_STATS` permission to read on-device screen time. After it is installed, grant Usage access to Stillness in Settings: **Settings → Apps → Special app access → Usage access → Stillness**. The app can also open that screen from **Open Usage Settings**.

Each published APK is debug-signed with a keystore created in CI for that build. If Android refuses to install an update over an existing copy, uninstall Stillness and install the new APK.

Weekly suggestions in this build are sample text. Live Gemini coaching needs Firebase AI Logic, and this project has no `google-services.json` or `GEMINI_API_KEY`.

## Build locally

You need JDK 17 or newer (JDK 21 works) and the Android SDK with platform `android-36.1` and build-tools `36.0.0`. Point `ANDROID_HOME` at the SDK, or set `sdk.dir` in `local.properties`.

Debug builds are signed with `debug.keystore` in the repo root. That file is gitignored. Create it if it is missing:

```bash
keytool -genkeypair -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

Then:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. No `google-services.json` or API key is required.
