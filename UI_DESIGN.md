# 📺 StreamVault — UI Design Specification

## Design Philosophy

> **Console game menu aesthetic**: Fast, predictable, visually clean, boring under the hood.

The UI prioritizes **smoothness over visual complexity**. Premium feel comes from spacing, typography, focus behavior, and consistency — never from heavy effects.

---

## 🎨 Design System

### Color Palette (Dark Theme Only)

```
Background:           #0D0D0D (near black)
Surface:              #1A1A1A (cards, panels)
Surface Elevated:     #242424 (dialogs, overlays)
Primary:              #6C63FF (accent, focused borders)
Primary Variant:      #8B83FF (hover states)
On Background:        #E8E8E8 (primary text)
On Surface:           #B0B0B0 (secondary text)
On Surface Dim:       #666666 (tertiary text)
Error:                #FF6B6B
Success:              #4ECB71
Live Indicator:       #FF4444 (pulsing dot)
```

### Typography (Google Font: Inter)

| Role         | Size  | Weight   | Usage                      |
|--------------|-------|----------|----------------------------|
| Display      | 32sp  | Bold     | Screen titles              |
| Headline     | 24sp  | SemiBold | Section headers            |
| Title        | 20sp  | Medium   | Card titles (focused)      |
| Body         | 16sp  | Regular  | Descriptions, metadata     |
| Label        | 14sp  | Medium   | Badges, timestamps         |
| Caption      | 12sp  | Regular  | Subtle info                |

### Spacing Scale

```
XS:  4dp     (icon padding)
S:   8dp     (card internal padding)
M:   16dp    (between cards)
L:   24dp    (section gaps)
XL:  32dp    (screen margins)
XXL: 48dp    (major section breaks)
```

---

## 📺 Visual Hierarchy

### Screen Layout Structure

```
┌─────────────────────────────────────────────────┐
│  [Logo]     Live TV │ Movies │ Series │ ★ │ ⚙   │  ← Top Nav Bar (56dp)
├─────────────────────────────────────────────────┤
│                                                 │
│  Category Title                                 │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐      │  ← Horizontal content rows
│  │     │ │     │ │ FOC │ │     │ │     │      │
│  │     │ │     │ │ USED│ │     │ │     │      │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘      │
│                                                 │
│  Another Category                               │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐      │
│  │     │ │     │ │     │ │     │ │     │      │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘      │
│                                                 │
└─────────────────────────────────────────────────┘
```

### Content Flow
1. Top navigation bar — always visible, horizontal tabs
2. Content area — vertical scroll of category rows
3. Each category row — horizontal lazy list of cards
4. Focus flows: Left/Right within row, Up/Down between rows

---

## 🖼 Card Specifications

### Channel Card (Live TV)
- **Size**: 200dp × 120dp (16:10 landscape)
- **Content**: Logo (centered, 64dp max), channel name below
- **Live indicator**: Small red dot + "LIVE" label
- **EPG overlay**: Current program name at bottom (truncated)

### Movie Card (VOD)
- **Size**: 160dp × 240dp (2:3 portrait poster)
- **Content**: Poster image, title below
- **Metadata**: Year, rating badge

### Series Card
- **Size**: 160dp × 240dp (2:3 portrait poster)
- **Content**: Poster image, title, season count badge

### Episode Card
- **Size**: 280dp × 160dp (16:9 landscape)
- **Content**: Thumbnail, episode title, duration
- **Progress**: Thin progress bar at bottom (if partially watched)

---

## 🔍 Focus Behavior (Critical)

### Focus Appearance

| State     | Scale  | Border               | Text Opacity | Animation |
|-----------|--------|----------------------|--------------|-----------|
| Unfocused | 1.0    | None                 | 70%          | —         |
| Focused   | 1.06   | 2dp solid #6C63FF   | 100%         | 180ms     |
| Pressed   | 0.98   | 2dp solid #8B83FF   | 100%         | 80ms      |

### Focus Rules
- Focus transitions use `tween(180ms, LinearEasing)` — no spring/bounce
- Only **ONE** animation property per interaction (scale only)
- Border is **static**, not animated
- Unfocus is **instant** (no delayed return animation)
- Focus state never triggers layout reflow
- Focus ring is drawn via `Modifier.border()`, not via shadow/glow

### Navigation Rules
- D-pad Left/Right: Move within current row
- D-pad Up/Down: Move between rows (focus restores last-focused index)
- Back: Navigate up one level (row → nav bar → exit prompt)
- Select/Enter: Open item
- Long press: Add to favorites (if applicable)

---

## 🎬 Animation Specifications

