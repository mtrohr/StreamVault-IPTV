package com.streamvault.app.ui.screens.multiview

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamvault.app.R
import com.streamvault.domain.model.StreamInfo

@Composable
fun MultiViewScreen(
    onBack: () -> Unit,
    viewModel: MultiViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initSlots()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 2x2 grid
            Row(modifier = Modifier.weight(1f)) {
                PlayerCell(
                    slot = uiState.slots.getOrNull(0),
                    isFocused = uiState.focusedSlotIndex == 0,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onFocused = { viewModel.setFocus(0) }
                )
                PlayerCell(
                    slot = uiState.slots.getOrNull(1),
                    isFocused = uiState.focusedSlotIndex == 1,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onFocused = { viewModel.setFocus(1) }
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                PlayerCell(
                    slot = uiState.slots.getOrNull(2),
                    isFocused = uiState.focusedSlotIndex == 2,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onFocused = { viewModel.setFocus(2) }
                )
                PlayerCell(
                    slot = uiState.slots.getOrNull(3),
                    isFocused = uiState.focusedSlotIndex == 3,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onFocused = { viewModel.setFocus(3) }
                )
            }
        }

        // Focused slot label
        val focused = uiState.slots.getOrNull(uiState.focusedSlotIndex)
        if (focused != null && focused.streamUrl.isNotBlank()) {
            Text(
                text = "▶ ${focused.title}",
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PlayerCell(
    slot: MultiViewSlot?,
    isFocused: Boolean,
    modifier: Modifier,
    onFocused: () -> Unit
) {
    Box(
        modifier = modifier
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) Color(0xFF4CAF50) else Color(0xFF333333)
            )
            .focusable()
            .onFocusChanged { if (it.isFocused) onFocused() }
            .background(Color(0xFF111111)),
        contentAlignment = Alignment.Center
    ) {
        when {
            slot == null || slot.streamUrl.isBlank() -> {
                // Empty slot
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "+",
                        color = Color(0xFF555555),
                        fontSize = 32.sp
                    )
                    Text(
                        text = stringResource(R.string.multiview_empty_slot),
                        color = Color(0xFF555555),
                        fontSize = 12.sp
                    )
                }
            }
            slot.isLoading -> {
                CircularProgressIndicator(
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            }
            slot.hasError -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠",
                        color = Color(0xFFFF5252),
                        fontSize = 24.sp
                    )
                    Text(
                        text = stringResource(R.string.multiview_stream_error),
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                // Render the player surface
                val engine = slot.playerEngine
                if (engine != null) {
                    AndroidView(
                        factory = { ctx ->
                            val surface = engine.getPlayerView() as? SurfaceView ?: SurfaceView(ctx)
                            surface
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Title overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Text(
                        text = slot.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
