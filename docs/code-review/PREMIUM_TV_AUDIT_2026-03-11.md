# StreamVault Premium TV Audit

Branch: `session/premium-audit-2026-03-11`  
Base: `master`  
Method: source-level audit plus targeted implementation pass  
Confidence: `confirmed` for code-backed findings, `likely` for runtime/perf behavior inferred from structure, `assumption` where device-level validation is still missing

## A. Executive Product Assessment
- Overall quality: `7.5/10`
- Biggest strengths:
  - `confirmed`: strong feature surface for an Android TV IPTV app: Live TV, Movies, Series, EPG, favorites, custom groups, parental control, resume history, multiview, numeric zapping, audio/subtitle switching.
  - `confirmed`: clean module split across `app`, `data`, `domain`, and `player`.
  - `confirmed`: the player stack is already better than average IPTV apps: Media3, error recovery, alternative-stream fallback, numeric input, diagnostics, catch-up entry points.
- Biggest weaknesses:
  - `confirmed`: several flows looked complete but were context-fragile. Search and Favorites were launching player with incomplete provider/category/internal IDs until this branch.
  - `confirmed`: the full-screen EPG was visually present but functionally weak: placeholder click handlers and only now-playing data.
  - `confirmed`: Movies and Series were still doing extra UI-side filtering/grouping work instead of using repository search for typed queries.
  - `confirmed`: the app now has a real dashboard-style Home screen instead of treating the Live TV browser as the landing surface; recent-live workflows still exist, but the extra `Recent` / `Last Channel` / `Last Group` chips were intentionally pulled back out of the Live TV browse header because that surface was getting jammed and low quality.
- Why it still does not fully feel premium:
  - `confirmed`: some screens are polished while others still expose placeholder behavior, hardcoded copy, or weaker empty/error states, though Movies/Series category copy and group-management affordances are stronger after this pass.
  - `likely`: huge-library responsiveness will still be uneven because Live is more optimized than VOD/Series browsing.
  - `likely`: full premium feel needs one more pass on information architecture, cross-screen consistency, and power-user workflows.
- Top opportunities:
  1. Finish Live TV information architecture: better recent-channel memory, better full EPG, better catch-up and stale-guide handling.
  2. Rework VOD/Series browsing for very large libraries beyond the first search optimization already implemented.
  3. Make favorites/groups and multi-provider workflows feel intentional instead of merely available.
  4. Unify TV-native polish across Search, Favorites, Settings, Series detail, and EPG.

