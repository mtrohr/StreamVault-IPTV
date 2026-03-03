package com.streamvault.app.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamvault.domain.model.Category


import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

@Composable
fun CategoryOptionsDialog(
    category: Category,
    onDismissRequest: () -> Unit,
    onSetAsDefault: (() -> Unit)? = null,
    onToggleLock: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReorderChannels: (() -> Unit)? = null
) {
    // Fix for ghost clicks: Debounce interaction for 500ms to ignore long-press release
    var canInteract by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500)
        canInteract = true
    }

    val safeDismiss = {
        if (canInteract) onDismissRequest()
    }

    AlertDialog(
        onDismissRequest = safeDismiss,
        title = { Text(text = category.name, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                
                // Option 1: Set as Default
                if (onSetAsDefault != null) {
                    androidx.tv.material3.Button(
                        onClick = { if (canInteract) onSetAsDefault() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.category_options_set_default))
                    }
                }

                // Option 2: Reorder Channels (only for virtual/custom categories)
                if (onReorderChannels != null) {
                    androidx.tv.material3.Button(
                        onClick = {
                            if (canInteract) {
                                onReorderChannels()
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("↕  Reorder Channels")
                    }
                }

                // Option 3: Lock/Unlock
                if (onToggleLock != null) {
                    val lockText = if (category.isUserProtected) stringResource(R.string.category_options_unlock) else stringResource(R.string.category_options_lock)
                    androidx.tv.material3.Button(
                        onClick = { if (canInteract) onToggleLock() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(lockText)
                    }
                }

                // Option 4: Delete (Optional)
                if (onDelete != null) {
                    androidx.tv.material3.Button(
                        onClick = { if (canInteract) onDelete() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.tv.material3.ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(stringResource(R.string.category_options_delete))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = safeDismiss) {
                Text(stringResource(R.string.category_options_cancel))
            }
        }
    )
}
