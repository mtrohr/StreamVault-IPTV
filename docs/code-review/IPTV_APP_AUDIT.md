# IPTV Android TV App Audit (Source-Level)

## Scope and Confidence
- Analysis type: source-level review (not APK-only); findings are based on current repository code.
- Confidence: high for issues with direct file/line evidence; medium for runtime behavior not fully exercised.
- Date: 2026-03-09
- Branch: code-review

## Progress Tracking (code-review branch)
- [x] Register Room migration `6 -> 7` in DI database builder.
- [x] Disable OkHttp logging in non-debug builds.
- [x] Fix Xtream API host handling by switching to dynamic `@Url` endpoint calls.
- [x] Stop returning silent success when initial provider sync fails.
- [x] Mask provider password input in setup UI.
- [x] Fix broken unit test compilation (`M3uParserTest`, `SyncManagerTest`).
- [x] Hash/salt parental PIN storage and remove PIN from backup export/import.
- [x] Encrypt provider password-at-rest with Keystore-backed AES-GCM (transparent decrypt on read).
- [x] Disable Android auto-backup (`allowBackup=false`).
- [x] Scope category protection DB updates by provider (prevents cross-provider lock updates).
- [x] Scope in-memory unlocked categories by provider (session unlock isolation fixed).
- [x] Add explicit forced refresh path that bypasses TTL for user-triggered sync.
- [x] Tie all player EPG collectors to one cancellable parent job (prevents zap-time collector buildup).
- [x] Remove generated `domain/bin` source tree from VCS and ignore it in `.gitignore`.
- [x] Consolidate duplicate EPG overlay implementation to a single component path.
- [x] Rework provider-scoped EPG persistence/query model.
- [x] Preserve remote content IDs separately (`streamId` / `seriesId` / `episodeId`) in domain mapping.
- [x] Stabilize sync identity by remapping local IDs from provider+remote ID lookups before provider replace-all writes.
- [x] Rework provider-scoped identity model for channels/movies/series/programs (including legacy ID normalization migration `8 -> 9`).
- [x] Add migration test coverage for Room upgrade paths including `9 -> 10` FTS schema migration.
- [x] Stop playback-history watch count inflation during periodic resume updates.
- [x] Implement series details fetch+persistence (Xtream seasons/episodes) with local fallback.
- [x] Replace favorites N+1 hydration with batched `IN` queries by content type.
- [x] Add top-level `BackHandler` integration to MultiView screen and clean broken overlay labels.
- [x] Replace `%LIKE%` search scans with provider-scoped FTS queries (plus SQL/UI result caps).
- [x] Add local imported M3U cleanup policy (retain latest files, delete stale imports).
- [x] Implement timed numeric channel entry (digit buffer + preview + delayed commit + invalid feedback) in player.
- [x] Add explicit network security policy via manifest `networkSecurityConfig`.
- [x] Harden Provider Setup local-file UX (localized labels + user-facing import failure messaging).
- [x] Clean Kotlin warnings in active TV flows (`HomeViewModel`, `MultiViewPlannerDialog`).
- [x] Polish player remote behavior: layered Back handling and GUIDE/INFO/MENU mappings; prevent VOD channel-zap key side effects.
- [x] Improve Home TV focus navigation from category rail to content/search via explicit DPAD-right handoff.
- [x] Normalize player overlay copy/icons to resource-driven TV labels (remove garbled/hardcoded symbols in controls, channel list, and EPG overlays).
- [x] Add Home focus restoration memory (return to last focused category/channel after dialog/pin/delete/options modal dismissal).
- [x] Add Player overlay focus restoration memory (channel list + EPG remember and restore last focused item across close/reopen).
- [x] Stop exposing stored Xtream passwords in edit UI; blank edit password now preserves existing encrypted secret.
- [x] Redact sensitive URL data from sync/player debug logging paths.
- [x] Stop returning decrypted provider passwords from repository read APIs (`getProviders`/`getProvider`/`getActiveProvider` now return redacted password fields).
- [x] Persist provider sync health on onboarding (`ACTIVE` on successful initial sync, `ERROR` on failed initial sync).
- [x] Guard M3U sync against empty/no-playable payloads before replace-all writes (prevents accidental data wipe on malformed/empty playlists).
- [x] Improve local M3U import error UX with explicit storage-full messaging.
- [x] Add transient retry/backoff for sync network paths (Xtream fetches, M3U HTTP download, and EPG refresh) to reduce failures on slow/unstable links.
- [x] Add lifecycle-aware player foreground/background handling (auto-pause/conditional resume + VOD progress flush on stop/dispose).
- [x] Add decoder-error recovery path (one-shot fallback to software decoder mode before failing playback).
- [x] Update provider sync health during manual refresh paths (`ACTIVE` on success, `ERROR` on failure).
- [x] Add partial-sync semantics (`SyncState.Partial` + provider `PARTIAL` status) for non-fatal degraded refreshes.
- [x] Surface partial-sync outcomes in Settings UX (provider status badge + warning snackbar on refresh).
- [x] Show per-section partial warning details in Settings refresh feedback and provider cards.
- [x] Add actionable partial-warning remediation buttons in Settings (`Retry EPG`, `Retry Movies`, `Retry Series`) backed by section-specific sync retries.
- [x] Add live zap buffering watchdog with automatic fallback to previous channel when channel switch stalls.
- [x] Replace old Settings selection popups with TV-styled premium dialogs (level/language selectors).
- [x] Improve player channel-list overlay with group-local numbering and smoother focus/scroll behavior during long-list navigation.
- [x] Centralize adult-content category classification with case-insensitive matching for M3U and Xtream.
- [x] Apply Xtream parental classification by category for series/items instead of leaving series permanently unprotected.
- [x] Expand parental category management to LIVE, Movies, and Series with type-scoped protection updates.

