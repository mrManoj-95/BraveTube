# BraveTube — YouTube for Android TV

A sideloadable Android TV app with a YouTube-style interface: a focus-driven nav rail,
home shelves, search with an on-screen remote keyboard, channel pages, and full-screen
playback with D-pad controls.

No Google account, no YouTube Data API key, no ads. Metadata and stream URLs come from
public [Piped](https://docs.piped.video) instances; playback is native
Media3 / ExoPlayer, so it uses your TV's hardware decoder rather than a WebView.

---

## Getting the APK

### Option A — let GitHub build it (no local setup)

1. Create a new repository on GitHub and push this project to it:

   ```bash
   cd BraveTube
   git init && git add . && git commit -m "BraveTube"
   git branch -M main
   git remote add origin https://github.com/<you>/BraveTube.git
   git push -u origin main
   ```

2. Open the repo's **Actions** tab. The `Build APK` workflow runs on every push
   (or run it manually with *Run workflow*).
3. When it finishes, download the **BraveTube-APK** artifact. It contains
   `app-debug.apk` and `app-release.apk` — either one installs.

The workflow generates a throwaway signing key so the release APK is installable.
If you plan to ship updates, replace it with your own keystore stored as repo secrets.

### Option B — build locally

Requires JDK 17 and the Android SDK (Android Studio installs both).

```bash
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Without a `KEYSTORE_FILE` environment variable the release build is signed with your
local debug key, which is fine for sideloading.

---

## Installing on the TV

Pick whichever is easiest:

**ADB over the network** (fastest if you have a computer nearby)

1. On the TV: *Settings → Device Preferences → About →* click **Build** seven times to
   enable Developer options.
2. *Settings → Device Preferences → Developer options →* turn on **USB debugging** /
   **Network debugging**, and note the TV's IP address.
3. From your computer:

   ```bash
   adb connect <tv-ip>:5555
   adb install -r app-release.apk
   ```

**Sideload without a computer**

1. Install *Downloader* (or any file manager) from the Play Store on the TV.
2. Put the APK somewhere reachable — a GitHub release URL, Google Drive direct link, or
   a USB stick.
3. Open it in the file manager and allow installs from that app when prompted.

The app registers a **LEANBACK_LAUNCHER** intent filter, so it appears on the Android TV
home row with its banner. It also registers a normal `LAUNCHER` filter, so it shows up on
TV boxes and phones that don't use the leanback launcher.

---

## Using the remote

| Where | Key | Does |
|---|---|---|
| Anywhere | ◀ from the first column | Opens the nav rail (it expands to show labels) |
| Browse | D-pad + OK | Move focus / open |
| Browse | BACK | Previous screen; exits from Home |
| Player | OK | Show controls, then play/pause |
| Player | ◀ ▶ | Seek 10 s |
| Player | ⏪ ⏩ (if present) | Seek 30 s |
| Player | ▲ | Quality menu |
| Player | ▼ | Up next — related videos |
| Player | BACK | Hide controls, then exit playback |

---

## How it works

```
Piped instance  ──/trending, /search, /channel──▶  browse UI (Compose)
                └─/streams/{id}──▶ videoStreams[] + audioStreams[]
                                        │
                        MergingMediaSource(video-only, audio-only)
                                        │
                                    ExoPlayer
```

- **`data/PipedApi.kt`** — OkHttp + kotlinx.serialization, with automatic failover across
  a list of public instances. Whichever instance answers first becomes the preferred one
  and is remembered.
- **`ui/player/StreamPicker.kt`** — chooses renditions. H.264 video and AAC audio are
  preferred over VP9/AV1/Opus because TV boxes vary a lot in what they can
  hardware-decode; a stream that plays smoothly beats a sharper one that stutters.
- **`ui/player/PlayerScreen.kt`** — YouTube gives video and audio as separate adaptive
  streams, so the player merges two `ProgressiveMediaSource`s with `MergingMediaSource`.
  Livestreams use the HLS manifest instead, and there's a progressive muxed fallback.
- **UI** — plain Compose Material3 with hand-rolled focus handling (`FocusCard` scales and
  outlines whatever the D-pad lands on) rather than Leanback fragments, which keeps the
  visual language close to the current YouTube app.

## Settings worth knowing

- **Content source** — if videos stop loading, switch the Piped instance. Public instances
  go up and down constantly; this is the first thing to try.
- **Trending region** — drives the Home and Trending shelves. Defaults to India.
- **Video quality** — caps the resolution the player picks. Drop it to 720p on older TV
  hardware.

History and recent searches are stored in `SharedPreferences` on the device only.

## Known limitations

- No sign-in, so no personal subscriptions, likes, or playlists. The home shelves
  approximate YouTube's layout using trending plus one curated query per category.
- Playback depends on the health of public Piped instances. When YouTube changes its
  stream signing, instances need a few days to catch up — switching instances usually
  works around it.
- Age-restricted and some region-locked videos won't resolve.

## Project layout

```
app/src/main/java/com/bravetube/tv/
├── BraveTubeApp.kt          Application + tiny manual DI (AppGraph)
├── MainActivity.kt          Back stack, nav rail wiring, deep links
├── data/
│   ├── Models.kt            Piped wire models + formatting helpers
│   ├── PipedApi.kt          HTTP client with instance failover
│   ├── Prefs.kt             SharedPreferences: instance, region, quality, history
│   └── Repository.kt        Home shelf definitions
└── ui/
    ├── components/          FocusCard, VideoCard, ShelfRow, VideoGrid, NavRail
    ├── screens/             Home, Search, Trending, History, Channel, Settings
    ├── player/              StreamPicker, PlayerScreen (ExoPlayer + TV controls)
    └── theme/               YouTube-dark palette and TV-sized typography
```
