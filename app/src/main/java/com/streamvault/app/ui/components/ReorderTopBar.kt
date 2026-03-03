package com.streamvault.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.streamvault.app.ui.theme.OnSurface
import com.streamvault.app.ui.theme.Primary

@Composable
fun ReorderTopBar(
    categoryName: String,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier.fillMaxWidth().height(80.dp).padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Reordering '$categoryName'",
                style = MaterialTheme.typography.titleLarge,
                color = Primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = OnSurface
                    )
                ) {
                    Text("Cancel", modifier = Modifier.padding(horizontal = 8.dp))
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.colors(
                        containerColor = Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save Order", modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}
