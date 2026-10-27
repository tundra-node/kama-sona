# kama sona

A small offline toki pona app for Android. It bundles the lipu Linku dictionary, a spaced
repetition deck, a sentence glosser, and a daily journal, and works with no network and no account.

I built it for an old Galaxy S6 (Android 7.0), so it targets minSdk 24 and stays light.

<!-- Screenshots: add images to docs/ and link them here, e.g. ![dictionary](docs/dictionary.png) -->
_Screenshots coming soon._

## Features

- **lipu nimi (dictionary):** all 176 Linku words with English definitions, commentary, etymology,
  sitelen pona glyphs (nasin-nanpa font), and filters by usage category (pu, ku suli, ku lili, nasa).
  Search matches the word or its English meaning.
- **sona (SRS):** an SM-2 style scheduler with three card types per word (toki pona to English,
  English to toki pona, glyph to word). Cards come in random order; buttons preview the next interval.
- **kama sona (trainer):** type a sentence and the glosser splits it into context, subject,
  predicate, object, and prepositional phrases, and labels each word. It explains the grammar; it does
  not translate. There is also a quick multiple choice quiz.
- **tenpo ni (daily):** word of the day, a journal for today's sentences, and a streak counter.
  A daily 19:00 reminder fires only if you haven't practiced yet.
- **jan pona (couples):** five words a week to use with a partner, with a checkbox for each of you.
- **Pronunciation:** 175 bundled recordings; falls back to on-device TTS when a word has no clip.

All progress lives in one local SQLite file. Nothing leaves the phone.

## Quick start

Requirements: JDK 17 and the Android SDK (platform 34). Android Studio provides both.

```sh
git clone https://github.com/tundra-node/kama-sona.git
cd kama-sona
# point Gradle at the SDK, either with ANDROID_HOME or a local.properties file:
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # JVM unit tests (SRS, glosser, dictionary, streak)
./gradlew lintDebug            # Android lint
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On macOS with Homebrew's JDK: `export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home`.

## Configuration

There is nothing to configure at runtime. Build-time settings:

| Setting | Where | Notes |
|---|---|---|
| `sdk.dir` / `ANDROID_HOME` | `local.properties` (gitignored) or env | Path to the Android SDK |
| `JAVA_HOME` | env | JDK 17 |
| Reminder time | `notify/Reminder.kt` (`HOUR`) | Default 19:00 local |

## Rebuilding the dictionary

`app/src/main/assets/dictionary.json` is generated from a checkout of
[lipu-linku/sona](https://github.com/lipu-linku/sona) placed at `tools/data/sona`:

```sh
python3 tools/build_dictionary.py   # needs Python 3.11+ (tomllib)
```

It also writes `tools/audio_urls.txt`, the list of recordings to fetch into `app/src/main/assets/audio/`.
Other scripts in `tools/`: `make_icons.py` (launcher PNGs), `check_font_coverage.py` (which glyphs the
font can draw), `ink_check.py` and `device.py` (screenshot checks and adb helpers for testing on the phone).

## Project layout

```
app/src/main/java/com/elias/kamasona/
  MainActivity.kt      bottom-nav host, loads the dictionary
  data/Dict.kt         dictionary model, loading, search
  data/Store.kt        SQLite: cards, reviews, journal, activity, weekly marks
  srs/Srs.kt           SM-2 scheduler
  gloss/Gloss.kt       rule-based sentence glosser
  audio/Speaker.kt     bundled clips, TTS fallback
  notify/Reminder.kt   daily reminder alarm
  ui/                  one fragment per tab, word detail screen, glyph rendering
app/src/main/assets/   dictionary.json, audio/*.mp3
app/src/test/          JVM unit tests
tools/                 dictionary and icon build scripts
```

## AI usage

<!-- TODO(Elias): describe how AI tools were used when writing this app. -->
Bug fixes, unit tests, and this README were written with help from an AI coding agent. (Elias: confirm and expand.)

## License and credits

- Code: MIT, see [LICENSE](LICENSE).
- Dictionary data: [lipu Linku](https://linku.la) (sona), CC BY-SA 4.0. The bundled
  `dictionary.json` is a derived work under the same license. See
  `tools/data/sona/LICENSE`.
- Dictionary API code: [lipu-linku/ijo](https://github.com/lipu-linku/ijo), GNU GPL v3.
  See `tools/data/sona/api/LICENSE`.
- Glyphs: [nasin-nanpa](https://github.com/ETBCOR/nasin-nanpa) by jan Itan, MIT.
- Audio: community recordings by kala Asi, via [lipu-linku/ijo](https://github.com/lipu-linku/ijo).
  **Note:** `lipu-linku/ijo` has no LICENSE file, so the bundled MP3 clips do not have a
  confirmed redistribution license. If you need the app to be fully redistributable, remove
  `app/src/main/assets/audio/` and use `tools/audio_urls.txt` to fetch clips at build time,
  or obtain a license from kala Asi before shipping.