## B. Full Feature Inventory
| Feature | Status | Notes |
|---|---|---|
| Xtream provider login | Full | `confirmed`: auth, sync, edit, active-provider switching exist. |
| M3U URL import | Full | `confirmed`: validation and sync path exist. |
| Local M3U file import | Full | `confirmed`: file picker, local copy, cleanup policy, storage-full messaging exist. |
| Multi-provider support | Full | `confirmed`: multiple providers, active provider, refresh, edit, delete. |
| Welcome/onboarding | Partial | `confirmed`: welcome split and auto-bypass exist; still basic for premium onboarding and trust-building. |
| Home dashboard | Partial | `confirmed`: Home now has a real premium direction: featured hero state, dashboard stats, provider-health surfacing, provider-warning drill-down, live-memory shortcut cards, favorite channels, recent channels, continue watching, and fresh Movies/Series shelves, plus category-aware deep links back into Live TV; it is much stronger, but still needs deeper personalization to feel fully premium. |
| Sync status / partial refresh warnings | Full | `confirmed`: provider status, partial warning actions, section retries exist. |
| Live TV categories | Full | `confirmed`: provider categories plus virtual favorites/groups/recent remain in the category rail; the browse surface was simplified in this pass so Live TV is back to being a channel browser instead of a pseudo-home screen. |
| Movies catalog | Full | `confirmed`: category rail, hero banner, continue watching, favorites/groups, a reachable saved-shortcuts strip for Favorites/custom groups, repository-backed all-library lens rows for Favorites, Continue Watching, Top Rated, and Fresh content, and a query-backed full-library browse mode. |
| Series catalog | Full | `confirmed`: category rail, continue watching, favorites/groups, detail screen, a reachable saved-shortcuts strip for Favorites/custom groups, repository-backed all-library lens rows for Favorites, Continue Watching, Top Rated, and Fresh content, and a query-backed full-library browse mode. |
| Series detail / seasons / episodes | Full | `confirmed`: series detail loads seasons/episodes and plays episodes. |
| Full-screen EPG | Full | `confirmed`: now actionable, category-aware, schedule-based, clearer about stale/missing data, exposes visible time-window controls and window labels, includes a dense aligned timeline grid with shared half-hour markers, current-time line, and time-scaled program placement, supports direct day browsing, has `Scheduled only` cleanup mode, guide-mode filters for timeline, anchor-time, and archive-ready browsing, now adds density/layout controls, full-window paging, persisted guide defaults, a favorite-channel-only mode, exact return-route handoff back from player, and a real TV-style program side panel with provider/channel/replay/progress metadata, surrounding schedule context, and stale-guide refresh. |
| Player EPG overlay | Full | `confirmed`: current/next/upcoming/history overlays exist. |
| Favorites | Partial | `confirmed`: the standalone saved-library route is now reachable from Home and behaves like a real hub with overview stats, type filters, sort presets, provider scope, section-jump chips, Continue Watching, Recent Live rows, higher-level saved presets including `Home Shelf`, and in-hub management actions; it is no longer effectively dead code, but it still lacks deeper merged-provider behavior and final cross-surface polish. |
| Custom virtual groups | Partial | `confirmed`: create/add/remove/delete/reorder/rename now exist in Live TV, Movies, and Series category sidebars, and saved shortcut cards can now long-press directly into those management flows; the overall saved-library UX is still fragmented across screens. |
| Continue watching | Partial | `confirmed`: movies and series continue-watching exists; live now has a separate recent-channels workflow instead of resume-style continue watching. |
| Resume playback | Full | `confirmed`: movies and series episodes resume with prompt. |
| Search | Partial | `confirmed`: FTS-backed search exists across Live/Movies/Series, Search exposes recent-query shortcuts with clear-history controls, and it now has visible no-provider/min-query states plus results-summary chips for one-click narrowing; it still lacks deeper filtering and stronger power-user UX. |
| Numeric channel entry | Full | `confirmed`: timed input, preview, invalid feedback, last-channel on `0`. |
| Channel zapping | Full | `confirmed`: next/previous, overlays, watchdog fallback, last channel. |
| Audio track switching | Full | `confirmed`: track discovery and selection exist. |
| Subtitle switching | Full | `confirmed`: subtitle track selection with disable option exists. |
| Video quality switching | Partial | `confirmed`: implemented as alternative-stream selection, not full adaptive/manual quality UX. |
| Catch-up / replay | Partial | `confirmed`: catch-up URL building and archive entry exist, and guide/player now expose provider-aware replay reasoning plus richer failure messages; provider compatibility handling and deeper archive intelligence still need more work. |
| Multiview / split screen | Full | `confirmed`: 4-slot multiview now includes slot manager, focused audio, saved presets, focused-slot replace/remove actions, audio pinning, device-tier performance modes, active-slot caps, staged decoder startup, runtime telemetry snapshots, sustained-load throttling, low-memory standby behavior, and thermal-aware slot reduction on supported Android TV devices. Manual lower-end device profiling is still required for final acceptance, but the in-app protection layer is now implemented. |
| Parental control | Full | `confirmed`: PIN, lock levels, category protection for Live/Movies/Series exist. |
| Backup / restore | Partial | `confirmed`: backup now includes expanded preferences, providers, playback history, promoted groups, and multiview presets, and Settings now exposes restore preview/conflict UX with section toggles and conflict-strategy selection; broader media/library validation and post-import reporting are still limited. |
| Settings | Partial | `confirmed`: provider, playback, parental, language, and backup controls already existed; this pass improves hierarchy with an overview card, section descriptions, cleaner provider status/warning copy, a real provider diagnostics panel with per-domain sync timestamps/counts and capability summaries, plus backup preview/import planning and a DVR status section, but deeper organization is still needed. |
| Localization | Full | `confirmed`: many translations and RTL handling exist. |
| Diagnostics overlay | Partial | `confirmed`: player stats now include provider/source, decoder mode, stream class, playback state, archive support, alternate stream count, channel error history, last failure reason, recovery actions, and troubleshooting hints; it is substantially stronger, but still not a dedicated end-to-end troubleshooting center. |
| Channel history / recent channels | Partial | `confirmed`: Live TV still exposes a reachable `Recent` category in the sidebar, and Player still surfaces recent channels plus remembered `Last Group` context inside playback overlays; the extra Live TV header shortcuts were removed because they cluttered browsing. |
| External player support | Missing | `confirmed`: no external player path found. |
| Recording | Partial | `confirmed`: local live DVR v1 now has a real recording manager, scheduled/manual recording state, reachable Player actions for record/stop/schedule, and a Settings surface for storage plus job management; it is still limited to direct stream URLs and does not yet include a recordings browser or background-robust scheduling. |
| Playlist merge / cross-provider unified library | Missing | `confirmed`: one active provider model, not merged unified browsing. |

## C. Partial / Incomplete Feature Report
- Favorites and custom groups
  - `confirmed`: storage and group membership are implemented.
  - `confirmed`: the main user path is the in-context category rails for Live, Movies, and Series, not the standalone Favorites route.
  - `confirmed`: Home now exposes the standalone saved-library route directly, so users have a real landing-surface entry point into that broader saved view.
  - `confirmed`: Live TV, Movies, and Series now support rename/delete/reorder management for custom groups directly from category long-press actions, with visible on-screen feedback.
  - `confirmed`: rename persistence had a real bug in Movies/Series because the UI was passing virtual negative IDs instead of stored group IDs; this branch fixes that.
  - `confirmed`: Movies and Series expose saved shortcuts in the main browse surface, but Live TV was deliberately simplified again after user review, so saved groups now stay primarily in the category rail there instead of competing with the channel grid.
  - `confirmed`: Movies and Series now keep saved shortcuts visible when you are inside a saved category, and they expose an explicit `Browse All` return path from that same strip instead of forcing a sidebar bounce.
  - `confirmed`: saved shortcut cards in Movies and Series can now long-press directly into group-management flows, so users do not have to backtrack to the sidebar just to rename, delete, or reorder a saved group.
  - `confirmed`: reorder mode now uses the long-pressed saved category itself in Live TV, Movies, and Series instead of accidentally reordering whatever category happened to be open or whatever preview shelf data was already on screen.
  - `confirmed`: Movies and Series saved-category views now show an inline saved-group context card with item counts plus direct `Manage` and `Browse All` actions, so saved views feel like intentional destinations instead of generic filtered grids.
  - `confirmed`: saved shortcut ordering is cleaner now: no duplicate `Favorites`, and empty groups are no longer surfaced as if they were meaningful saved destinations.
  - `confirmed`: the standalone Saved Library screen now exposes a proper overview card, type filters, quick section jumps, direct Continue Watching plus Recent Live rows, visible browse sort presets for manual order, recent activity, and alphabetical browsing, and higher-level saved presets like `Watch Next`, `Live Recall`, Movies, Series, and Custom Groups.
  - `confirmed`: the Saved Library hub now also exposes managed custom-group actions directly inside the route itself: remove from current saved context, move items between groups, merge groups, delete groups, and pin or unpin live groups for Home surfacing.
  - `confirmed`: Home now respects promoted live groups from the Saved Library hub, so the cross-surface saved model is no longer purely passive.
  - `confirmed`: the Saved Library hub now supports current-provider versus all-provider scoping without leaving the route.
  - `confirmed`: the Saved Library hub now also exposes a dedicated `Home Shelf` preset so live favorites and promoted live groups can be reviewed as a real dashboard policy, not just hidden long-press state.
  - Still incomplete: current-provider scope is real, but `All Providers` is still a cross-provider filter over saved/history data, not a true merged-provider browse model, and saved-group policy controls still stop at Home promotion.
  - Production-grade finish: keep unifying favorites, recents, continue watching, and custom groups into one coherent library model while adding true merged-provider behavior, stronger preset policy, and richer in-place actions.
