package com.streamvault.app.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.streamvault.app.R
import com.streamvault.domain.model.Category
import kotlinx.coroutines.delay

@Composable
fun CategoryOptionsDialog(
    category: Category,
    onDismissRequest: () -> Unit,
    onSetAsDefault: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onToggleLock: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReorderChannels: (() -> Unit)? = null
) {
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
        title = {
            androidx.tv.material3.Text(
                text = category.name,
                style = androidx.tv.material3.MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSetAsDefault != null) {
                    androidx.tv.material3.Button(
                        onClick = { if (canInteract) onSetAsDefault() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.tv.material3.Text(stringResource(R.string.category_options_set_default))
                    }
                }

                if (onRename != null) {
                    androidx.tv.material3.Button(
                        onClick = { if (canInteract) onRename() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.tv.material3.Text(stringResource(R.string.category_options_rename))
                    }
                }

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
                        androidx.tv.material3.Text(stringResource(R.string.category_options_reorder))
                    }
                }

                if (onToggleLock != null) {
                    val lockText = if (category.isUserProtected) {
                        stringResource(R.string.category_options_unlock)
                    } else {
                        stringResource(R.string.category_options_lock)
                    }
                    androidx.tv.material3.Button(
                        onClick = { if (canInteract) onToggleLock() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.tv.material3.Text(lockText)
                    }
                }

                if (onDelete != null) {
                    androidx.tv.material3.Button(
                        onClick = { if (canInteract) onDelete() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.tv.material3.ButtonDefaults.colors(
                            containerColor = androidx.tv.material3.MaterialTheme.colorScheme.errorContainer,
                            contentColor = androidx.tv.material3.MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        androidx.tv.material3.Text(stringResource(R.string.category_options_delete))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = safeDismiss) {
                androidx.tv.material3.Text(stringResource(R.string.category_options_cancel))
            }
        }
    )
}
