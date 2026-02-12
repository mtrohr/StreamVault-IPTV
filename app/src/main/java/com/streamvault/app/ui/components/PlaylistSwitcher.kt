package com.streamvault.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Provider

@Composable
fun PlaylistSwitcher(
    currentProvider: Provider?,
    allProviders: List<Provider>,
    onProviderSelected: (Provider) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProviderList by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Current provider button
        Surface(
            onClick = { showProviderList = !showProviderList },
            modifier = Modifier.onFocusChanged { isFocused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (isFocused) Primary.copy(alpha = 0.2f) else SurfaceElevated,
                focusedContainerColor = Primary.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📺",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = currentProvider?.name ?: "No Provider",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFocused) Primary else OnBackground
                )
                Text(
                    text = if (showProviderList) "▲" else "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }
        }

        // Provider list dropdown
        if (showProviderList && allProviders.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .padding(top = 48.dp)
                    .width(250.dp),
                shape = RoundedCornerShape(8.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = SurfaceElevated
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    allProviders.forEach { provider ->
                        ProviderItem(
                            provider = provider,
                            isSelected = provider.id == currentProvider?.id,
                            onClick = {
                                onProviderSelected(provider)
                                showProviderList = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderItem(
    provider: Provider,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent,
            focusedContainerColor = Primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFocused || isSelected) Primary else OnBackground
            )
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary
                )
            }
        }
    }
}