## A. Executive Summary
- Overall quality rating: 5.8/10 (strong ambition, fragile internals, TV UX partially mature)
- Main strengths:
  - Clear module split (`app`, `data`, `domain`, `player`)
  - Broad feature scope (Live, Movies, Series, EPG, multiview, parental controls)
  - Compose TV-first UI foundation with focus handling in many screens
- Main weaknesses:
  - Several critical data/network correctness risks
  - Security/privacy posture is below production baseline
  - High coupling between provider sync and local schema assumptions
  - UI styling is inconsistent and partly non-native/polish-deficient for premium TV apps
- Biggest risks:
  - Xtream connection flow may fail due base URL wiring
  - DB migration path mismatch can break upgrades
  - Cross-provider data corruption/overlap risk for IDs and EPG
  - Memory/leak-like behavior from uncancelled collector jobs in player
- Biggest opportunities:
  - Fix data model boundaries (provider-scoped IDs and EPG keys)
  - Harden sync/retry/error states
  - Improve premium TV UX consistency and focus ergonomics
  - Implement secure credential storage and logging hardening

## B. Findings by Category

### 1) Xtream API base URL wiring is likely broken
- Severity: Critical
- Area: Reliability / Networking / IPTV
- Status: Done in `code-review` (dynamic `@Url` endpoint implementation)
- What is wrong:
  - Retrofit service is created with fixed placeholder base URL (`https://placeholder.com/`). Xtream API methods are static relative paths and do not use dynamic `@Url`.
- Why it matters:
  - Authentication/data calls can hit wrong host or fail for real providers.
- Evidence:
  - `app/src/main/java/com/streamvault/app/di/NetworkModule.kt:49-53`
  - `data/src/main/java/com/streamvault/data/remote/xtream/XtreamApiService.kt:17-21`
  - `data/src/main/java/com/streamvault/data/remote/xtream/XtreamProvider.kt:23-25`
  - `data/src/main/java/com/streamvault/data/sync/SyncManager.kt:110-114`
