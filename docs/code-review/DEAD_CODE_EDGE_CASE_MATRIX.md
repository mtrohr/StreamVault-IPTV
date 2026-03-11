# Dead Code, Reachability, and Edge-Case Matrix

## Progress Tracking (code-review branch)
- [x] `M3uParserTest` compilation fixed for new parser `ParseResult` contract.
- [x] `SyncManagerTest` fake DAO updated to implement `updateEpgUrl`.
- [x] Removed no-op behavior in `PreferencesRepository.clearDefaultViewMode`.
- [x] Player EPG collector fan-out fixed by consolidating under one cancellable parent job.
- [x] Duplicate EPG overlay paths consolidated.
- [x] Generated `domain/bin` tree removed from VCS.
- [x] Favorites N+1 resolution replaced with batched content lookups.
- [x] MultiView top-level back handling wired via `BackHandler`.
- [x] Search upgraded to FTS for channels/movies/series with SQL and UI result caps retained.
- [x] Local imported M3U files are trimmed on provider-list updates and post-save flows while preserving provider-referenced files.
- [x] Live player numeric channel entry implemented (buffer, preview, timeout commit, invalid feedback).
- [x] Player remote key semantics tightened (GUIDE/INFO/MENU support, layered Back behavior, no VOD zap on channel keys).
- [x] Home category rail now supports explicit DPAD-right handoff into content/search focus targets.
- [x] Player overlays migrated away from hardcoded/garbled icon text to localized string resources and ASCII-safe control labels.
- [x] Home screen now restores focus to last active category/channel after modal dialogs close (favorites/group/PIN/delete/category-options flows).
- [x] Player channel list and EPG overlays now persist last focused row/program and restore focus on reopen.
- [x] Provider edit flow no longer rehydrates stored passwords into UI state; blank password preserves existing encrypted secret.
- [x] Sensitive URL logging reduced by redacting discovered EPG URLs and alternative stream debug outputs.
- [x] Provider repository read paths now return redacted password fields instead of decrypted credential values.
- [x] Provider onboarding now persists sync outcome in provider status (`ACTIVE`/`ERROR`) for clearer failure semantics.
- [x] M3U sync now aborts on empty/no-playable payloads before DB replace-all operations.
- [x] Provider local file import now surfaces storage-full specific error messaging.
- [x] SyncManager now retries transient network failures with bounded exponential backoff for Xtream, M3U HTTP fetch, and EPG refresh.
- [x] Player lifecycle now reacts to foreground/background transitions and flushes VOD progress on stop/dispose boundaries.
- [x] Player decoder errors now trigger one-time software-decoder fallback before hard-fail.
- [x] Manual provider refresh now updates provider sync status (`ACTIVE`/`ERROR`) consistently with onboarding sync.
- [x] Sync pipeline now exposes degraded-but-complete outcomes via `SyncState.Partial` and provider `PARTIAL` status.
- [x] Settings now surfaces `PARTIAL` refresh outcomes with explicit warning snackbar and provider status badge.
- [x] Settings now includes per-section warning detail text (snackbar + provider card strip) for partial syncs.
- [x] Warning strips now include actionable section-retry controls with real backend handlers (`EPG`, `Movies`, `Series` where supported).
- [x] Live zapping now has a buffering-timeout watchdog with automatic fallback to the previous channel.
- [x] Settings popups (language/parental level) migrated from default alert style to premium TV dialog styling.
- [x] Player channel list overlay now shows group-local numbering and uses animated list scrolling without focus-scale jump.
- [x] Adult-content detection is now centralized with case-insensitive matching and reused by both M3U sync and Xtream mapping.
- [x] Xtream series and episode detail paths now inherit adult status from classified series categories instead of hardcoding `false`.
- [x] Parental-control category management now spans LIVE/MOVIE/SERIES and scopes protection writes by category type.

## C. Dead Code / Redundancy / Reachability

### Duplicated logic
- Resolved in `code-review`: duplicate EPG overlay file removed.
  - Kept canonical implementation in `app/src/main/java/com/streamvault/app/ui/screens/player/PlayerScreen.kt`.

