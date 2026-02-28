package com.streamvault.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val xs: Dp = 8.dp,
    val sm: Dp = 16.dp,
    val md: Dp = 24.dp,
    val lg: Dp = 32.dp,
    val xl: Dp = 48.dp,
    val xxl: Dp = 64.dp,
    
    // TV Overscan Safe Area
    val safeTop: Dp = 32.dp,
    val safeBottom: Dp = 32.dp,
    val safeHoriz: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
