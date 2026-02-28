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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
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
        NavTab(stringResource(id = R.string.nav_live_tv), Routes.HOME),
        NavTab(stringResource(id = R.string.nav_movies), Routes.MOVIES),
        NavTab(stringResource(id = R.string.nav_series), Routes.SERIES),
        NavTab(stringResource(id = R.string.nav_settings), Routes.SETTINGS)
    )

    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp) // Premium height
            .padding(horizontal = LocalSpacing.current.safeHoriz) // Premium 48dp overscan
            .focusProperties {
                enter = {
                    val activeTabRoute = tabs.firstOrNull { it.route == currentRoute }?.route
                    val activeRequester = focusRequesters[activeTabRoute]
                    activeRequester ?: FocusRequester.Default
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg) // Premium 32dp spacing
    ) {
        // Logo / App Name
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = Primary,
            modifier = Modifier.padding(end = LocalSpacing.current.lg)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)
        ) {
            tabs.forEach { tab ->
                val requester = focusRequesters.getOrPut(tab.route) { FocusRequester() }
                NavTabButton(
                    text = tab.label,
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
        targetValue = if (isFocused) 1.03f else 1f, // Premium subtle scale
        animationSpec = tween(durationMillis = 200),
        label = "tabScale"
    )

    val backgroundColor = when {
        isSelected -> Primary.copy(alpha = 0.15f) // Cleaner selection tint
        isFocused -> SurfaceElevated
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Primary
        isFocused -> TextPrimary // High contrast white
        else -> TextSecondary // Muted inactive
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)), // Premium 12dp
        colors = ClickableSurfaceDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = SurfaceElevated
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder), // Premium White Focus
                shape = RoundedCornerShape(12.dp)
            )
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall, // 16sp, robust TV font
            color = textColor,
            modifier = Modifier.padding(
                horizontal = LocalSpacing.current.md, 
                vertical = LocalSpacing.current.xs
            )
        )
    }
}

private data class NavTab(
    val label: String,
    val route: String
)
