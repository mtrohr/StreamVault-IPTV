package com.streamvault.app.ui.screens.provider

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Provider
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ──────────────────────────────────────────────────────

@HiltViewModel
class ProviderSetupViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderSetupState())
    val uiState: StateFlow<ProviderSetupState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            providerRepository.getActiveProvider().collect { provider ->
                if (provider != null) {
                    _uiState.update { it.copy(hasExistingProvider = true) }
                }
            }
        }
    }

    fun loginXtream(serverUrl: String, username: String, password: String, name: String) {
        // Clear previous errors
        _uiState.update { it.copy(validationError = null, error = null) }
        
        // Validation
        if (serverUrl.isBlank()) {
            _uiState.update { it.copy(validationError = "Please enter server URL") }
            return
        }
        if (username.isBlank()) {
            _uiState.update { it.copy(validationError = "Please enter username") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, validationError = null) }
            when (val result = providerRepository.loginXtream(serverUrl, username, password, name)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, loginSuccess = true, error = null)
                    }
                }
                is Result.Error -> {
                    val userMessage = when {
                        result.message.contains("authentication failed", ignoreCase = true) || 
                        result.message.contains("auth", ignoreCase = true) ->
                            "Login failed — please check your credentials and server URL"
                        result.message.contains("unable to connect", ignoreCase = true) ||
                        result.message.contains("timeout", ignoreCase = true) ||
                        result.message.contains("network", ignoreCase = true) ->
                            "Cannot reach server — check your internet CONNECTION and server URL"
                        else -> result.message
                    }
                    _uiState.update {
                        it.copy(isLoading = false, error = userMessage)
                    }
                }
                is Result.Loading -> { /* no-op */ }
            }
        }
    }

    fun addM3u(url: String, name: String) {
        // Clear previous errors
        _uiState.update { it.copy(validationError = null, error = null) }
        
        // Validation
        if (url.isBlank()) {
            _uiState.update { it.copy(validationError = "Please enter M3U URL") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, validationError = null) }
            when (val result = providerRepository.validateM3u(url, name)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, loginSuccess = true, error = null)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Could not validate playlist URL — please check the URL: ${result.message}")
                    }
                }
                is Result.Loading -> { /* no-op */ }
            }
        }
    }
}

data class ProviderSetupState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val hasExistingProvider: Boolean = false,
    val error: String? = null,
    val validationError: String? = null
)

// ── Screen ─────────────────────────────────────────────────────────

@Composable
fun ProviderSetupScreen(
    onProviderAdded: () -> Unit,
    viewModel: ProviderSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate on success
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onProviderAdded()
    }

    // Auto-skip if already configured
    LaunchedEffect(uiState.hasExistingProvider) {
        if (uiState.hasExistingProvider) onProviderAdded()
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Xtream, 1 = M3U
    var name by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "StreamVault",
                style = MaterialTheme.typography.displaySmall,
                color = Primary
            )
            Text(
                text = "Add your IPTV provider",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabButton("Xtream Codes", selectedTab == 0) { selectedTab = 0 }
                TabButton("M3U Playlist", selectedTab == 1) { selectedTab = 1 }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playlist Name (Common)
            TvTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = "Playlist Name (Optional)"
            )

            when (selectedTab) {
                0 -> {
                    TvTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = "Server URL")
                    TvTextField(value = username, onValueChange = { username = it }, label = "Username")
                    TvTextField(value = password, onValueChange = { password = it }, label = "Password")

                    ActionButton(
                        text = if (uiState.isLoading) "Connecting..." else "Login",
                        enabled = !uiState.isLoading,
                        onClick = {
                            viewModel.loginXtream(serverUrl, username, password, name)
                        }
                    )
                }
                1 -> {
                    TvTextField(value = m3uUrl, onValueChange = { m3uUrl = it }, label = "M3U URL")

                    ActionButton(
                        text = if (uiState.isLoading) "Adding..." else "Add Playlist",
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