- Recent channels / live history
  - `confirmed`: live playback now records into history and Live TV exposes a dedicated `Recent` virtual category in the category rail.
  - `confirmed`: `Recent` now stays visible in the Live TV category rail even before it has any channel history, so the feature is actually discoverable from reachable UI.
  - `confirmed`: this pass removes the extra Live TV header shortcuts for `Recent`, `Last Channel`, and `Last Group`; those controls were making the browse surface feel jammed instead of premium.
  - `confirmed`: Home now surfaces live memory where it belongs: recent channels plus quick-access shortcuts for Favorites, Recent, Last Group, and populated custom live groups.
  - `confirmed`: Player still surfaces recent channels and the remembered `Last Group`, so the live-memory model remains available where it is more contextually appropriate.
  - `confirmed`: Player now surfaces recent channels through the live info/channel-list flow instead of trapping recents on Home only.
  - `confirmed`: Player now also exposes the remembered `Last Group` through the live info overlay and channel-list header, so the browsing-memory model carries into playback instead of stopping at the Live TV screen.
  - Still incomplete: no top-nav shortcut, no richer timeline/history presentation, and no broader recall model outside Live TV, Player, Home, and Saved Library.
  - Production-grade finish: add recent-channel row/shortcut entry points, mini-guide recall, and broader provider-aware surfacing across Live TV, Player, and any future landing surface.
- Full-screen EPG
  - `confirmed`: was placeholder-heavy and only now-playing based.
  - `confirmed`: earlier work added actionable channel launch and program detail dialog plus short schedule loading.
  - `confirmed`: this pass adds category filtering, guide summary metadata, retry handling, stale/missing-data messaging, visible time-window controls, direct day browsing, half-hour and prime-time jumps, a shared aligned timeline grid with current-time line, time-scaled program placement, a `Scheduled only` mode that hides channels with no guide data, and guide modes for anchor-time and archive-ready browsing.
  - `confirmed`: the full guide now launches archive-capable programs directly into catch-up playback instead of stopping at a read-only detail dialog.
  - `confirmed`: the guide now exposes layout-density controls for compact, comfortable, and larger-screen browsing.
  - `confirmed`: the guide now exposes full-window paging actions, so users can move across broader timeline blocks without chaining repeated small jumps.
  - `confirmed`: favorite live channels now get visible overlay treatment inside the guide grid, and program dialogs now explain archive availability more clearly by distinguishing archive-ready programs from provider-level catch-up support and fully unavailable replay.
  - `confirmed`: the guide side panel now also includes provider-source metadata and a provider troubleshooting card that explains replay template issues, missing stream ids, stale guide risk, and other provider-specific replay blockers more explicitly.
  - `confirmed`: full-guide launches now preserve exact category, anchor-time, and favorites-only context into Player and can navigate back through the same guide route instead of dropping users into a generic guide state.
  - `confirmed`: the program detail surface now exposes channel, provider, language, replay state, progress, stale-guide refresh, and nearby schedule context through a dedicated TV-style side panel instead of a thin alert dialog.
  - Still incomplete: deeper archive reasoning is materially better now, but it is still bounded by provider metadata quality rather than guide-surface mechanics alone.
- Movies and Series browsing
  - `confirmed`: rows, hero content, category rails, continue watching, and dialogs exist.
  - `confirmed`: typed search path previously relied on extra UI-side filtering over full collections.
  - `confirmed`: this pass moves typed query mode onto repository-backed search flows, shifts selected-category loading onto category-scoped repository queries, and changes all-library rows into preview shelves with accurate category counts instead of treating every category row as a full in-memory list.
  - `confirmed`: Movies and Series now expose real `See All` actions per row, and selected-category views use dedicated full-list loads while all-category mode stays preview-oriented.
  - `confirmed`: selected Movies and Series categories now expose visible filter and sort chips for favorites, resume/in-progress, unwatched or updated content, top-rated content, and ordering by library, title, freshness, or rating, but the UI now suppresses that control stack for very small non-saved categories where it was crowding out the actual titles.
  - `confirmed`: selected Movies and Series categories now load in bounded pages with explicit `Load more` affordances and visible loaded-count feedback, so large provider categories no longer dump their full contents into the UI at once.
  - `confirmed`: all-library mode now adds repository-backed lens rows for Favorites, Continue Watching, Top Rated, and Fresh content instead of relying only on broad category maps.
  - `confirmed`: Movies and Series now expose a query-backed `Browse Full Library` mode from the all-library surface, so full-provider paging is no longer limited to category-specific views.
  - Still incomplete: there is still no full-library section virtualization, no provider-scoped advanced sort/filter surface, and no repository-backed multi-facet retrieval model beyond the current lens layer, full-library browse mode, and selected-category view.