- Recommended fix:
  - Either build Retrofit per-provider base URL, or change service methods to dynamic `@Url` and pass full endpoint per call.

### 2) Room migration registration mismatch
- Severity: Critical
- Area: Reliability / Storage
- Status: Done in `code-review` (registered missing migrations and added migration instrumentation test coverage including `9 -> 10`)
- What is wrong:
  - DB version is 7 and `MIGRATION_6_7` exists, but DI builder registers migrations only to `5_6`.
- Why it matters:
  - App upgrades from v6 can fail at startup.
- Evidence:
  - `data/src/main/java/com/streamvault/data/local/StreamVaultDatabase.kt:24,141-146`
  - `app/src/main/java/com/streamvault/app/di/DatabaseModule.kt:26-31`
- Recommended fix:
  - Register `StreamVaultDatabase.MIGRATION_6_7` in Hilt database module and add upgrade test.

### 3) External IDs are written into auto-generated primary keys
- Severity: Critical
- Area: Architecture / Data Integrity
- Status: Done in `code-review` (remote IDs are explicit domain fields; sync writes keep local PK internal; migration `8 -> 9` normalizes legacy local IDs and remaps favorites/history/episode links)
- What is wrong:
  - Entities define `id` as auto-generated local PK but mappers set `id = externalStreamId`.
- Why it matters:
  - Collisions and inconsistent identity across providers; broken references and updates.
- Evidence:
  - `data/src/main/java/com/streamvault/data/local/entity/Entities.kt:39-43,69-73,106-110`
  - `data/src/main/java/com/streamvault/data/mapper/EntityMappers.kt:64-67,114-117`
- Recommended fix:
  - Keep local PK internal; store remote IDs in dedicated columns with provider-scoped unique indices.

### 4) EPG persistence is not provider-scoped and has unstable IDs
- Severity: Critical
- Area: IPTV / Data Integrity / Reliability
- Status: Done in `code-review` (program table now carries `provider_id`; DAO/repository/viewmodel queries are provider-scoped; migration `7 -> 8` added)
- What is wrong:
  - Program table/query paths are not fully provider-scoped; mapper keeps parser IDs that can default to 0.
- Why it matters:
  - EPG overlap, duplicate inserts, wrong now-playing data between providers.
- Evidence:
  - `data/src/main/java/com/streamvault/data/local/entity/Entities.kt:180-198`
  - `data/src/main/java/com/streamvault/data/local/dao/Daos.kt:257-258`
  - `data/src/main/java/com/streamvault/data/repository/EpgRepositoryImpl.kt:58-78`
  - `data/src/main/java/com/streamvault/data/mapper/EntityMappers.kt:268-277`
- Recommended fix:
  - Add provider key to program identity, define stable composite uniqueness, and enforce provider filter in queries.

### 5) Provider setup can succeed while initial sync failed
- Severity: High
- Area: Reliability / UX
- Status: Done in `code-review` (setup now returns error on initial sync failure; successful sync sets `ACTIVE`; degraded non-fatal sync sections now map to `PARTIAL`; hard failures map to `ERROR`, across onboarding and manual refresh paths)
- What is wrong:
  - Exceptions during first sync are swallowed and setup still returns success.
- Why it matters:
  - User sees provider as added but data is incomplete/empty.
- Evidence:
  - `data/src/main/java/com/streamvault/data/repository/ProviderRepositoryImpl.kt:109-118,163-171`
- Recommended fix:
  - Surface sync status explicitly and mark provider as `degraded` until first successful sync.

### 6) Parental lock operations are not provider-scoped
- Severity: High
- Area: Correctness / Security / IPTV
- Status: Done in `code-review` (DB write paths and in-memory unlock model are provider-scoped)
- What is wrong:
  - Category lock updates match only category ID; memory unlock state also lacks provider dimension.
