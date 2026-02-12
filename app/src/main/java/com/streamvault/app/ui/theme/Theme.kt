package com.streamvault.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    background = Background,
    onBackground = OnBackground,
    error = ErrorColor,
    onError = OnBackground
)

@Composable
fun StreamVaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}
