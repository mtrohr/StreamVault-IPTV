package com.streamvault.app.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

@Composable
fun RenameGroupDialog(
    initialName: String,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by rememberSaveable(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            androidx.tv.material3.Text(stringResource(R.string.category_options_rename))
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    androidx.tv.material3.Text(stringResource(R.string.add_group_name_hint))
                },
                supportingText = errorMessage?.let { message ->
                    {
                        androidx.tv.material3.Text(message)
                    }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                androidx.tv.material3.Text(stringResource(R.string.add_group_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                androidx.tv.material3.Text(stringResource(R.string.add_group_cancel))
            }
        }
    )
}
