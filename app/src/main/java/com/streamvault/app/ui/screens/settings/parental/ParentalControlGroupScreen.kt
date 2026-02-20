package com.streamvault.app.ui.screens.settings.parental

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.tv.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.domain.model.Category
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester

@Composable
fun ParentalControlGroupScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ParentalControlGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        backButtonFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopNavBar(
            currentRoute = currentRoute,
            onNavigate = onNavigate
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.focusRequester(backButtonFocusRequester)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "🛡️ Protected Groups",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        // Search Bar
        // Search Bar with Click-to-Focus (No layout jump)
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        var isSearchActive by remember { mutableStateOf(false) }
        val searchFocusRequester = remember { FocusRequester() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .onFocusChanged { 
                        if (!it.isFocused && isSearchActive) {
                             // Optional: if focus lost via other means
                        }
                    },
                placeholder = { Text("Search categories...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { 
                        focusManager.clearFocus() 
                        isSearchActive = false
                    }
                ),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Overlay to intercept clicks when inactive
            if (!isSearchActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null, // No ripple on overlay, or use ripple if desired
                            onClick = {
                                isSearchActive = true
                                searchFocusRequester.requestFocus()
                            }
                        )
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories, key = { it.id }) { category ->
                    CategoryProtectionCard(
                        category = category,
                        onToggle = { viewModel.toggleCategoryProtection(category) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryProtectionCard(
    category: Category,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Optimistic state for smooth animation
    // We initialize with the category state, but update immediately on user interaction
    var isChecked by remember(category.isUserProtected, category.isAdult) { 
        mutableStateOf(category.isUserProtected || category.isAdult) 
    }
    
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isFocused) 2.dp else 0.dp
    val backgroundColor = if (isFocused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        onClick = { 
            if (!category.isAdult) {
                isChecked = !isChecked // Optimistic update
                onToggle() 
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(borderWidth, borderColor, MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            pressedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
            pressedContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (category.isAdult) {
                    Text(
                        text = "Adult Category (Auto-Protected)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            androidx.compose.material3.Switch(
                checked = isChecked,
                onCheckedChange = { 
                     if (!category.isAdult) {
                         isChecked = it // Optimistic update
                         onToggle()
                     }
                },
                enabled = !category.isAdult,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