### Unused or suspicious structures
- Potentially unused parser helpers:
  - `data/src/main/java/com/streamvault/data/parser/M3uParser.kt:100` (`parseToChannels`)
  - `data/src/main/java/com/streamvault/data/parser/M3uParser.kt:183` (`isVodEntry`)

### Unreachable / suspicious branches
- Parental control loading/state flow appears inconsistent and may represent dead branch behavior:
  - `app/src/main/java/com/streamvault/app/ui/screens/settings/parental/ParentalControlGroupViewModel.kt:31-38,48`

### Simplification candidates
- Introduce shared badge composables and shared focus surface patterns.
- Remove duplicated overlay rendering paths.
- Delete generated source artifacts from repository and CI packaging.

## F. Edge Case Matrix

| Edge Case | Status | Evidence / Note |
|---|---|---|
| No internet during provider add | Covered well | Provider add flow now returns explicit error when initial sync fails (`ProviderRepositoryImpl.kt`). |
| Slow internet / high latency | Partially handled | Sync pipeline now retries transient failures with bounded exponential backoff; UX-level retry feedback/control is still basic. |
| Playlist partially loads | Partially handled | Initial sync failures surface explicitly; degraded section refreshes now mark provider `PARTIAL`, surface section-level warnings, and provide section-specific retry actions from Settings. |
| Invalid playlist URL/file | Covered well | Validation/error paths present in setup flow. |
| Empty playlist | Covered well | M3U sync now fails fast on empty/no-playable payloads and does not perform destructive replace-all writes. |
| Huge playlist (50k+ channels/items) | Partially handled | Favorites N+1 removed and FTS-backed search added; full stress/perf validation still pending. |
| Broken stream URL | Partially handled | Player now auto-falls back to previous channel when live zap stalls in buffering/error state; broader multi-strategy fallback UX is still limited. |
| Missing EPG | Partially handled | EPG optional paths exist, but stale/missing signaling is weak. |
| Corrupted provider response | Partially handled | Exceptions are handled and transient failures are retried; semantic validation/recovery for malformed payload subsets is still limited. |
| Resume after device sleep | Partially handled | Player observes lifecycle and now auto-pauses on `ON_STOP` with conditional resume on `ON_START`; still needs long-run device sleep stress validation. |
| Resume after process death | Partially handled | ViewModel/state restore is mixed; no clear end-to-end guarantee observed. |
| Returning from background during playback | Covered well | Player screen now wires lifecycle observer (`ON_STOP`/`ON_START`) and VM persists VOD progress + resumes only when session was playing pre-background. |
| Rapid channel switching | Partially handled | `PlayerViewModel.fetchEpg` now uses one cancellable parent job for all EPG collectors. |
| Repeated play/stop/open/close | Partially handled | Improved with lifecycle-driven pause/resume and progress flush; still requires endurance validation for long sessions/memory pressure. |
| Storage full on import/cache | Partially handled | Local M3U import now reports explicit storage-full error; broader cache/storage-pressure handling is still limited. |
| Permission issues on file import | Partially handled | Try/catch exists and setup now shows explicit import-failure feedback; retry/help UX is still basic. |
| Low memory conditions | Unknown | No targeted low-memory handling observed. |
| Unsupported codecs | Partially handled | Improved: decoder errors now attempt one software-decoder fallback automatically; still needs device-level codec compatibility test matrix. |
| Multiple input methods/remotes | Partially handled | D-pad, numeric entry, GUIDE/INFO/MENU remote keys, and back-stack behavior are improved; hardware keyboard-specific UX remains basic. |
| Provider metadata inconsistencies | Partially handled | Category-level adult classification is now centralized and propagated for M3U/Xtream, but provider naming remains heuristic when no explicit metadata exists. |
| Duplicate/empty categories | Partially handled | UI supports category lists; robust dedupe strategy unclear. |
| Large dataset search | Partially handled | FTS-backed search is in place with SQL/UI caps; ranking and stress tuning are still pending. |
| First launch vs returning user | Covered well | Welcome/setup split exists; still needs stronger failure-state signaling. |

## Tracking Notes
- Use this file as implementation checklist.
- Mark each row to `covered well` only after test evidence (unit/integration/manual scenario).