### Allowed Animations
| Property | Duration | Easing       | Use Case              |
|----------|----------|--------------|------------------------|
| Scale    | 180ms    | Linear       | Card focus             |
| Alpha    | 150ms    | Linear       | Text/overlay appear    |
| Color    | 150ms    | Linear       | Button state change    |

### Forbidden Animations
- ❌ Blur / glassmorphism
- ❌ Physics-based (spring, fling)
- ❌ Layout size changes
- ❌ Position animations
- ❌ Shadow depth changes
- ❌ Crossfade on images
- ❌ Multiple chained animations
- ❌ Particle effects

---

## 📱 Screen Designs

### 1. Live TV Screen
```
┌──────────────────────────────────────────┐
│ Nav: [●Live TV] Movies  Series  ★  ⚙    │
├──────────────────────────────────────────┤
│ All Channels      ▸ Search               │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐     │
│ │ CH │ │ CH │ │ CH │ │ CH │ │ CH │     │
│ │logo│ │logo│ │logo│ │logo│ │logo│     │
│ └────┘ └────┘ └────┘ └────┘ └────┘     │
│ Entertainment                            │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐     │
│ │    │ │    │ │    │ │    │ │    │     │
│ └────┘ └────┘ └────┘ └────┘ └────┘     │
│ Sports                                   │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐     │
│ └────┘ └────┘ └────┘ └────┘ └────┘     │
└──────────────────────────────────────────┘
```

### 2. Movies Screen
- Vertical scroll of category rows
- Each row is a lazy horizontal list of poster cards
- Categories fetched from provider

### 3. Series Screen  
- Same layout as Movies
- Selecting a series → Season list → Episode list (drill-down)

### 4. Player Screen
```
┌──────────────────────────────────────────┐
│                                          │
│            [Video Surface]               │
│                                          │
│──────────────────────────────────────────│
│ Overlay (auto-hide after 5s):            │
│ ┌──────────────────────────────────────┐ │
│ │ Channel Name / Movie Title           │ │
│ │ Now Playing: Program Name            │ │
│ │ ◄◄  ▶/❚❚  ►►   🔊 Vol   ⚙ Decoder  │ │
│ │ ━━━━━━━━━━━●━━━━━━━━━━  12:30/45:00 │ │
│ └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```
- Overlay appears on any D-pad input, hides after 5s idle
- Progress bar only for VOD, not live
- Decoder mode toggle accessible from overlay

### 5. EPG Timeline
```
┌──────────────────────────────────────────┐
│ EPG Guide                 ◄ Feb 10 2026 ►│
├──────┬───────┬───────┬───────┬──────────┤
│      │ 17:00 │ 17:30 │ 18:00 │ 18:30    │
├──────┼───────┴───────┼───────┼──────────┤
│ BBC  │  News Hour    │ Sport │ Drama    │
├──────┼───────┬───────┴───────┼──────────┤
│ CNN  │ Live  │ Documentary   │ Talk     │
├──────┼───────┼───────────────┼──────────┤
│ ESPN │ NBA Live              │ Replay   │
└──────┴───────────────────────┴──────────┘
```
- Horizontal scroll for time, vertical scroll for channels
- Current time indicator (vertical red line)
- Focus on program cell → show details overlay

### 6. Favorites Screen
- Grid layout of favorited items (mixed: channels, movies, series)
- Drag & drop reorder (hold select to enter reorder mode)
- Virtual group tabs at top
- "Create Group" button

### 7. Settings Screen
- Vertical list of setting groups
- Provider management (add/remove/edit)
- Decoder mode selection
- EPG refresh interval
- Image cache management
- About / version info

---

## ⚡ Performance Considerations

### Image Strategy
- **Coil** with disk + memory cache
- Request images at exact card dimensions (no `wrap_content`)
- No crossfade animations on image load
- Placeholder: solid `#1A1A1A` rectangle
- Error: channel name text fallback

### Lazy List Optimization
- `LazyRow` / `LazyVerticalGrid` for all content lists
- Items keyed by stable ID
- No unnecessary recomposition — use `remember` and `derivedStateOf`
- Pre-fetch beyond visible items (`beyondBoundsItemCount`)

### Recomposition Discipline
- UI state is immutable data classes
- ViewModels expose `StateFlow<UiState>`
- No `MutableState` at screen level
- Avoid collecting high-frequency flows (playback position) in Composables

### Frame Budget
- Target: ≤16ms per frame
- No GC pressure from UI allocations
- No blocking I/O on main thread
- Compose stability annotations where needed

---

## 🎯 Design Summary

The visual identity is **dark, clean, and fast**. Every design decision serves the goal of instant-feeling D-pad navigation. The palette is muted with a single accent color. Cards are simple rectangles with posters. Focus is communicated through a consistent scale + border pattern. The entire UI could run on a $30 Android TV stick without frame drops.