- Search
  - `confirmed`: search results work and now launch player with correct context.
  - `confirmed`: Search now keeps recent queries in reachable UI so repeat searches are faster on TV remotes.
  - `confirmed`: Search now avoids blank result panes by surfacing explicit no-provider and minimum-query states, and the results side now exposes one-click content-type narrowing with visible counts.
  - `confirmed`: Search now restores focus back to the main query field after PIN interruptions, which makes repeated TV search sessions less awkward.
  - `confirmed`: Search now renders those empty and readiness states through the same elevated empty-state treatment instead of a plainer one-off layout, which improves consistency with the rest of the premium TV direction.
  - Still incomplete: no top-level filter/sort presets, no provider scoping toggle, and no deeper power-user shortcuts beyond text entry, recent queries, and content-type narrowing.
- Catch-up / archive
  - `confirmed`: program archive and restart actions exist.
  - `confirmed`: the full guide now exposes a direct catch-up handoff into Player for archive-capable programs instead of only offering live-channel launch.
  - `confirmed`: Player now surfaces explicit typed in-player recovery notices when catch-up context is missing or a provider does not return a replay stream, instead of failing silently.
  - `confirmed`: the player diagnostics overlay now also carries provider/source, stream-class, playback state, archive support status, alternate-stream count, channel error history, last failure reason, recent recovery actions, and troubleshooting hints, so archive failures are not just raw error toasts anymore.
  - `confirmed`: catch-up failure messages now distinguish between missing archive support, missing replay template, missing replay stream id, and provider replay rejection more clearly.
  - `confirmed`: the live playback error surface now exposes a direct `Last Channel` recovery action alongside retry when a previous live channel exists, which is a more TV-native recovery path than forcing users to back out manually.
  - `likely`: real-world provider reliability will still vary because catch-up templates/providers are messy and provider-specific degradation handling is still limited.
  - Production-grade finish: provider-specific replay failure reasons and richer archive metadata.
- Home dashboard
  - `confirmed`: Home is no longer just a hero plus static rows; it now exposes provider-health context directly and reorders its shelves around the current feature/action focus.
  - `confirmed`: this makes the landing surface feel more responsive to actual user state instead of forcing one fixed rail order all the time.
  - `confirmed`: Home now also surfaces active-provider warnings with a direct Settings drill-down instead of burying those issues only inside Settings.
  - Still incomplete: no editorial curation and no user-configurable layout.
- Multiview
  - `confirmed`: working slot playback exists.
  - `confirmed`: the multiview screen now has focused-slot replace/remove actions plus audio pinning so users can keep one tile audible while moving focus elsewhere.
  - `confirmed`: multiview now auto-detects a device performance tier, exposes a visible performance-mode selector, caps active simultaneous slots by policy, and holds extra slots in standby instead of trying to decode everything at once on weaker hardware.
  - `confirmed`: multiview now samples live slot telemetry, exposes a visible runtime profiling summary, tracks dropped-frame/buffering/error pressure, and dynamically reduces active decoders when sustained load, memory pressure, or thermal status demand it.
- Provider diagnostics
  - `confirmed`: Home already exposed warning and health surfacing, and Settings already had warning-driven retry actions.
  - `confirmed`: this pass turns the provider card into a real diagnostics entry point with sync-domain timestamps, item counts, last sync status, provider source/expiry/connection/archive summaries, and provider capability notes.
  - Still incomplete: no dedicated diagnostics route and no historical sync logs.
- Backup / restore
  - `confirmed`: backup now carries expanded preferences, provider definitions, playback history, promoted groups, and multiview presets.
  - `confirmed`: Settings now inspects a backup before import, shows section counts/conflicts, lets users choose keep-versus-replace behavior, and supports selective section import.
  - Still incomplete: import reporting is still summary-level, not a detailed conflict-by-conflict report, and broader media/library validation is limited.
  - Production-grade finish: add integrity reporting, richer skipped-item diagnostics, and clearer partial-restore messaging.

## D. Premium Gap Analysis
- What premium IPTV apps do better
  - `confirmed`: they preserve playback context consistently across every launch path. StreamVault had gaps in Search and Favorites until this pass.
  - `confirmed`: they make the guide actionable and central to Live TV, not a secondary placeholder screen.
  - `likely`: they use stronger VOD/Series information architecture for huge libraries, often with faster facet/filter systems than StreamVault currently exposes.
  - `likely`: they surface recent channels, better favorites/group workflows, and clearer provider health/troubleshooting.
- What this app still lacks
  - External player support.
  - Deeper full-guide personalization and overlays on top of the current timeline/return-context model.
  - Unified multi-provider or merged-library workflows.
  - A fuller recordings browser and more robust DVR scheduling lifecycle.
- Highest impact remaining changes
  1. Deepen Favorites into a stronger saved-library hub with better management presets and saved views.
  2. Extend recent channels into a stronger live-memory model anchored in Home and Player, not jammed into the Live TV browse header.
  3. Upgrade full-screen EPG from the current strong timeline surface into a more personalized and overlay-rich guide.
  4. Push Movies/Series from the current lens layer and selected-category paging into deeper repository-backed multi-facet retrieval and large-library virtualization.

## E. UI / UX Findings
- TV navigation issues
  - `confirmed`: Search and Favorites player launches were missing context, which broke zap/EPG behavior from those entry points.
  - `confirmed`: full-screen EPG row/program clicks were placeholders.
- Focus issues
  - `confirmed`: many screens already manage focus well; Home and Player are stronger than average.
  - `likely`: Search, Favorites, Settings, and Series detail still need a consistency pass to reach the same standard.
- Discoverability issues
  - `confirmed`: custom groups, replay, diagnostics, and some long-press actions are more discoverable to expert users than first-time TV users.
- Visual hierarchy issues
  - `confirmed`: Home/Player/Movies feel more premium than EPG/Favorites/Series detail.
  - `confirmed`: some screens still use hardcoded or generic copy instead of a shared premium tone, though Settings is noticeably stronger after the overview/section polish pass.
