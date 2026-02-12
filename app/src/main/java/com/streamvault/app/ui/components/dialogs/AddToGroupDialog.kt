package com.streamvault.app.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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

@Composable
fun AddToGroupDialog(
    channel: Channel,
    groups: List<Category>, // Only custom groups
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToGroup: (Category) -> Unit,
    onRemoveFromGroup: (Category) -> Unit,
    onCreateGroup: (String) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    var showCreateGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(400.dp).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Manage: ${channel.name}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (showCreateGroup) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateGroup = false }) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            if (newGroupName.isNotBlank()) {
                                onCreateGroup(newGroupName)
                                showCreateGroup = false
                                newGroupName = ""
                            }
                        }) {
                            Text("Create")
                        }
                    }
                } else {
                    // Favorites Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onToggleFavorite,
                            modifier = Modifier.fillMaxWidth(),
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
                                text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                color = if (isFavorite) Color.Black else Color.White
                            )
                        }
                    }
                    
                    // Reordering Controls (only if enabled)
                    if (onMoveUp != null && onMoveDown != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reorder",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = onMoveUp, modifier = Modifier.weight(1f)) {
                                Text("Move Up")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = onMoveDown, modifier = Modifier.weight(1f)) {
                                Text("Move Down")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Custom Groups",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false).heightIn(max = 200.dp)
                    ) {
                        items(groups) { group ->
                            // We need to know if channel is already in this group. 
                            // Since we don't have that info passed easily, we perform the check in UI state upstream?
                            // Or we treat it as toggle logic in repository.
                            // For now, let's assume we can add/remove blindly or we need state.
                            // Ideally check if channel is in group.
                            // Let's assume clicking specific action adds/removes.
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(group.name, style = MaterialTheme.typography.bodyMedium)
                                
                                // Simplified: "Add" button always visible? 
                                // Or we need to know membership. 
                                // Let's use simple buttons for now "Add" / "Remove" logic is clearer 
                                // if we don't have membership state here.
                                // BUT user experience is better with checkboxes.
                                // I will assume we can make the parent pass membership info.
                                // For now, simple Button "Add to Group" is safer.
                                
                                Button(onClick = { onAddToGroup(group) }) {
                                    Text("Add")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showCreateGroup = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Group")
                    }
                }
            }
        }
    }
}
