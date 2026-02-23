package com.streamvault.app.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamvault.app.ui.theme.ErrorColor
import com.streamvault.app.ui.theme.Primary
import com.streamvault.app.ui.theme.SurfaceElevated
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

@Composable
fun PinDialog(
    onDismissRequest: () -> Unit,
    onPinEntered: (String) -> Unit,
    title: String? = null,
    error: String? = null
) {
    var pin by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // Auto-submit when length reaches 4
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            delay(300) // Small delay for visual feedback
            onPinEntered(pin)
            pin = "" // Reset logic usually handled by parent state (error or dismiss)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .background(SurfaceElevated, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = title ?: stringResource(R.string.pin_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < pin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) Primary else Color.White.copy(alpha = 0.1f)
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) Primary else Color.White.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorColor
                    )
                }

                // Hidden text field to capture input overlays everything but is invisible
                // We use a small size to ensure it doesn't displace layout but is focusable
                BasicTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                        }
                    },
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                
                // Cancel Button
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.pin_dialog_cancel), color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