- Workflow friction
  - `confirmed`: recent-live browsing now lives in the Live TV category rail plus Player overlays, and Home now adds category-aware live shortcuts and a recent-channels row without polluting the Live TV browse screen.
  - `confirmed`: favorites/group workflows are improved in the category rails users actually use, Movies/Series still expose saved shortcuts directly in the main browse surface with long-press management and inline saved-group context cards, Home now exposes a direct saved-library entry point, and the Saved Library route now also covers Continue Watching and Recent Live; Live TV was intentionally simplified after review so saved groups do not crowd the main browse area there.
  - `confirmed`: the guide now behaves like a real aligned schedule surface with shared time markers, denser jump controls, and properly scaled program placement across rows, which is a major quality jump over the earlier pseudo-timeline list.
  - `confirmed`: the guide now offers visible density/layout choices and full-window paging, so users can trade channel density against readability and move across broader time blocks without repetitive step-jumping.
  - `confirmed`: Movies and Series now expose a visible Library Lens row in all-library mode, so Favorites, Continue Watching, Top Rated, and Fresh content are reachable without drilling into raw categories first.
  - `confirmed`: Search no longer drops into a blank pane when prerequisites are missing, and result narrowing is now possible from the results side instead of forcing users back to the left-side tab chips only.
  - `confirmed`: Settings now exposes a clearer top-level overview and section subtitles, so it reads more like a control center than a long undifferentiated list.
  - `confirmed`: Settings now also gives the no-provider case a proper empty state instead of a bare line of copy, and Player overlays now explain the channel-list and mini-guide actions more clearly from within playback.
- Premium polish issues
  - `confirmed`: loading/error/empty states are inconsistent.
  - `likely`: transitions and screen-to-screen rhythm still need one dedicated polish pass.

## F. Performance & Reliability Findings
- Bottlenecks
  - `confirmed`: Movies and Series were still doing redundant UI-side filtering for search.
  - `confirmed`: full-library grouping for VOD/Series was too eager; this pass shifts selected-category loads to category-scoped repository queries and changes all-category browsing into lighter preview shelves.
  - `confirmed`: selected provider-category views no longer need to load the entire category at once; they now use bounded page loads with explicit expansion.
  - `likely`: full-screen EPG should stay bounded; the current pass limits guide loading to a visible subset for safety while rendering that window as a denser aligned grid.
- Risks
  - `confirmed`: favorites/global routes had broken playback context before this pass.
  - `confirmed`: live channel identity inside Player was going stale after zaps before this pass; that is now fixed.
  - `likely`: current "all categories" VOD/Series presentations still scale worse than Live TV.
  - `confirmed`: the app now contains runtime multiview telemetry and thermal/load guards, but `likely` lower-end Android TV hardware still needs manual profiling sessions to validate sustained behavior under real decoder, thermal, and memory pressure.
- Optimization opportunities
  - Introduce richer image sizing/prefetch policies for posters/backdrops/logos.
  - Add recent-query and cached-result layers for search.
  - Consider a dedicated guide cache/view model for longer-range EPG timeline navigation.
- Scalability concerns
  - `confirmed`: Live is still architecturally ahead of VOD/Series in large-library readiness, but the gap is smaller now that Movies/Series use preview shelves, category-scoped exact loads, bounded page loads, and repository-backed all-library lens rows.
  - `likely`: 50k+ VOD/Series libraries will still need a deeper data-flow rewrite beyond this pass, especially for full-library paging, virtualization, and richer multi-facet retrieval across mixed provider/category surfaces.

## G. Missing Features Roadmap
### Core expected
- Merged-provider Saved Library behavior and final management polish
  - User value: high
  - UX impact: high
  - Complexity: medium
  - Effort: medium
- Deeper recent channels / live history workflows
  - User value: high
  - UX impact: high
  - Complexity: medium
  - Effort: medium
- Premium full-screen guide overlays and personalization
  - User value: high
  - UX impact: high
  - Complexity: medium-high
  - Effort: medium-large

### Premium
- Repository-backed full-library VOD/Series paging and deeper multi-facet retrieval
  - User value: high
  - UX impact: high
  - Complexity: high
  - Effort: medium-large
- Provider diagnostics and troubleshooting center
  - User value: medium
  - UX impact: medium
  - Complexity: medium
  - Effort: medium
- Backup/restore integrity reporting and detailed conflict diagnostics
  - User value: medium
  - UX impact: medium
  - Complexity: medium
  - Effort: medium

### Exceptional / differentiating
- Unified multi-provider merged library
  - User value: high
  - UX impact: high
  - Complexity: high
  - Effort: large
- Channel surfing memory model
  - Notes: recent channels, smart last-groups, mini-guide recall, provider-aware history
  - User value: high
  - Effort: medium
- Advanced multiview control layer
  - Notes: slot presets, audio pinning, quick replace/remove, and device-tier runtime resource profiles are now implemented; remaining gap is real-device tuning on weaker Android TV hardware.
  - User value: medium-high
  - Effort: medium-large
- DVR / recording
  - Notes: local live DVR v1 now exists; remaining gap is a recordings browser, more robust scheduling lifecycle, and broader protocol/provider support
  - User value: high
  - Effort: large

