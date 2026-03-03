package com.streamvault.app.ui.screens.epg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Program
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FullEpgScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: EpgViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopNavBar(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading EPG...", color = OnBackground)
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            EpgGrid(channels = uiState.channels, programsByChannel = uiState.programsByChannel)
        }
    }
}

@Composable
fun EpgGrid(channels: List<Channel>, programsByChannel: Map<String, List<Program>>) {
    // Current time to show line
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(channels) { channel ->
            val programs = channel.epgChannelId?.let { programsByChannel[it] } ?: emptyList()
            EpgRow(channel = channel, programs = programs, now = now)
        }
    }
}

@Composable
fun EpgRow(channel: Channel, programs: List<Program>, now: Long) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().height(84.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel Info
        Surface(
            onClick = { /* TODO: Play channel */ },
            modifier = Modifier.width(220.dp).fillMaxHeight().onFocusChanged { isFocused = it.isFocused },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = SurfaceElevated,
                focusedContainerColor = SurfaceHighlight
            ),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, FocusBorder),
                    shape = RoundedCornerShape(8.dp)
                )
            )
        ) {
            Row(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${channel.number}. ${channel.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) TextPrimary else OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Programs
        if (programs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().background(SurfaceElevated, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No EPG Data Available", color = OnSurfaceDim)
            }
        } else {
            LazyRow(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(programs) { program ->
                    ProgramItem(program = program, isCurrent = now in program.startTime..program.endTime)
                }
            }
        }
    }
}

@Composable
fun ProgramItem(program: Program, isCurrent: Boolean) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Format times
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startStr = format.format(Date(program.startTime))
    val endStr = format.format(Date(program.endTime))

    // Estimate width based on duration (1 min = 6dp)
    val durationMin = ((program.endTime - program.startTime) / 60000).coerceAtLeast(15)
    val itemWidth = (durationMin * 6).toInt().dp.coerceAtMost(600.dp) // Cap width to prevent ultra-wide items

    Surface(
        onClick = { /* TODO: Optional details dialog */ },
        modifier = Modifier.width(itemWidth).fillMaxHeight().onFocusChanged { isFocused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isCurrent) Primary.copy(alpha = 0.2f) else SurfaceElevated,
            focusedContainerColor = SurfaceHighlight
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = program.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isFocused) TextPrimary else (if (isCurrent) Primary else OnSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$startStr - $endStr",
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) TextSecondary else OnSurfaceDim
            )
        }
    }
}
