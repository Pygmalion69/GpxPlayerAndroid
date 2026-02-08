# GpxPlayer Android

GpxPlayer Android is a lightweight companion app for [**GpxPlayer Desktop**](https://github.com/Pygmalion69/GpxPlayerDesktop). It listens for broadcast intents that describe a location (and optional speed) and injects that data as mock GPS coordinates on the device. This allows the desktop application to drive location-based testing on an Android emulator or device.

## What it does

- Exposes a broadcast receiver for `org.nitri.gpxplayer.ACTION_SET_LOCATION` intents with a `geo:` URI payload.
- Converts the incoming coordinates into a mock location and sets them for GPS (and network) providers.
- Prompts users to grant mock location permissions if the app is not configured as the mock location app in Developer Options.

## Requirements

- **Android Studio** (or the Android SDK + Gradle).
- **Device or emulator** running Android 6.0+ (minSdk 23).
- **Developer Options** enabled, with *GpxPlayer* selected as the mock location app.

## Build and install

```bash
./gradlew assembleDebug
```

Install the APK on a connected device/emulator:

```bash
./gradlew installDebug
```

## Usage

[GpxPlayer Desktop](https://github.com/Pygmalion69/GpxPlayerDesktop) (or any client) should send a broadcast intent with a `geo:` URI and an optional speed extra.

- **Action**: `org.nitri.gpxplayer.ACTION_SET_LOCATION`
- **Data URI**: `geo:LAT,LON`
- **Extras**:
  - `speed` (integer, km/h) — optional, defaults to 3.

### Example (ADB)

```bash
adb shell am broadcast \
  -a org.nitri.gpxplayer.ACTION_SET_LOCATION \
  -d "geo:37.4219983,-122.084" \
  --ei speed 12
```

If the app is not set as the mock location provider, it will open Developer Options and prompt you to select **GpxPlayer**.

## Project structure

- `MainActivity` handles the permission alert UI.
- `MockLocationReceiver` receives broadcast intents and forwards them to the provider.
- `MockLocationProvider` configures test providers and injects mock location updates.

## Troubleshooting

- **No location updates**: confirm Developer Options ➜ *Select mock location app* is set to **GpxPlayer**.
- **Broadcast ignored**: verify the intent action and that the data URI starts with `geo:`.

## License

See [LICENSE](LICENSE).