## H. Changes Implemented
- `confirmed`: created session branch `session/premium-audit-2026-03-11`.
- `confirmed`: tightened the internal player launch contract by adding route helpers for Live, Movie, and Episode playback.
- `confirmed`: fixed Search player navigation so it now preserves `providerId`, `categoryId`, `internalId`, and content type.
- `confirmed`: fixed Favorites player navigation so live/movie launches retain provider and playback context instead of using `-1` fallbacks, including grouped-live playback context.
- `confirmed`: enriched `FavoriteUiModel` with provider/category/EPG context and launch metadata needed for correct playback behavior.
- `confirmed`: made full-screen EPG actionable by wiring channel launch and program detail dialog instead of leaving TODO click handlers.
- `confirmed`: upgraded full-screen EPG data loading from "now playing only" to a bounded schedule window per visible channel.
- `confirmed`: added Live category filtering to the full-screen EPG instead of always loading the first global channel slice.
- `confirmed`: added guide summary/status UI for channel coverage, last refresh timing, and stale-data warnings.
- `confirmed`: upgraded EPG empty/error states with retry actions and clearer no-provider/no-category feedback.
- `confirmed`: added visible guide time controls for jumping backward, jumping forward, returning to now, and moving to tomorrow.
- `confirmed`: added direct day-navigation controls to the TV Guide so users can move across dates without chaining repeated time-window jumps.
- `confirmed`: added a `Scheduled only` guide mode so large or messy provider guides can hide empty channels instead of mixing no-data rows into the main schedule view.
- `confirmed`: added guide-mode chips for full timeline browsing, anchor-time browsing, and archive-ready rows so the guide can pivot between broad exploration and IPTV-specific catch-up hunting.
- `confirmed`: added guide density/layout chips so users can switch between compact, comfortable, and larger-row guide presentations from the TV Guide itself.
- `confirmed`: added full-window guide paging actions so users can move across larger guide blocks without repeatedly stacking half-hour and three-hour jumps.
- `confirmed`: added a clear guide-window label so the EPG shows which time range is currently loaded.
- `confirmed`: added a visible timeline-style ruler to the TV Guide so the loaded window reads more like a real guide surface instead of a plain list of program cards.
- `confirmed`: added live progress treatment to currently airing guide cards, including progress bars and remaining-time labels.
- `confirmed`: upgraded the full-screen guide into an aligned timeline grid with shared half-hour markers, a persistent current-time line, and time-scaled program placement across channel rows.
- `confirmed`: added denser guide time navigation with half-hour jumps and a direct `Prime Time` jump alongside the existing day/window controls.
- `confirmed`: added archive-ready program labeling inside the full guide so catch-up-capable entries are visually distinct without opening Player first.
- `confirmed`: persisted TV Guide density, mode, favorite-channel-only filtering, and anchor-time defaults so the guide reopens in the user’s last working state instead of resetting each session.
- `confirmed`: added a favorite-channel-only guide source filter so the full guide can collapse down to pinned live channels without changing categories.
- `confirmed`: extended the full-guide navigation contract so guide category, anchor time, and favorites-only state can be passed through player launches as an exact return route.
- `confirmed`: moved Movies and Series typed search mode onto repository-backed search flows instead of re-filtering the full in-memory list.
- `confirmed`: added repository-backed preview queries and count flows for Movies and Series so all-library browsing can stay preview-oriented while selected categories load their full contents on demand.
- `confirmed`: moved Movies and Series selected-category views onto dedicated category-scoped repository loading instead of relying on the all-library grouped map as the working dataset.
- `confirmed`: turned Movies and Series category rows into true preview shelves with actionable `See All` controls and accurate category/library counts.
- `confirmed`: added repository-backed page loading for selected provider categories in Movies and Series, plus visible `Loaded X of Y` feedback and `Load more` actions on the grid surfaces.
- `confirmed`: introduced explicit `LibraryBrowseQuery` and `PagedResult` contracts plus repository-backed browse methods for Movies and Series so category paging is no longer just a UI convention.
- `confirmed`: added a visible query-backed `Browse Full Library` mode to Movies and Series so users can page through the full provider catalog without drilling into categories first.
- `confirmed`: added recent-search shortcuts and clear-history controls to the Search screen so repeat searches are reachable without retyping.
- `confirmed`: added SearchViewModel coverage for recent-query history behavior.
- `confirmed`: upgraded Search empty-state behavior so it now shows explicit no-provider, ready-to-search, and no-results messaging instead of leaving the results pane visually blank.
- `confirmed`: added results-summary chips to Search so users can narrow to Live TV, Movies, or Series directly from the results side with visible counts.
- `confirmed`: added SearchViewModel coverage for provider/query readiness state under the debounced search flow.
- `confirmed`: upgraded Settings with a top-level overview card, section subtitles, and cleaner provider warning/status copy so the screen feels more deliberate and TV-native.
- `confirmed`: upgraded Settings provider cards into a real diagnostics surface with per-domain sync timestamps and counts, last sync status, capability summaries, and the existing retry actions in one place.
- `confirmed`: reduced a broad hero selection flattening path in Movies to a first-item-per-category scan.
- `confirmed`: added a shared virtual-category contract for Favorites and Recent channels.
- `confirmed`: rebuilt the Favorites screen into real saved-library sections: global favorites plus populated Live/Movie/Series custom groups.
- `confirmed`: added section-scoped reorder behavior in Favorites instead of global flat-list reorder only.
- `confirmed`: turned the standalone Saved Library route into a real hub with overview stats, type filters, and section-jump chips now that Home exposes it.
- `confirmed`: expanded the Saved Library hub with Continue Watching and Recent Live rows so the route now unifies saved items with playback recall workflows instead of only listing favorites/groups.
- `confirmed`: added Saved Library sort presets for manual order, recent activity, and alphabetical browsing without breaking the existing manual reorder flow.
- `confirmed`: tied Saved Library recent sorting to playback-history activity so the new sort presets reflect actual use instead of static metadata.
- `confirmed`: added higher-level Saved Library preset views for `All Saved`, `Watch Next`, `Live Recall`, Movies, Series, and Custom Groups so the hub now behaves more like a recall dashboard than a flat saved list.
- `confirmed`: added a managed custom-groups row to the Saved Library hub with direct long-press actions for merge, delete, reorder, and Home promotion, plus item-level remove and move-between-group actions without leaving the hub.
- `confirmed`: changed Saved Library group clicks to jump within the hub itself instead of bouncing users out to other top-level screens, keeping the route as the canonical cross-type management surface.
- `confirmed`: added provider scoping inside the Saved Library hub so users can switch between current-provider and all-provider saved/history views without leaving the screen.
- `confirmed`: added a dedicated `Home Shelf` saved preset so promoted live groups and dashboard-eligible live favorites can be reviewed and managed intentionally.
- `confirmed`: added recent-live history recording and surfaced it as a Live TV virtual category with proper playlist/zap context.
- `confirmed`: made the Live TV `Recent` category permanently visible in the sidebar, even when empty, so the feature is discoverable from reachable UI.
- `confirmed`: added provider-scoped `Last Group` memory for Live TV and Player, then intentionally removed the extra Live TV header shortcuts after review so the browse screen is no longer acting like a cluttered pseudo-home.
- `confirmed`: surfaced recent live channels inside Player via a visible quick action and a recent row in the channel list overlay.
- `confirmed`: surfaced the remembered live `Last Group` inside Player through both the live info overlay and the channel-list header, so browsing memory carries into playback UI too.
- `confirmed`: updated Player state on channel changes so live playback context, title, and URL do not go stale after zapping.
- `confirmed`: fixed the Player channel-list selection highlight so it tracks the currently tuned channel instead of the original route channel ID after zapping.
- `confirmed`: added a non-resume playback history write path for live sessions while keeping VOD resume writes separate.
- `confirmed`: filtered Movies and Series continue-watching rows so newly recorded live history does not pollute VOD surfaces.
- `confirmed`: added custom-group rename/manage actions directly to Movies and Series category sidebars instead of treating the standalone Favorites route as the primary workflow.
- `confirmed`: added the same custom-group rename flow to the reachable Live TV category rail for parity with Movies and Series.
- `confirmed`: fixed custom-group rename persistence so Movies, Series, and Live TV now map virtual category IDs back to the real stored group IDs before renaming.
- `confirmed`: kept the built-in `Favorites` bucket fixed/reserved and limited rename management to true custom groups.
- `confirmed`: added visible snackbar feedback for Movies and Series group create/rename/delete actions.
- `confirmed`: replaced more hardcoded Movies/Series category sidebar copy with string resources.
- `confirmed`: removed a broad `flatten()` hero-pick path in Series in favor of the same first-item-per-category scan used in Movies.
- `confirmed`: added reachable saved-shortcuts strips to the Movies and Series main browse surfaces so Favorites and populated custom groups are one-click targets instead of being buried only in the sidebar.
- `confirmed`: added empty-state guidance to those saved-shortcut strips so users can tell how to populate saved items before the shortcuts appear.
- `confirmed`: fixed Movie/Series saved shortcut construction so `Favorites` is no longer duplicated, empty groups are filtered out, and an explicit `Browse All` shortcut is available from the same strip.
- `confirmed`: kept the saved shortcut strips visible in saved-category Movies/Series views so switching between Favorites, custom groups, and the full library no longer depends on going back to the sidebar, while normal provider categories stay visually cleaner.
- `confirmed`: added direct long-press management from saved shortcut cards in Movies and Series, plus an on-screen hint so saved groups are not browse-only shortcuts anymore.
- `confirmed`: added inline saved-group context cards to selected Movies and Series saved views with direct `Manage` and `Browse All` actions plus visible item counts.
- `confirmed`: added category-scoped Movies and Series facet/sort chips so users can narrow selected categories by favorites, resume state, unwatched or updated content, and rating without forcing a full-library load.
- `confirmed`: trimmed the selected-category VOD/Series chrome so normal provider categories no longer show the full Saved block, and small categories now skip the bulky facet/sort stack that was pushing the actual titles off-screen.
- `confirmed`: added repository-backed all-library Library Lens rows to Movies and Series for Favorites, Continue Watching, Top Rated, and Fresh content so premium discovery is no longer limited to raw category rails.
- `confirmed`: created a real dashboard-style Home screen and split Live TV into its own dedicated top-level route instead of using the Live browser as the landing page.
- `confirmed`: expanded Home into a richer landing surface with featured hero state, library stats, live shortcut cards, recent live channels, and category-aware deep links back into Live TV groups.
- `confirmed`: added provider-health surfacing to Home so the dashboard now exposes provider status, source type, sync recency, expiry context, and connection count without sending users into Settings first.
- `confirmed`: made Home shelf ordering more adaptive by biasing the row order toward the current hero/feature context instead of leaving the dashboard static.
- `confirmed`: added provider-warning surfacing on Home with a direct Settings drill-down so partial sync issues are visible from the landing surface.
- `confirmed`: reinforced Home-to-diagnostics flow by making the provider-health card itself expose the diagnostics drill-down action instead of relying only on warning states.
- `confirmed`: extended the player route contract with optional archive-window parameters so the full TV Guide can launch directly into catch-up playback.
- `confirmed`: added a full-guide `Play Catch-up` action for archive-capable programs instead of limiting program dialogs to live-channel launch only.
- `confirmed`: added an in-player archive failure notice so missing catch-up URLs or invalid replay context are surfaced to the user instead of failing silently.
- `confirmed`: added favorite-channel badging directly inside the guide grid and upgraded the program dialog with provider-level archive hints plus stale-guide replay caveats so archive-heavy browsing gives clearer context before playback starts.
- `confirmed`: added a direct saved-library entry point from Home and promoted the standalone Favorites route into an actual reachable product surface instead of leaving it effectively hidden.
- `confirmed`: simplified the Live TV browse surface by removing the added saved/recent/last-group shortcut strip from the content area.
- `confirmed`: added category-aware `Live TV` route deep links so Home shortcuts can open Favorites, Recent, Last Group, and custom live groups directly instead of always landing on the generic Live view.
- `confirmed`: fixed focus restoration after closing category/group option dialogs so the previously focused group stays active in Live TV, Movies, and Series.
- `confirmed`: fixed category reorder targeting so Live TV, Movies, and Series now reorder the long-pressed saved category instead of the currently open category or a partial preview list.
- `confirmed`: replaced the inline custom-group creation mode inside the add-to-group dialog with a dedicated create-group dialog to reduce keyboard/back-flow conflicts.
- `confirmed`: removed nested-dialog stacking during group creation so the Add-to-Group sheet now yields to dedicated create/split dialogs instead of keeping multiple active dialog layers alive under the TV keyboard.
- `confirmed`: tightened Create Group dialog behavior by trimming names, disabling empty confirmation, and hiding the software keyboard on confirm/dismiss.
- `confirmed`: tightened PIN dialog behavior by showing the numeric keyboard on entry and hiding it on submit/dismiss so TV modal transitions leave less keyboard residue behind.
- `confirmed`: restored Search focus to the main query field on entry and after PIN dismissal so repeated search/refine flows behave more like a TV-native search surface.
- `confirmed`: upgraded the player error surface with an explicit `Last Channel` recovery action for live playback instead of leaving recovery at `Retry` plus a passive back hint only.
- `confirmed`: replaced plain player notice strings with typed recovery notices that can expose `Retry`, `Alternate Stream`, `Last Channel`, and `Open Guide` actions directly from the playback surface.
- `confirmed`: added session-level alternate-stream failure memory so the player deprioritizes already-failed alternates during the same viewing session before surfacing fallback actions.
- `confirmed`: expanded the in-player diagnostics overlay with provider/source, decoder mode, stream class, playback state, archive support, alternate-stream count, channel error history, last failure reason, recovery actions, and troubleshooting hints so the player now shows actual troubleshooting context instead of only raw codec/bitrate stats.
- `confirmed`: added exact guide-return behavior from player notices so `Open Guide` can navigate back to the originating full-guide route instead of only opening a generic overlay.
- `confirmed`: replaced the old guide alert dialog with a TV-style program side panel that adds provider/channel/language/replay/progress metadata, nearby schedule context, and a direct stale-guide refresh action.
- `confirmed`: updated Player back behavior so transient player notices dismiss before playback exits, which makes the overlay stack feel less abrupt on the remote.
- `confirmed`: added clearer inline guidance to the Player channel list and mini-guide overlays, and upgraded the Settings no-provider surface to the same elevated empty-state treatment used elsewhere.
- `confirmed`: added multiview presets backed by preferences so common split-screen slot layouts can be saved and restored.
- `confirmed`: added focused-slot multiview controls for replace, remove, and audio pinning, so split-screen management no longer depends only on the planner dialog or focus-follow audio.
- `confirmed`: added a device-tier multiview performance policy with persisted Auto / Conservative / Balanced / Maximum modes, effective active-slot caps, staged decoder startup, and standby messaging for slots held back on weaker devices.
- `confirmed`: added telemetry-backed multiview protection with dropped-frame/buffering/error sampling, runtime profiling summaries, low-memory standby handling, and thermal-aware slot throttling on supported Android TV versions.
- `confirmed`: expanded Settings provider diagnostics with source, expiry, connection, and archive summaries instead of only capability text plus sync counts.
- `confirmed`: expanded the guide side panel with provider-specific replay troubleshooting so catch-up blockers like missing templates, missing replay stream ids, and stale guide risk are explained in-place.
- `confirmed`: upgraded catch-up failure reasoning in Player so replay errors describe the likely provider-side blocker instead of collapsing into one generic replay failure message.
- `confirmed`: expanded backup/restore into version 2 data bundles that now include providers, playback history, promoted live groups, guide preferences, and multiview presets.
- `confirmed`: upgraded Settings backup import into a real preview/conflict flow with section counts, conflict counts, keep-vs-replace strategy, and per-section import toggles before applying a restore.
- `confirmed`: wired the existing recording manager into DI and shipped local live DVR v1 with manual recording, scheduled recording, storage state, persisted recording jobs, and stop/cancel actions.
- `confirmed`: added reachable DVR UI in Player for `Record`, `Stop Recording`, and `Schedule`, and added a Settings recording section that surfaces storage health, active jobs, scheduled jobs, and failure reasons.
- `confirmed`: added navigation contract tests for Live/Movie/Episode routes.
- `confirmed`: added route tests covering full-guide route context and player return-route preservation.
- `confirmed`: added HomeViewModel coverage for the new recent-live virtual category behavior, including empty-history visibility.
- `confirmed`: `./gradlew test` passes after the changes.

## I. Recommended Next Roadmap
1. Quick wins
   - Refine the new Home dashboard with deeper personalization on top of the new hero/health/warning/shortcut structure.
   - Tighten Saved Library policy controls beyond provider scoping and Home promotion.
2. Medium effort / high value
   - Move Movies/Series from the current browse-query foundation and selected-category paging into repository-backed multi-facet retrieval and deeper full-library paging.
   - Deeper Settings reorganization around Provider, Playback, Library, and Privacy beyond the new overview/section polish.
   - Stronger Search UX with saved queries and result shortcuts.
   - Expand the typed player recovery model into a fuller diagnostics and recovery panel with deeper provider-account troubleshooting context.
3. Major premium upgrades
   - DVR recordings browser plus stronger scheduling lifecycle beyond in-app state.
   - Backup/restore integrity reporting and conflict-by-conflict diagnostics.
   - Advanced archive overlays and personalization on top of the new aligned timeline grid.
4. Standout differentiators
   - Manual multiview profiling and tuning on real lower-end Android TV hardware now that the in-app telemetry/protection layer exists.
   - Unified multi-provider browsing.
   - Live-TV surfer mode with recent-channel intelligence and ultra-fast recall.