- Why it matters:
  - Locks can apply incorrectly across providers with same category IDs.
- Evidence:
  - `data/src/main/java/com/streamvault/data/local/dao/Daos.kt:78-79,146-147,182-183,245-246`
  - `data/src/main/java/com/streamvault/data/repository/CategoryRepositoryImpl.kt:26-33`
  - `domain/src/main/java/com/streamvault/domain/manager/ParentalControlManager.kt:11-26`
- Recommended fix:
  - Scope lock state by `(providerId, categoryId)` in DB and in-memory manager.

### 7) Player EPG collection jobs can accumulate during channel zapping
- Severity: High
- Area: Performance / Reliability
- Status: Done in `code-review` (single parent `epgJob` now owns now/next/history/upcoming collectors)
- What is wrong:
  - `fetchEpg` cancels one job but spawns additional collectors that are not fully tied to a single cancellable handle.
- Why it matters:
  - Increased memory/CPU and inconsistent UI updates during fast channel switching.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/player/PlayerViewModel.kt:386-397,405-425`
- Recommended fix:
  - Use one structured `channelEpgJob` per channel and cancel/restart atomically.

### 8) Series details loading appears incomplete
- Severity: High
- Area: IPTV / Correctness
- Status: Done in `code-review` (Xtream `getSeriesInfo` is fetched by remote `series_id`, persisted to local `series`/`episodes`, and returned with merged seasons/episodes; local-cache fallback on remote failure)
- What is wrong:
  - Repository returns local series row only; detail VM expects seasons/episodes structure.
- Why it matters:
  - Empty/partial series detail screen in real-world providers.
- Evidence:
  - `data/src/main/java/com/streamvault/data/repository/SeriesRepositoryImpl.kt:75-78`
  - `app/src/main/java/com/streamvault/app/ui/screens/series/SeriesDetailViewModel.kt:51-60`
- Recommended fix:
  - Add remote details fetch + local relational persistence for seasons/episodes.

### 9) Credentials and PIN are stored unsafely
- Severity: High
- Area: Security / Privacy
- Status: Partially done in `code-review` (PIN hashed/salted, PIN removed from backup export/import, provider password-at-rest encrypted, edit flow no longer pre-fills stored password, provider repository read APIs now redact password fields by default, and sensitive stream/EPG URL logging is redacted; broader secrets hardening still pending)
- What is wrong:
  - Provider credentials saved plaintext in Room; parental PIN stored plaintext with default `0000`; backup includes PIN.
- Why it matters:
  - Device compromise or backups can expose sensitive account controls.
- Evidence:
  - `data/src/main/java/com/streamvault/data/local/entity/Entities.kt:18-20`
  - `data/src/main/java/com/streamvault/data/preferences/PreferencesRepository.kt:46-49,69-72`
  - `data/src/main/java/com/streamvault/data/manager/BackupManagerImpl.kt:36-39,91-97`
- Recommended fix:
  - Encrypt credentials with Android Keystore backed keys; hash PIN with strong KDF; remove secrets from backup payloads.

### 10) Network logging and backup flags are production-risky
- Severity: High
- Area: Security
- Status: Done in `code-review` (release logging disabled and `allowBackup=false`)
- What is wrong:
  - OkHttp logging interceptor is BASIC globally; `allowBackup=true` in manifest.
- Why it matters:
  - Metadata leakage and broader extraction surface.
- Evidence:
  - `app/src/main/java/com/streamvault/app/di/NetworkModule.kt:33-36`
  - `app/src/main/AndroidManifest.xml:18`
- Recommended fix:
  - Disable network logging for release builds; review backup policy and sensitive exclusions.

### 11) Unit tests do not compile in current state
- Severity: High
- Area: Maintainability / Reliability
- Status: Done in `code-review` (`./gradlew test` passes)
- What is wrong:
  - Parser test expects wrong return type; fake DAO missing new method.
- Why it matters:
  - CI quality gate is broken; regression risk is high.
- Evidence:
  - `data/src/test/java/com/streamvault/data/parser/M3uParserTest.kt:36-41`
  - `data/src/main/java/com/streamvault/data/parser/M3uParser.kt:53`
  - `data/src/test/java/com/streamvault/data/sync/SyncManagerTest.kt:38-50`
- Recommended fix:
  - Update tests to new parser contract and DAO interface; enforce CI on PR.

### 12) Manual refresh is not truly forced
- Severity: Medium
- Area: UX / Reliability
- Status: Done in `code-review` (`ProviderRepository.refreshProviderData(force=true)` wired from user-triggered refresh actions)
- What is wrong:
  - Settings refresh path can still be gated by TTL checks in sync manager.
- Why it matters:
  - User requests refresh but stale data remains.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/settings/SettingsViewModel.kt:53-55,86-89`
  - `data/src/main/java/com/streamvault/data/sync/SyncManager.kt:122-193`
