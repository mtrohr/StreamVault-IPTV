package com.streamvault.app.ui.screens.provider

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.tv.material3.*
import com.streamvault.app.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.streamvault.app.R

@Composable
fun ProviderSetupScreen(
    onProviderAdded: () -> Unit,
    editProviderId: Long? = null,
    viewModel: ProviderSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        var fileName = "Local_Playlist"
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (displayNameIndex != -1) {
                                    val displayName = it.getString(displayNameIndex)
                                    if (displayName.contains(".")) {
                                        fileName = displayName.substringBeforeLast(".")
                                    } else {
                                        fileName = displayName
                                    }
                                }
                            }
                        }

                        val outFile = java.io.File(context.filesDir, "m3u_${System.currentTimeMillis()}.m3u")
                        outFile.outputStream().use { out ->
                            inputStream.copyTo(out)
                        }

                        withContext(Dispatchers.Main) {
                            m3uUrl = "file://${outFile.absolutePath}"
                            if (name.isEmpty()) name = fileName
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Navigate on success
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onProviderAdded()
    }

    // Auto-skip logic moved to MainActivity/Splash - preventing redirect loop here
    // LaunchedEffect(uiState.hasExistingProvider) {
    //    if (uiState.hasExistingProvider) onProviderAdded()
    // }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Xtream, 1 = M3U
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Initialize state from ViewModel if editing
    LaunchedEffect(editProviderId) {
        if (editProviderId != null) {
            viewModel.loadProvider(editProviderId)
        }
    }

    // Reflect ViewModel state in local state once loaded
    LaunchedEffect(uiState.isEditing, uiState.existingProviderId) {
        if (uiState.isEditing) {
            selectedTab = uiState.selectedTab
            name = uiState.name
            serverUrl = uiState.serverUrl
            username = uiState.username
            password = uiState.password
            m3uUrl = uiState.m3uUrl
        }
    }

    // Keep local state for editing
    // var selectedTab by remember { mutableStateOf(0) } - moved below to use state driven init? No, we need local state for editing.
    // We already have the vars defined above. 

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = if (uiState.isEditing) stringResource(R.string.setup_edit_provider) else stringResource(R.string.setup_title_streamvault),
                style = MaterialTheme.typography.displaySmall,
                color = Primary
            )
            Text(
                text = if (uiState.isEditing) stringResource(R.string.setup_update_desc) else stringResource(R.string.setup_add_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab selector - Disable changing type during edit
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                
                if (!uiState.isEditing || uiState.selectedTab == 0) {
                     TabButton(stringResource(R.string.setup_xtream), selectedTab == 0) { if (!uiState.isEditing) selectedTab = 0 }
                }
                if (!uiState.isEditing || uiState.selectedTab == 1) {
                     TabButton(stringResource(R.string.setup_m3u), selectedTab == 1) { if (!uiState.isEditing) selectedTab = 1 }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playlist Name (Common)
            TvTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = stringResource(R.string.setup_name_hint)
            )

            when (selectedTab) {
                0 -> {
                    TvTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = stringResource(R.string.setup_server_hint))
                    TvTextField(value = username, onValueChange = { username = it }, label = stringResource(R.string.setup_user_hint))
                    TvTextField(value = password, onValueChange = { password = it }, label = stringResource(R.string.setup_pass_hint))

                    ActionButton(
                        text = if (uiState.isLoading) stringResource(R.string.setup_connecting) else if (uiState.isEditing) stringResource(R.string.setup_save) else stringResource(R.string.setup_login),
                        enabled = !uiState.isLoading,
                        onClick = {
                            viewModel.loginXtream(serverUrl, username, password, name)
                        }
                    )
                }
                1 -> {
                    TvTextField(value = m3uUrl, onValueChange = { m3uUrl = it }, label = stringResource(R.string.setup_m3u_hint))

                    ActionButton(
                        text = "Select Local File",
                        enabled = !uiState.isLoading,
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                    )

                    ActionButton(
                        text = if (uiState.isLoading) stringResource(R.string.setup_validating) else if (uiState.isEditing) stringResource(R.string.setup_save) else stringResource(R.string.setup_add),
                        enabled = !uiState.isLoading,
                        onClick = { viewModel.addM3u(m3uUrl, name) }
                    )
                }
            }

            // Validation error
            uiState.validationError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorColor
                )
            }
            
            // Error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorColor
                )
            }
        }
    }

    if (uiState.syncProgress != null) {
        SyncProgressDialog(message = uiState.syncProgress!!)
    }
}

@Composable
fun SyncProgressDialog(message: String) {
    Dialog(onDismissRequest = {}) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(300.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    val borderColor = if (isFocused) Primary else SurfaceHighlight
    val bgColor = if (isFocused) Surface else SurfaceElevated
    val borderWidth = if (isFocused) 2.dp else 1.dp

    // Use Box instead of TV Surface so key events (paste, etc.) reach BasicTextField
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Placeholder text
        if (value.isEmpty() && !isFocused) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim
            )
        }

        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = OnBackground
            ),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Primary)
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(150),
        label = "btnScale"
    )

    Surface(
        onClick = { if (enabled) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (enabled) Primary else SurfaceHighlight,
            focusedContainerColor = if (enabled) PrimaryVariant else SurfaceHighlight
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) OnBackground else OnSurfaceDim
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.2f) else SurfaceElevated,
            focusedContainerColor = SurfaceHighlight
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Primary else OnSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}
