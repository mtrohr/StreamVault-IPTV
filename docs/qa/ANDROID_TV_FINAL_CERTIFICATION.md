# Android TV Final Certification Checklist

Date: 2026-03-12
Branch: `ui-change`
Build under test: `appDebug`

## Severity
- `ship blocker`: must be fixed before release
- `polish defect`: should be fixed before release unless explicitly waived
- `minor follow-up`: acceptable for post-release follow-up

## Environment
- Device:
- Android TV version:
- Input device:
- Build fingerprint:
- Tester:

## Global Checks
- [ ] App launches without layout jumps or placeholder flashes.
- [ ] Initial focus is visible on every major route.
- [ ] `Back` behavior is deterministic and never traps focus.
- [ ] Focus is restored after dialogs and sheets close.
- [ ] Focus ring, text size, and contrast are readable from TV distance.
- [ ] No clipped titles, buttons, or badges on 1080p and 4K outputs.
- [ ] RTL layout mirrors rails, overlays, and aligned metadata correctly.
- [ ] Search fields behave correctly with remote and soft keyboard input.

## Welcome And Onboarding
- [ ] Welcome screen first focus lands on the primary onboarding action.
- [ ] Provider setup is fully navigable with remote only.
- [ ] Xtream and playlist flows preserve validation and error messaging.
- [ ] Long forms do not lose focus when validation errors appear.

## Dashboard
- [ ] Hero, shortcuts, continue-watching, and provider health are all reachable by D-pad.
- [ ] Shelf-to-shelf traversal does not skip or trap focus.
- [ ] Provider health messaging remains readable at TV distance.

## Live TV
- [ ] Category rail initial focus is correct.
- [ ] Rail-to-channel-pane crossing is stable in both directions.
- [ ] Large channel lists remain smooth while scrolling.
- [ ] Channel search and category search both preserve focus correctly.
- [ ] Reorder mode is remote-friendly and exits cleanly with `Back`.
- [ ] Group rename/delete flows restore focus to the triggering item.
- [ ] Multiview entry works from live browse and returns cleanly.
- [ ] Locked channels clearly communicate protected state.

## Movies And Series
- [ ] Landing state is navigable without hidden focus jumps.
- [ ] Category selection enters the paged grid correctly.
- [ ] `Back` exits selected-category browse before leaving the route.
- [ ] Load-more behavior remains correct and focusable.
- [ ] Long-press actions still work on posters and shortcuts.
- [ ] Locked titles remain readable and visually distinct.

## Series Detail
- [ ] Hero CTA, season rail, and episodes are all reachable by remote only.
- [ ] Season changes keep focus in a predictable place.
- [ ] Episode rows preserve hierarchy and readable metadata.

## Saved
- [ ] Preset chips, provider scope, and grouped shelves are all reachable.
- [ ] Manage or reorder mode is visually distinct from browse mode.
- [ ] Continue-watching and live recall sections keep consistent focus styling.

## Guide
- [ ] Filter strip, timeline grid, and details panel all have deterministic focus.
- [ ] Focused program details always match the selected program cell.
- [ ] Archive-ready states are clearly communicated.
- [ ] Timeline scrolling remains performant on dense schedules.

## Settings And Parental Controls
- [ ] Provider cards, diagnostics, and management actions are all reachable.
- [ ] Backup and restore actions are understandable without pointer input.
- [ ] Parental group management keeps correct focus after PIN or dialog dismissal.

## Player
- [ ] Controls open with the intended primary focus target.
- [ ] Transport, quick actions, and side overlays feel like one system.
- [ ] Channel list overlay restores focus when reopened.
- [ ] Guide and info overlays close in the correct `Back` order.
- [ ] Track selection dialog and diagnostics sheet are fully remote-operable.
- [ ] Resume prompt, numeric channel input, and error notices render cleanly.
- [ ] Live playback, VOD playback, and archive playback all preserve the same control language.

## Large Library Validation
- [ ] Live browse remains responsive with representative 50k-100k channel data.
- [ ] Movie browse remains on the paged library path under large datasets.
- [ ] Series browse remains on the paged library path under large datasets.
- [ ] No route shows whole-list loading or focus lag under large datasets.

## Findings
| Area | Severity | Result | Notes |
| --- | --- | --- | --- |
| Welcome |  |  |  |
| Dashboard |  |  |  |
| Live TV |  |  |  |
| Movies |  |  |  |
| Series |  |  |  |
| Series Detail |  |  |  |
| Saved |  |  |  |
| Guide |  |  |  |
| Settings |  |  |  |
| Player |  |  |  |
| RTL |  |  |  |
| Accessibility |  |  |  |

## Sign-Off
- [ ] No unresolved ship blockers remain.
- [ ] All polish defects are fixed or explicitly waived.
- [ ] Final build/test bar has been re-run after the last fix.
