package com.streamvault.app.ui.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

@Composable
fun DeleteGroupDialog(
    groupName: String,
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    // Fix for ghost clicks: Debounce interaction for 500ms to ignore long-press release
    var canInteract by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        canInteract = true
    }

    val safeDismiss = {
        if (canInteract) onDismissRequest()
    }

    AlertDialog(
        onDismissRequest = safeDismiss,
        title = { Text(stringResource(R.string.home_delete_group_title)) },
        text = { Text(stringResource(R.string.home_delete_group_body, groupName)) },
        confirmButton = {
            TextButton(onClick = { if (canInteract) onConfirmDelete() }) {
                Text(stringResource(R.string.home_delete_group_confirm), color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = safeDismiss) {
                Text(stringResource(R.string.home_delete_group_cancel))
            }
        }
    )
}
