package com.streamvault.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.streamvault.app.navigation.Routes
import com.streamvault.app.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TopNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTab(stringResource(id = R.string.nav_live_tv), Routes.HOME, "📺"),
        NavTab(stringResource(id = R.string.nav_movies), Routes.MOVIES, "🎬"),
        NavTab(stringResource(id = R.string.nav_series), Routes.SERIES, "📺"),
        // Search and Favorites are accessible from within the Live TV screen;
        // they don't need top-level nav slots.
        NavTab(stringResource(id = R.string.nav_settings), Routes.SETTINGS, "⚙️")
    )

    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 32.dp)
            .focusProperties {
                enter = {
                    val activeTabRoute = tabs.firstOrNull { it.route == currentRoute }?.route
                    val activeRequester = focusRequesters[activeTabRoute]
                    activeRequester ?: FocusRequester.Default
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Logo / App Name
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = Primary,
            modifier = Modifier.padding(end = 32.dp)
        )

        tabs.forEach { tab ->
            val requester = focusRequesters.getOrPut(tab.route) { FocusRequester() }
            NavTabButton(
                text = "${tab.icon} ${tab.label}",
                isSelected = currentRoute == tab.route,
                modifier = Modifier.focusRequester(requester),
                onClick = {
                    if (currentRoute != tab.route) {
                        onNavigate(tab.route)
                    }
                }
            )
        }
    }
}

@Composable
private fun NavTabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "tabScale"
    )

    val backgroundColor = when {
        isSelected -> Primary.copy(alpha = 0.2f)
        isFocused -> SurfaceElevated
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Primary
        isFocused -> OnBackground
        else -> OnSurface
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = SurfaceElevated
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private data class NavTab(
    val label: String,
    val route: String,
    val icon: String
)