- Recommended fix:
  - Add explicit `force=true` path that bypasses TTL for user-triggered refresh.

### 13) Playback history watch count is inflated
- Severity: Medium
- Area: Data Quality / UX
- Status: Done in `code-review` (periodic resume updates now preserve existing `watchCount` instead of incrementing on each tick)
- What is wrong:
  - Resume updates every 5 seconds may increase watch count repeatedly for same session.
- Why it matters:
  - Incorrect popularity/recent metrics and resume UX noise.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/player/PlayerViewModel.kt:363-381`
  - `data/src/main/java/com/streamvault/data/repository/PlaybackHistoryRepositoryImpl.kt:31-40`
- Recommended fix:
  - Increment watch count on session start/end events, not periodic progress ticks.

### 14) Search path is not scalable for large datasets
- Severity: Medium
- Area: Performance / IPTV
- Status: Done in `code-review` (provider-scoped FTS search implemented for channels/movies/series; SQL and UI caps remain in place)
- What is wrong:
  - Previous implementation used `%query%` LIKE scans without FTS/index strategy, which was expensive on large datasets.
- Why it matters:
  - Large playlists will lag and hurt remote responsiveness.
- Evidence:
  - `data/src/main/java/com/streamvault/data/local/dao/Daos.kt:51-52,96-97,158-159`
  - `app/src/main/java/com/streamvault/app/ui/screens/search/SearchScreen.kt:93-100`
- Recommended fix:
  - Add Room FTS tables or prefix indexing, debounce, and result limits.

### 15) Favorites screen has N+1 lookup behavior
- Severity: Medium
- Area: Performance
- Status: Done in `code-review` (FavoritesViewModel now batch-resolves channel/movie/series records through `get*ByIds` flow combines)
- What is wrong:
  - For each favorite, additional repository lookups are performed.
- Why it matters:
  - Slow loading and unnecessary DB work with large favorites list.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/favorites/FavoritesViewModel.kt:37-65`
- Recommended fix:
  - Replace with joined query returning fully typed favorite payloads in one flow.

### 16) TV key behavior is incomplete for numeric channel input
- Severity: Medium
- Area: Android TV UX
- Status: Done in `code-review` (live player now supports buffered numeric entry with preview, timeout auto-commit, Enter commit, and invalid-input feedback; `0` retains last-channel quick recall when buffer is empty; remote GUIDE/INFO/MENU behavior and Back stack were tightened for TV expectations)
- What is wrong:
  - Supports `KEYCODE_0` recall behavior but no robust direct numeric entry workflow.
- Why it matters:
  - Power users with remotes expect fast channel number zapping.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/player/PlayerScreen.kt:285-287`
- Recommended fix:
  - Implement timed numeric buffer + candidate preview + confirm/fallback behavior.

### 17) MultiView screen back handling appears incomplete
- Severity: Medium
- Area: Android TV UX / Navigation
- Status: Done in `code-review` (explicit `BackHandler` wired to `onBack`; focused-slot overlays cleaned)
- What is wrong:
  - Screen API includes `onBack` but no clear local `BackHandler` integration at top-level.
- Why it matters:
  - Potential inconsistent exit behavior for remote back key.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/multiview/MultiViewScreen.kt:33-35`
