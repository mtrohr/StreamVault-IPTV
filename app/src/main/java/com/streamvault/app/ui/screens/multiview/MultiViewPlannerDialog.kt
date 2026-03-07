package com.streamvault.app.ui.screens.multiview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamvault.app.R
import com.streamvault.domain.model.Channel

@Composable
fun MultiViewPlannerDialog(
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    viewModel: MultiViewViewModel = hiltViewModel()
) {
    val queue by viewModel.queueFlow.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔳  " + stringResource(R.string.multiview_planner_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${queue.size}/${MultiViewManager.MAX_SLOTS}",
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Queue list
                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.multiview_planner_empty),
                            color = Color(0xFF888888),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(queue, key = { it.id }) { channel ->
                            QueueItem(
                                channel = channel,
                                onRemove = { viewModel.removeFromQueue(channel.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearQueue()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252)
                        )
                    ) {
                        Text(stringResource(R.string.multiview_planner_clear))
                    }

                    Button(
                        onClick = onLaunch,
                        enabled = queue.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(stringResource(R.string.multiview_planner_launch))
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItem(
    channel: Channel,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16213E), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = channel.name,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Text("✕", color = Color(0xFFFF5252), fontSize = 14.sp)
        }
    }
}
