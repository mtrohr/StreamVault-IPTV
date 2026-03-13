package com.streamvault.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.streamvault.app.ui.design.AppSpacing

data class Spacing(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val safeTop: Dp,
    val safeBottom: Dp,
    val safeHoriz: Dp
)

val LocalSpacing = staticCompositionLocalOf { defaultSpacing() }

fun defaultSpacing(): Spacing {
    val spacing = AppSpacing()
    return Spacing(
        xs = spacing.xs,
        sm = spacing.md,
        md = spacing.lg,
        lg = spacing.xl,
        xl = spacing.screenGutter,
        xxl = spacing.screenGutter + spacing.md,
        safeTop = spacing.safeTop,
        safeBottom = spacing.safeBottom,
        safeHoriz = spacing.screenGutter
    )
}
