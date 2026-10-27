# AGENTS.md: kama-sona

Offline toki pona app for Android (package `com.elias.kamasona`, minSdk 24), built for a rooted Galaxy S6 (SM-G920P, Android 7.0).

## Stack
Kotlin, Gradle (Kotlin DSL), Android SDK. Data and build scripts are Python in `tools/`.

## Commands
- Debug build: `./gradlew assembleDebug` (APK in `app/build/outputs/apk/debug/`). `app/build/` is regenerable and ignored.
- Unit tests: `./gradlew testDebugUnitTest` (JVM, in `app/src/test`). Lint: `./gradlew lintDebug`.
- JDK: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` (no system Java on this Mac).
- Install on the phone: `adb install -r <apk>` (use the `s6-device-ops` skill for the device).
- Dictionary and icons: scripts in `tools/` (`build_dictionary.py`, `make_icons.py`, `check_font_coverage.py`, `ink_check.py`). Run with `python3`.

## Where things are
- `app/src/main`: app code. `assets/`: dictionary and audio.
- `tools/`: data and build scripts. A copy of the source and `dictionary.json` is archived in `~/s6-archive/src`.

## Rules
- `local.properties` holds this machine's SDK path. Never commit it (it is gitignored).
- Data is CC BY-SA 4.0 (lipu Linku), font is MIT. Keep the attribution in the README when changing data.
- minSdk 24: do not use APIs newer than API 24 without a compat check.
- Signing keys, if added, stay out of git (`*.keystore`, `keystore.properties`).
- Do not commit build output, `__pycache__`, or `.idea/`.

## Before committing
`./gradlew assembleDebug testDebugUnitTest` must succeed. Commit with the noreply identity (`117379918+tundra-node@users.noreply.github.com`).
