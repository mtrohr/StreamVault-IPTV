package com.streamvault.app.ui.screens.multiview

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import com.streamvault.app.R
import com.streamvault.app.ui.components.dialogs.PremiumDialog
import com.streamvault.app.ui.components.dialogs.PremiumDialogActionButton
import com.streamvault.app.ui.components.dialogs.PremiumDialogFooterButton
import com.streamvault.app.ui.theme.Primary

@Composable
fun MultiViewScreen(
    onBack: () -> Unit,
    viewModel: MultiViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val firstSlotFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var showReplacementPicker by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        viewModel.initSlots()
        try {
            kotlinx.coroutines.delay(100)
            firstSlotFocusRequester.requestFocus()
        } catch (_: Exception) {
            // No-op: focus request can fail during composition transitions.
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (showReplacementPicker) {
            ReplaceSlotDialog(
                candidates = uiState.replacementCandidates,
                onDismiss = { showReplacementPicker = false },
                onReplace = { channel ->
                    viewModel.replaceFocusedSlot(channel)
                    showReplacementPicker = false
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                PlayerCell(
                    slot = uiState.slots.getOrNull(0),
                    isFocused = uiState.focusedSlotIndex == 0,
                    showSelectionBorder = uiState.showSelectionBorder,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(firstSlotFocusRequester),
                    onFocused = { viewModel.setFocus(0) }
                )
                PlayerCell(
                    slot = uiState.slots.getOrNull(1),
                    isFocused = uiState.focusedSlotIndex == 1,
                    showSelectionBorder = uiState.showSelectionBorder,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onFocused = { viewModel.setFocus(1) }
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                PlayerCell(
                    slot = uiState.slots.getOrNull(2),
                    isFocused = uiState.focusedSlotIndex == 2,
                    showSelectionBorder = uiState.showSelectionBorder,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onFocused = { viewModel.setFocus(2) }
                )
                PlayerCell(
                    slot = uiState.slots.getOrNull(3),
                    isFocused = uiState.focusedSlotIndex == 3,
                    showSelectionBorder = uiState.showSelectionBorder,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onFocused = { viewModel.setFocus(3) }
                )
            }
        }

        val focused = uiState.slots.getOrNull(uiState.focusedSlotIndex)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MultiViewPolicyCard(
                policy = uiState.performancePolicy,
                telemetry = uiState.telemetry,
                onModeSelected = viewModel::setPerformanceMode
            )
            if (focused != null && focused.title.isNotBlank()) {
                Text(
                    text = stringResource(R.string.multiview_focused_prefix, focused.title),
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            if (uiState.presets.isNotEmpty()) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.presets.forEach { preset ->
                        val presetLabel = stringResource(R.string.multiview_preset_label, preset.index + 1)
                        Button(onClick = { viewModel.loadPreset(preset.index) }) {
                            Text(
                                text = if (preset.isPopulated) {
                                    "$presetLabel (${preset.channelCount})"
                                } else {
                                    presetLabel
                                }
                            )
                        }
                        Button(onClick = { viewModel.saveCurrentAsPreset(preset.index) }) {
                            Text(text = stringResource(R.string.multiview_preset_save, preset.index + 1))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showReplacementPicker = true },
                    enabled = uiState.replacementCandidates.isNotEmpty()
                ) {
                    Text(stringResource(R.string.multiview_replace_slot))
                }
                Button(
                    onClick = { viewModel.removeFocusedSlot() },
                    enabled = focused != null && !focused.isEmpty
                ) {
                    Text(stringResource(R.string.multiview_remove_slot))
                }
                if (uiState.pinnedAudioSlotIndex == uiState.focusedSlotIndex) {
                    Button(onClick = { viewModel.clearPinnedAudio() }) {
                        Text(stringResource(R.string.multiview_audio_follow_focus))
                    }
                } else {
                    Button(
                        onClick = { viewModel.pinAudioToFocusedSlot() },
                        enabled = focused != null && !focused.isEmpty
                    ) {
                        Text(stringResource(R.string.multiview_pin_audio))
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerCell(
    slot: MultiViewSlot?,
    isFocused: Boolean,
    showSelectionBorder: Boolean,
    modifier: Modifier,
    onFocused: () -> Unit
) {
    val showBorder = isFocused && showSelectionBorder

    Surface(
        onClick = { },
        modifier = modifier
            .padding(2.dp)
            .onFocusChanged { if (it.isFocused) onFocused() },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(
                    width = if (showBorder) 4.dp else 0.dp,
                    color = if (showBorder) Color.White else Color.Transparent
                )
            ),
            focusedBorder = Border.None,
            pressedBorder = Border.None
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF111111),
            contentColor = Color.White,
            focusedContainerColor = Color(0xFF111111)
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                slot == null || slot.isEmpty -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+", color = Color(0xFF555555), fontSize = 32.sp)
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.multiview_empty_slot),
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
                        Text("!", color = Color(0xFFFF5252), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.multiview_stream_error),
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                !slot.performanceBlockedReason.isNullOrBlank() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.multiview_policy_blocked),
                            color = Color(0xFFFFC107),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = slot.performanceBlockedReason.orEmpty(),
                            color = Color(0xFFFFE082),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                else -> {
                    val engine = slot.playerEngine
                    if (engine != null) {
                        val exoPlayer = engine.getPlayerView()
                        if (exoPlayer is androidx.media3.common.Player) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        isFocusable = false
                                        isFocusableInTouchMode = false
                                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                                    }
                                },
                                update = { view ->
                                    if (view.player != exoPlayer) {
                                        view.player = exoPlayer
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (isFocused && !slot.isEmpty) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (slot.isAudioPinned) {
                                    stringResource(R.string.multiview_audio_pinned_badge)
                                } else {
                                    stringResource(R.string.multiview_audio_badge)
                                },
                                color = Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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
}

@Composable
private fun MultiViewPolicyCard(
    policy: MultiViewPerformancePolicyUiModel,
    telemetry: MultiViewTelemetryUiModel,
    onModeSelected: (MultiViewPerformanceMode) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(
                R.string.multiview_policy_summary,
                policy.tier.name.lowercase().replaceFirstChar { it.uppercase() },
                policy.maxActiveSlots
            ),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = policy.summary,
            color = Color(0xFFB0BEC5),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.multiview_telemetry_snapshot,
                    telemetry.activeSlots,
                    telemetry.standbySlots,
                    telemetry.bufferingSlots,
                    telemetry.errorSlots
                ),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = stringResource(
                    R.string.multiview_telemetry_detail,
                    telemetry.totalDroppedFrames,
                    telemetry.droppedFramesDelta,
                    telemetry.sustainedLoadScore,
                    when (telemetry.thermalStatus) {
                        MultiViewThermalStatus.NORMAL -> stringResource(R.string.multiview_thermal_normal)
                        MultiViewThermalStatus.LIGHT -> stringResource(R.string.multiview_thermal_light)
                        MultiViewThermalStatus.MODERATE -> stringResource(R.string.multiview_thermal_moderate)
                        MultiViewThermalStatus.SEVERE -> stringResource(R.string.multiview_thermal_severe)
                        MultiViewThermalStatus.CRITICAL -> stringResource(R.string.multiview_thermal_critical)
                        MultiViewThermalStatus.UNKNOWN -> stringResource(R.string.multiview_thermal_unknown)
                    }
                ),
                color = Color(0xFFB0BEC5),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            if (telemetry.isLowMemory) {
                Text(
                    text = stringResource(R.string.multiview_low_memory_warning),
                    color = Color(0xFFFFC107),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            telemetry.throttledReason?.let { reason ->
                Text(
                    text = reason,
                    color = Color(0xFFFFE082),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = telemetry.recommendation,
                color = Color(0xFF90CAF9),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MultiViewPerformanceMode.entries.forEach { mode ->
                Surface(
                    onClick = { onModeSelected(mode) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (policy.mode == mode) Color(0xFF1E3A5F) else Color(0xFF1A1A30),
                        focusedContainerColor = Color(0xFF294B75)
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border(
                            androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (policy.mode == mode) Color.White else Color.Transparent
                            )
                        )
                    )
                ) {
                    Text(
                        text = when (mode) {
                            MultiViewPerformanceMode.AUTO -> stringResource(R.string.multiview_policy_auto)
                            MultiViewPerformanceMode.CONSERVATIVE -> stringResource(R.string.multiview_policy_conservative)
                            MultiViewPerformanceMode.BALANCED -> stringResource(R.string.multiview_policy_balanced)
                            MultiViewPerformanceMode.MAXIMUM -> stringResource(R.string.multiview_policy_maximum)
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplaceSlotDialog(
    candidates: List<com.streamvault.domain.model.Channel>,
    onDismiss: () -> Unit,
    onReplace: (com.streamvault.domain.model.Channel) -> Unit
) {
    PremiumDialog(
        title = stringResource(R.string.multiview_replace_title),
        subtitle = stringResource(R.string.multiview_replace_empty),
        onDismissRequest = onDismiss,
        widthFraction = 0.5f,
        content = {
            if (candidates.isEmpty()) {
                Text(
                    text = stringResource(R.string.multiview_replace_empty),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    candidates.forEach { channel ->
                        PremiumDialogActionButton(
                            label = channel.name,
                            onClick = { onReplace(channel) }
                        )
                    }
                }
            }
        },
        footer = {
            PremiumDialogFooterButton(
                label = stringResource(R.string.settings_cancel),
                onClick = onDismiss
            )
        }
    )
}
