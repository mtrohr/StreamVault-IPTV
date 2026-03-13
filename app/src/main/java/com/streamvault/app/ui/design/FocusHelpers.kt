package com.streamvault.app.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

data class FocusMemoryKey(
    val screen: String,
    val slot: String
)

@Composable
fun rememberFocusMemoryKey(screen: String, slot: String): FocusMemoryKey {
    return remember(screen, slot) { FocusMemoryKey(screen = screen, slot = slot) }
}

@Composable
fun TvInitialFocus(
    focusRequester: FocusRequester,
    enabled: Boolean = true
) {
    LaunchedEffect(focusRequester, enabled) {
        if (enabled) {
            runCatching { focusRequester.requestFocus() }
        }
    }
}

@Composable
fun FocusRestoreHost(content: @Composable () -> Unit) {
    content()
}
