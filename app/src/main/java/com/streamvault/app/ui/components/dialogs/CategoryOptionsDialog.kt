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
    onSetAsDefault: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: (() -> Unit)? = null
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
                androidx.compose.material3.Button(
                    onClick = { if (canInteract) onSetAsDefault() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.category_options_set_default))
                }

                // Option 2: Lock/Unlock
                val lockText = if (category.isUserProtected) stringResource(R.string.category_options_unlock) else stringResource(R.string.category_options_lock)
                androidx.compose.material3.Button(
                    onClick = { if (canInteract) onToggleLock() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(lockText)
                }

                // Option 3: Delete (Optional)
                if (onDelete != null) {
                    androidx.compose.material3.Button(
                        onClick = { if (canInteract) onDelete() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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
