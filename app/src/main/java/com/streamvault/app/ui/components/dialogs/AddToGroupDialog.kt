package com.streamvault.app.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Category

import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay

@Composable
fun AddToGroupDialog(
    contentTitle: String,
    groups: List<Category>, // Only custom groups
    isFavorite: Boolean,
    memberOfGroups: List<Long>,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToGroup: (Category) -> Unit,
    onRemoveFromGroup: (Category) -> Unit,
    onCreateGroup: (String) -> Unit,
    onAddToSplitScreen: (() -> Unit)? = null,
    isSplitScreenQueued: Boolean = false
) {
    var showCreateGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Fix for ghost clicks: Debounce interaction for 500ms to ignore long-press release
    var canInteract by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        delay(500)
        canInteract = true
    }
    
    val safeDismiss = {
        if (canInteract) onDismiss()
    }

    Dialog(onDismissRequest = safeDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.add_group_manage_title, contentTitle),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = safeDismiss,
                        modifier = Modifier.focusRequester(focusRequester)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.add_group_close_cd))
                    }
                }

                if (showCreateGroup) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text(stringResource(R.string.add_group_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newGroupName.isNotBlank()) {
                                    onCreateGroup(newGroupName)
                                    showCreateGroup = false
                                    newGroupName = ""
                                }
                            }
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateGroup = false }) {
                            Text(stringResource(R.string.add_group_cancel))
                        }
                        Button(onClick = {
                            if (newGroupName.isNotBlank()) {
                                onCreateGroup(newGroupName)
                                showCreateGroup = false
                                newGroupName = ""
                            }
                        }) {
                            Text(stringResource(R.string.add_group_create))
                        }
                    }
                } else {
                    // Content in a scrollable container to prevent cut-off
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            // Favorites Toggle
                            var isFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .border(
                                        if (isFocused) 2.dp else 0.dp,
                                        if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        CircleShape
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFavorite) Color.Yellow else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color.Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFavorite) stringResource(R.string.add_group_remove_favorites) else stringResource(R.string.add_group_add_favorites),
                                    color = if (isFavorite) Color.Black else Color.White
                                )
                            }
                        }

                        // Add to Split Screen button
                        if (onAddToSplitScreen != null) {
                            item {
                                var isFocused by remember { mutableStateOf(false) }
                                Button(
                                    onClick = {
                                        onAddToSplitScreen()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .border(
                                            if (isFocused) 2.dp else 0.dp,
                                            if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            CircleShape
                                        ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSplitScreenQueued) Color(0xFF1B5E20) else Color(0xFF2E7D32)
                                    )
                                ) {
                                    Text("🔳  ", color = Color.White)
                                    Text(
                                        text = if (isSplitScreenQueued)
                                            stringResource(R.string.multiview_queued)
                                        else
                                            stringResource(R.string.multiview_add_to_split),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Reordering Controls removed (handled in dedicated reorder mode)
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = stringResource(R.string.add_group_custom_groups_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(groups) { group ->
                            val groupId = -group.id
                            val isMember = memberOfGroups.contains(groupId)
                            var isFocused by remember { mutableStateOf(false) }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.name, 
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { 
                                        if (isMember) onRemoveFromGroup(group) else onAddToGroup(group) 
                                    },
                                    modifier = Modifier
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .border(
                                            if (isFocused) 2.dp else 0.dp,
                                            if (isFocused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            CircleShape
                                        ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isMember) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (isMember) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Text(if (isMember) stringResource(R.string.add_group_remove_btn) else stringResource(R.string.add_group_add_btn))
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showCreateGroup = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.add_group_create_new_btn))
                            }
                        }
                    }
                }
            }
        }
    }
}
