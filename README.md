# Amla

**A professional, automatic equalizer for Android audio outputs.**

Amla detects what you are listening through — Bluetooth / LE Audio headphones,
USB DACs, wired earphones or the built-in speaker — and automatically applies the
EQ profile you saved for that device. Band frequencies are read from the device's
own audio stack, so the sliders correspond to real hardware bands.

- Application ID: `com.amala`
- Kotlin + Jetpack Compose (Material 3)
- Minimum Android: 8.0 (API 26)

## Features

- **Automatic output-device detection**
  - Bluetooth A2DP and LE Audio (headsets, speakers, hearing aids, broadcast)
  - USB audio devices, DACs and USB headsets
  - Wired 3.5 mm headphones and headsets
  - Built-in speakers, HDMI, docks, line-out and other outputs
  - Uses `AudioManager.getDevices()` + `AudioDeviceCallback` + headset-plug broadcasts
- **One profile per device** — saved locally and applied the moment the output changes
- **Studio-style 5–31 band equalizer** with real frequency labels from `Equalizer.getBand()`
- **12 presets** (Flat, Bass Boost, Treble, Vocal, Rock, Pop, Jazz, Classical,
  Electronic, Acoustic, Podcast, Custom) that are resampled to the device's band count
- **Bass boost** and **surround / virtualizer** controls when supported by the device
- **Instant bypass** with a master effect switch
- **Profile manager** — apply, inspect and delete saved profiles
- **Settings** — toggle auto-detect, reinitialize effects, inspect detected hardware
- **Material 3 UI** with light/dark themes and a custom launcher icon
- **Global effects** — session 0 attaches the effects to the output mix, so EQ applies
  to music, video, games and navigation, not just one app

## Architecture

| Layer | Responsibility |
| --- | --- |
| `model/` | Device families, equalizer profiles, presets and pure EQ math |
| `audio/` | `AudioEngine` (Android `Equalizer` / `BassBoost` / `Virtualizer`) and `AudioDeviceDetector` |
| `data/` | `KeyValueStore` abstraction + `AmlaPreferences` (per-device profiles and settings) |
| `viewmodel/` | `MainViewModel` coordinates detection, profile loading and effect application |
| `ui/` | Compose screens: Equalizer, Profiles, Settings, About |

The pure model and persistence logic is covered by JVM unit tests; Android
integration is covered by an instrumentation smoke test.

## Build

Everything is built in CI — no Android SDK is required locally.

```bash
git clone https://github.com/kalhydra6-wq/Amla.git
cd Amla
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # APK: app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions

`.github/workflows/android.yml` runs on every push and pull request to `main`:

1. Sets up JDK 17 and Gradle 8.7 (wrapper)
2. `./gradlew testDebugUnitTest`
3. `./gradlew assembleDebug`
4. Uploads `amla-debug-apk` as a workflow artifact

Download the APK from the **Actions → Android CI → Artifacts** section.

## Notes

- Some phone manufacturers restrict global audio effects. If audio does not change,
  open **Settings → Reinitialize effects** and check the device's sound settings.
- Android only allows one active global EQ at a time. Amla yields to other EQ apps
  that hold the effect, and reports effects as unavailable in that case.
- No account, network access or Bluetooth permission is required at runtime on
  Android 12+; device discovery uses the audio framework only.

## License

MIT — see [LICENSE](LICENSE).