- Recommended fix:
  - Add explicit `BackHandler` contract and focus-safe exit path.

### 18) Provider setup password field is visible text
- Severity: Medium
- Area: Security / UX
- Status: Done in `code-review` (password masking added)
- What is wrong:
  - Password input is plain text field without masking.
- Why it matters:
  - Credential exposure on-screen in shared living-room environments.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/provider/ProviderSetupScreen.kt:176-178`
- Recommended fix:
  - Add visual transformation and optional reveal toggle.

### 19) Local file-imported playlists may accumulate
- Severity: Medium
- Area: Storage / Maintainability
- Status: Done in `code-review` (cleanup now runs on every provider-list change and after successful provider save/edit; stale `m3u_*.m3u` files are trimmed while preserving currently referenced local playlists)
- What is wrong:
  - M3U import writes timestamped files under app files without visible cleanup lifecycle.
- Why it matters:
  - Wasted storage and stale artifacts.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/provider/ProviderSetupScreen.kt:73-76`
- Recommended fix:
  - Track ownership and garbage-collect old imported playlists.

### 20) Potential cleartext compatibility ambiguity
- Severity: Low
- Area: Security / Compatibility
- Status: Done in `code-review` (explicit `network_security_config` added and wired in manifest)
- What is wrong:
  - No explicit network security config policy for HTTP-only providers.
- Why it matters:
  - Either runtime failures for HTTP providers or unintended broad cleartext allowance later.
- Evidence:
  - Manifest/config inspection (no explicit cleartext policy observed in app manifest).
- Recommended fix:
  - Define network security config with minimal allowed domains/policies.

### 21) Duplicate EPG overlay implementations
- Severity: Low
- Area: Maintainability / Redundancy
- Status: Done in `code-review` (removed redundant `EpgOverlay.kt`; only `PlayerScreen` overlay remains)
- What is wrong:
  - EPG overlay logic exists in separate files and can drift.
- Why it matters:
  - Inconsistent UX and duplicated bug fixes.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/player/PlayerScreen.kt:1343`
  - `app/src/main/java/com/streamvault/app/ui/screens/player/EpgOverlay.kt:27`
- Recommended fix:
  - Consolidate into a single reusable overlay component.

### 22) Generated `domain/bin` sources are committed
- Severity: Low
- Area: Maintainability / Hygiene
- Status: Done in `code-review` (tree removed from VCS; `domain/bin/` ignored)
- What is wrong:
  - Repository includes generated duplicate source tree.
- Why it matters:
  - Confusion, stale code, noisy diffs.
- Evidence:
  - `domain/bin/main/...`
- Recommended fix:
  - Remove generated outputs from VCS and enforce ignore rules.

### 23) Player lifecycle handling was not foreground-aware
- Severity: Medium
- Area: Reliability / Android TV
- Status: Done in `code-review` (screen lifecycle now pauses on background, conditionally resumes on foreground, and flushes VOD progress on stop/dispose)
- What is wrong:
  - Playback/progress tracking paths were not tied to screen lifecycle events, so background/sleep transitions could continue stale state updates.
- Why it matters:
  - Leads to unstable resume behavior, unnecessary background writes, and weaker resilience when returning to playback after sleep/home transitions.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/player/PlayerScreen.kt:135-146`
  - `app/src/main/java/com/streamvault/app/ui/screens/player/PlayerViewModel.kt:392-434,780`
- Recommended fix:
  - Observe lifecycle (`ON_STOP`/`ON_START`) in player screen, pause/resume intentionally, and persist VOD progress at lifecycle boundaries.

