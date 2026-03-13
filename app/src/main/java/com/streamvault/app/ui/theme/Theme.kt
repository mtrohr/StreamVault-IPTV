package com.streamvault.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.streamvault.app.ui.design.AppShapes
import com.streamvault.app.ui.design.LocalAppShapes
import com.streamvault.app.ui.design.LocalAppSpacing

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    surface = Surface,
    onSurface = TextSecondary,
    background = Background,
    onBackground = TextPrimary,
    error = ErrorColor,
    onError = OnPrimary
)

@Composable
fun StreamVaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpacing provides defaultSpacing(),
        LocalAppSpacing provides com.streamvault.app.ui.design.AppSpacing(),
        LocalAppShapes provides AppShapes()
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = StreamVaultTypography,
            content = content
        )
    }
}
