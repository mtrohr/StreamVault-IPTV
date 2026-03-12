package com.streamvault.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.streamvault.app.R
import com.streamvault.app.navigation.Routes
import com.streamvault.app.ui.theme.FocusBorder
import com.streamvault.app.ui.theme.LocalSpacing
import com.streamvault.app.ui.theme.Primary
import com.streamvault.app.ui.theme.SurfaceElevated
import com.streamvault.app.ui.theme.TextPrimary
import com.streamvault.app.ui.theme.TextSecondary
import com.streamvault.app.ui.theme.TextTertiary

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TopNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTab(stringResource(id = R.string.nav_home), Routes.HOME),
        NavTab(stringResource(id = R.string.nav_live_tv), Routes.LIVE_TV),
        NavTab(stringResource(id = R.string.nav_movies), Routes.MOVIES),
        NavTab(stringResource(id = R.string.nav_series), Routes.SERIES),
        NavTab(stringResource(id = R.string.nav_epg), Routes.EPG),
        NavTab(stringResource(id = R.string.nav_settings), Routes.SETTINGS)
    )
    val subtitle = when (currentRoute) {
        Routes.FAVORITES -> stringResource(id = R.string.favorites_title)
        Routes.SEARCH -> stringResource(id = R.string.search_title)
        else -> tabs.firstOrNull { it.route == currentRoute }?.label ?: stringResource(id = R.string.app_name)
    }

    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(horizontal = LocalSpacing.current.safeHoriz)
            .focusProperties {
                enter = {
                    val activeTabRoute = tabs.firstOrNull { it.route == currentRoute }?.route
                    focusRequesters[activeTabRoute] ?: FocusRequester.Default
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.width(LocalSpacing.current.md))

        Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
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
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "tabScale"
    )

    val textColor = when {
        isSelected -> Primary
        isFocused -> TextPrimary
        else -> TextSecondary
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.16f) else SurfaceElevated,
            focusedContainerColor = SurfaceElevated
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, SurfaceElevated),
                shape = RoundedCornerShape(12.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(12.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = textColor
            )
            AnimatedVisibility(visible = isSelected || isFocused) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                color = if (isSelected) Primary else FocusBorder,
                                shape = RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
        }
    }
}

private data class NavTab(
    val label: String,
    val route: String
)