### 24) Decoder failures had no runtime fallback strategy
- Severity: Medium
- Area: Reliability / Playback
- Status: Done in `code-review` (decoder errors now trigger one-time software-decoder fallback and replay attempt)
- What is wrong:
  - Decoder errors previously surfaced directly to error UI with no automatic recovery strategy.
- Why it matters:
  - IPTV providers often mix problematic streams/codecs; immediate hard-fail hurts playback continuity and perceived quality.
- Evidence:
  - `app/src/main/java/com/streamvault/app/ui/screens/player/PlayerViewModel.kt:127-134,275-276`
- Recommended fix:
  - On decoder failure, attempt one bounded fallback path (software decoder) before surfacing hard failure.

## D. Premium App Gap Analysis

### What premium IPTV apps do better
- Provider onboarding trust flow: visible connectivity test, masked credentials, clear error categories, and first-sync progress with recover actions.
- Live TV channel zap ergonomics: direct number input, quick channel list overlay, instant last-channel switch, predictable back behavior.
- EPG reliability: resilient merge strategy, conflict resolution, timezone handling, clear stale/partial indicators.
- Large dataset handling: fast indexed search, lazy loading, aggressive caching, incremental rendering.
- Playback experience: clean overlays, stable focus paths, subtitle/audio controls, reliable resume and error recovery.

### What this app currently lacks
- Robust provider-scoped data model for IDs and EPG.
- Production-grade secrets handling and release logging policy.
- Mature first-sync state model (success/degraded/failed).
- High-confidence performance strategy for very large playlists.
- Fully premium visual consistency across setup, navigation, and playback overlays.

### What changes would have highest impact
1. Fix data identity and EPG scoping (prevents silent corruption and wrong content display).
2. Fix Xtream networking host handling and migration chain (prevents hard failures).
3. Harden provider setup + sync status model (prevents false success).
4. Ship secure credential/PIN handling (trust and compliance baseline).
5. Unify TV interaction model (focus, back, key handling, overlays) and polished visual system.

## E. Optimization Opportunities (Prioritized)
1. Done in `code-review`: provider-scoped DB indices for channels/programs + FTS search tables.
2. High impact: structured concurrency cleanup in player EPG observers.
3. High impact: batch queries for favorites/history instead of per-item lookups.
4. Medium impact: add force-refresh pathway bypassing TTL for explicit user action.
5. Medium impact: reduce recomposition pressure in large grids via stable item models and `derivedStateOf` guards.
6. Medium impact: optimize image loading policies for logos/posters (size hints, placeholders, cache strategy).
7. Medium impact: incremental EPG ingestion pipeline with bounded memory and checkpointing.
8. Low impact: remove dead/duplicate code paths and generated `domain/bin` tree.

## G. Refactoring Plan

### 1) Quick wins (1-3 days)
- Register missing migration in DI.
- Fix broken tests (`M3uParserTest`, `SyncManagerTest`).
- Mask password input in provider setup.
- Disable verbose/network logging in release.
- Add explicit UI messaging when initial sync fails.

### 2) Medium effort (3-10 days)
- Introduce forced refresh path and sync state model (`idle/running/partial/failed/success`).
- Consolidate EPG overlay component.
- Add provider-aware parental lock keying.
- Replace N+1 favorites and broad search with optimized DAO queries.

### 3) High-impact architectural changes (2-6 weeks)
- Rework entity identity model: local PK + provider-scoped remote IDs and constraints.
- Rebuild EPG persistence schema for provider/channel scoping and dedupe.
- Re-architect Xtream service host strategy.
- Add encrypted secret storage and secure backup strategy.

### 4) Nice-to-have premium upgrades
- Numeric channel input with timed buffer and visual confirmation.
- Rich channel zap mini-guide and fast category switching.
- Better fallback handling for broken logos/metadata.
- Premium visual polish pass across all major screens and overlays.
