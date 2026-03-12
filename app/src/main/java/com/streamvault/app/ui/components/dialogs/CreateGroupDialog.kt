package com.streamvault.app.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.DialogProperties
import com.streamvault.app.R

@Composable
fun CreateGroupDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            androidx.tv.material3.Text(stringResource(R.string.add_group_create_new_btn))
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = {
                    androidx.tv.material3.Text(stringResource(R.string.add_group_name_hint))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val normalized = value.trim()
                        if (normalized.isNotEmpty()) {
                            keyboardController?.hide()
                            onConfirm(normalized)
                        }
                    }
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = value.trim()
                    if (normalized.isNotEmpty()) {
                        keyboardController?.hide()
                        onConfirm(normalized)
                    }
                },
                enabled = value.trim().isNotEmpty()
            ) {
                androidx.tv.material3.Text(stringResource(R.string.add_group_create))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                keyboardController?.hide()
                onDismissRequest()
            }) {
                androidx.tv.material3.Text(stringResource(R.string.add_group_cancel))
            }
        }
    )
}
