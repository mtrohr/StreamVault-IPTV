# 📺 StreamVault — Android TV IPTV Player

## Project Overview

StreamVault is a production-grade Android TV IPTV player targeting Android 14+ (API 34). It provides Live TV, VOD (Movies), Series, Favorites with manual ordering, and EPG — comparable to IPTV Smarters Pro but with cleaner architecture and better maintainability.

---

## 🏗 High-Level Architecture

```
┌──────────────────────────────────────────────────────┐
│                    :app (Main)                       │
│  ┌────────────────────────────────────────────────┐  │
│  │           Jetpack Compose for TV UI            │  │
│  │  ┌──────┬──────┬──────┬──────┬──────────────┐  │  │
│  │  │LiveTV│Movies│Series│ Favs │  Settings     │  │  │
│  │  └──────┴──────┴──────┴──────┴──────────────┘  │  │
│  └─────────────────────┬──────────────────────────┘  │
│                        │                             │
│  ┌─────────────────────▼──────────────────────────┐  │
│  │              :domain (Use Cases)               │  │
│  │  Channel, Movie, Series, Episode, Program      │  │
│  │  IptvProvider interface                        │  │
│  │  Repository interfaces                         │  │
│  └──────────┬──────────────────────┬──────────────┘  │
│             │                      │                 │
│  ┌──────────▼───────┐  ┌──────────▼──────────────┐  │
│  │   :data           │  │     :player             │  │
│  │  Room DB          │  │  Media3 ExoPlayer       │  │
│  │  Xtream Client    │  │  PlayerEngine           │  │
│  │  M3U Parser       │  │  HLS/DASH/TS/Prog      │  │
│  │  XMLTV Parser     │  │  Decoder selection      │  │
│  │  Repositories     │  │  Error recovery         │  │
│  └──────────────────┘  └─────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

The UI never touches Xtream, M3U, or XMLTV directly. It operates exclusively on domain models.

---

## 📦 Module Breakdown

### `:domain`
Pure Kotlin, no Android dependencies.
- Domain models: `Channel`, `Movie`, `Series`, `Season`, `Episode`, `Program`, `Provider`, `Favorite`, `VirtualGroup`
- Interfaces: `IptvProvider`, `ChannelRepository`, `MovieRepository`, `SeriesRepository`, `EpgRepository`, `FavoriteRepository`
- Use cases: `GetChannels`, `GetMovies`, `GetSeries`, `GetEpg`, `ManageFavorites`, `LoginProvider`, `SearchContent`

### `:data`
Android library, implements `:domain` interfaces.
- **Room database**: Entities, DAOs, type converters, migrations
- **Xtream Codes client**: Retrofit service + response DTOs + mapper to domain
- **M3U parser**: Streaming line parser, tolerant of malformed input
- **XMLTV parser**: SAX/Pull parser for EPG data, timezone-safe
- **Repository implementations**: Combining network + cache with offline support

### `:player`
Android library, wraps Media3.
- `PlayerEngine` interface (domain-level abstraction)
- `Media3PlayerEngine` implementation
- `DataSource` factory supporting custom headers, tokens, cookies
- Decoder mode selection (Auto/HW/SW)
- Stream type detection + fallback
- Resume position tracking
- Error classification & recovery

### `:app`
The application module.
- Compose for TV UI (all screens)
- Navigation graph
- Hilt DI setup
- Theme / design system
- Focus management utilities

---

## ⚙️ Tech Stack Summary

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Language     | Kotlin 2.0+                         |
| UI           | Jetpack Compose for TV 1.0          |
| Player       | Media3 1.5 (ExoPlayer)              |
| Database     | Room 2.6                            |
| Networking   | OkHttp 4.12 + Retrofit 2.11        |
| Image Loading| Coil 3.x (Compose)                  |
| DI           | Hilt                                |
| Async        | Coroutines + Flow                   |
| Build        | Gradle Kotlin DSL, AGP 8.7         |
| Min SDK      | 34 (Android 14)                     |
| Target       | Android TV (Leanback launcher)      |

---

## 📡 Stream & Codec Support

### Protocols
- HLS (`.m3u8`) including AES-128 encrypted
- MPEG-TS over HTTP/HTTPS (`.ts`)
- DASH (`.mpd`)
- Progressive HTTP (MP4, MKV)
- Tokenized / expiring URLs
- Custom headers (Authorization, User-Agent, Cookie, Referrer)

### Video Codecs (device-dependent)
- H.264 (AVC) — universal
- H.265 (HEVC) — most modern devices, 4K where hardware supports
- MPEG-2 — best effort via hardware decoder

### Audio Codecs
- AAC, MP3 — universal
- AC3 / E-AC3 (Dolby Digital / Plus) — passthrough where device supports
- DTS — passthrough only if device allows

### Decoder Strategy
- Hardware first → automatic software fallback
- User-configurable: Auto / Hardware / Software
- No bundled FFmpeg, no soft-codec magic — honest about device limits

---

## 🔒 Explicit Assumptions & Limitations

### Assumptions
- Target devices are Android TV boxes running Android 14+
- Users provide their own Xtream Codes credentials or M3U URLs
- EPG data will be in XMLTV format
- Internet connection is available during content browsing

### Intentionally NOT Supported
- ❌ DRM (Widevine, PlayReady, FairPlay) — would require licensing
- ❌ Recording / PVR — requires local storage management beyond scope
- ❌ Timeshift (pausing live TV) — requires buffering infrastructure
- ❌ Picture-in-picture — Android TV PiP is limited and rarely useful
- ❌ Chromecast output — the app IS the TV target
- ❌ Mobile phone / tablet layout — Android TV only
- ❌ Multi-user profiles — future consideration
- ❌ Cloud sync — future consideration
- ❌ Stalker Portal / MAC-based providers — Xtream + M3U only for MVP
- ❌ Subtitle rendering from external sources — embedded subtitles only

### Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Xtream APIs are undocumented | Use well-known community-documented endpoints; build defensive parsing |
| M3U playlists are wildly inconsistent | Line-by-line streaming parser with extensive fallbacks |
| EPG data can be huge (100MB+) | SAX/Pull parser, streaming, background processing, incremental DB updates |
| Cheap TV boxes have limited RAM | Aggressive image caching, lazy loading, minimal overdraw |
| Providers rate-limit or block | Configurable request delays, retry with backoff |
| Stream URLs expire | Re-fetch URL on playback start, handle 403/410 gracefully |
| Android TV focus system is fragile | Explicit focus management, no reliance on implicit Compose guessing |

---

## 📐 Project Structure

```
streamvault/
├── app/
│   ├── src/main/
│   │   ├── java/com/streamvault/app/
│   │   │   ├── StreamVaultApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── di/                    # Hilt modules
│   │   │   ├── navigation/            # Navigation graph
│   │   │   └── ui/
│   │   │       ├── theme/             # Colors, typography, theme
│   │   │       ├── components/        # Reusable card, list, focus components
│   │   │       ├── screens/
│   │   │       │   ├── home/          # Live TV
│   │   │       │   ├── movies/        # Movies
│   │   │       │   ├── series/        # Series
│   │   │       │   ├── favorites/     # Favorites + virtual groups
│   │   │       │   ├── player/        # Playback screen
│   │   │       │   ├── epg/           # EPG timeline
│   │   │       │   ├── settings/      # Settings + provider mgmt
│   │   │       │   └── provider/      # Add/login provider
│   │   │       └── util/              # Focus helpers, modifiers
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── domain/
│   ├── src/main/java/com/streamvault/domain/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── usecase/
│   │   └── provider/
│   └── build.gradle.kts
├── data/
│   ├── src/main/java/com/streamvault/data/
│   │   ├── local/                     # Room entities, DAOs, database
│   │   ├── remote/
│   │   │   ├── xtream/                # Xtream Codes API
│   │   │   └── dto/                   # Network DTOs
│   │   ├── parser/
│   │   │   ├── m3u/                   # M3U parser
│   │   │   └── epg/                   # XMLTV parser
│   │   ├── mapper/                    # DTO → Domain mappers
│   │   └── repository/               # Repository implementations
│   └── build.gradle.kts
├── player/
│   ├── src/main/java/com/streamvault/player/
│   │   ├── PlayerEngine.kt           # Interface
│   │   ├── Media3PlayerEngine.kt     # Implementation
│   │   ├── StreamTypeDetector.kt
│   │   ├── CustomDataSourceFactory.kt
│   │   └── DecoderMode.kt
│   └── build.gradle.kts
├── build.gradle.kts                   # Root build file
├── settings.gradle.kts
├── gradle.properties
└── gradle/
    └── libs.versions.toml             # Version catalog
```

---

## 🗓 Development Phases

### Phase 1 — Foundation (Core infrastructure)
- Project scaffolding, Gradle config, version catalog
- Domain models and interfaces
- Room database schema
- Basic Hilt DI setup

### Phase 2 — Data Layer (Providers + parsing)
- Xtream Codes API client
- M3U parser
- XMLTV EPG parser
- Repository implementations

### Phase 3 — Player (Playback engine)
- Media3 wrapper
- Stream type detection
- Custom headers / tokens
- Decoder mode selection
- Error handling + recovery

### Phase 4 — UI Shell (Navigation + screens)
- Theme / design system
- Top-level navigation scaffold
- Live TV screen
- Movies screen
- Series screen

### Phase 5 — Features (EPG, Favorites, Settings)
- EPG timeline UI
- Favorites with manual ordering
- Virtual groups / collections
- Settings screen
- Provider management

### Phase 6 — Polish & Release
- Error handling across all layers
- Performance optimization
- APK signing
- Testing on emulator
