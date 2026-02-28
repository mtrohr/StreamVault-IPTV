package com.streamvault.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand Primary — Deep Indigo-Violet ────────────────────────────
val Primary          = Color(0xFF6C63FF)
val PrimaryLight     = Color(0xFF9D97FF)   // hover/pressed states
val PrimaryVariant   = PrimaryLight        // legacy alias
val PrimaryDark      = Color(0xFF4A42DB)
val PrimaryGlow      = Color(0x336C63FF)   // 20% opacity glow ring

// ── TV-Optimized Backgrounds — Cinematic Depth ──────────────────
val BackgroundDeep   = Color(0xFF09090E)   // Ultra Deep — behind everything
val Background       = Color(0xFF0D0D12)   // Legacy alias fallback
val Surface          = Color(0xFF14141E)   // Standard Card surfaces
val SurfaceElevated  = Color(0xFF1E1E2D)   // Modals, overlays, sidebars
val SurfaceHighlight = Color(0xFF2A2A3D)   // Soft selection indication
val SurfaceVariant   = SurfaceElevated

// ── TV-Optimized Text — Massive Readability ───────────────────────
val TextPrimary      = Color(0xFFFFFFFF)   // Absolute White for core data
val TextSecondary    = Color(0xFFA0A0B0)   // Sub-titles / descriptions
val TextTertiary     = Color(0xFF656575)   // Low priority metadata
val TextDisabled     = Color(0xFF4A4A58)   // Disabled states

// Legacy aliases kept for compatibility
val OnBackground     = TextPrimary
val OnSurface        = TextSecondary
val OnSurfaceVariant = TextSecondary
val OnSurfaceDim     = TextTertiary

// ── Accents ────────────────────────────────────────────────────────
val AccentRed        = Color(0xFFFF4B6A)   // Live indicator, errors
val AccentGreen      = Color(0xFF2DD881)   // Success, online
val AccentAmber      = Color(0xFFFFA742)   // Warnings, badges (Reorder drag)
val AccentCyan       = Color(0xFF00D4FF)   // EPG progress, info

// Legacy aliases
val OnPrimary        = Color(0xFFFFFFFF)
val Secondary        = Color(0xFF03DAC6)
val ErrorColor       = AccentRed
val SuccessColor     = AccentGreen
val LiveIndicator    = AccentRed
val WarningColor     = AccentAmber

// ── Cinematic Gradient Overlays ───────────────────────────────────
val GradientOverlayTop    = Color(0x99050508)   // 60% opacity for cleaner fade
val GradientOverlayBottom = Color(0xE6050508)   // 90% opacity for solid dark-read

// ── TV Focus System ───────────────────────────────────────────────
val FocusBorder             = Color(0xFFF0F0F5) // HIGH CONTRAST WHITE for 10-foot legibility
val CardBackground          = Surface
val ProgressBar             = Primary
val ProgressBarBackground   = Color(0xFF1E1E2D)
