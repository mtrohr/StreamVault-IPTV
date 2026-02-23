package com.streamvault.app.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.streamvault.app.R
import com.streamvault.app.ui.theme.Primary
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Program
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EpgOverlay(
    isVisible: Boolean,
    currentChannel: Channel?,
    currentProgram: Program?,
    nextProgram: Program?,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startX = 0f,
                        endX = 1000f
                    )
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(400.dp)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Channel Info Header
                if (currentChannel != null) {
                    Text(
                        text = "${currentChannel.number}. ${currentChannel.name}",
                        style = MaterialTheme.typography.displaySmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Now Playing
                Text(
                    text = stringResource(R.string.epg_now_playing),
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (currentProgram != null) {
                    Text(
                        text = currentProgram.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${timeFormat.format(Date(currentProgram.startTime))} - ${timeFormat.format(Date(currentProgram.endTime))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    if (!currentProgram.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentProgram.description!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val now = System.currentTimeMillis()
                    val start = currentProgram.startTime
                    val end = currentProgram.endTime
                    if (start in 1..<end) {
                        val progress = (now - start).toFloat() / (end - start)
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.epg_no_data),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Next Up
                if (nextProgram != null) {
                    Text(
                        text = stringResource(R.string.epg_up_next),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nextProgram.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${timeFormat.format(Date(nextProgram.startTime))} - ${timeFormat.format(Date(nextProgram.endTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
