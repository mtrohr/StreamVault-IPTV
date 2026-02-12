package com.streamvault.app.ui.screens.series

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.*
import com.streamvault.app.ui.components.CategoryRow
import com.streamvault.app.ui.components.SeriesCard
import com.streamvault.app.ui.components.TopNavBar
import com.streamvault.app.ui.theme.*
import com.streamvault.domain.model.Episode
import com.streamvault.domain.model.Series
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val seriesRepository: SeriesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            providerRepository.getActiveProvider()
                .filterNotNull()
                .collectLatest { provider ->
                    seriesRepository.getSeries(provider.id).collect { seriesList ->
                        val grouped = seriesList.groupBy { it.categoryName ?: "Uncategorized" }
                        _uiState.update {
                            it.copy(seriesByCategory = grouped, isLoading = false)
                        }
                    }
                }
        }
    }
}

data class SeriesUiState(
    val seriesByCategory: Map<String, List<Series>> = emptyMap(),
    val isLoading: Boolean = true
)

@Composable
fun SeriesScreen(
    onSeriesClick: (Long) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopNavBar(currentRoute = currentRoute, onNavigate = onNavigate)

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading series...", style = MaterialTheme.typography.bodyLarge, color = OnSurface)
            }
        } else if (uiState.seriesByCategory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📺", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("No series found", style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = uiState.seriesByCategory.entries.toList(),
                    key = { it.key }
                ) { (categoryName, seriesList) ->
                    CategoryRow(title = categoryName, items = seriesList) { series ->
                        SeriesCard(
                            series = series,
                            onClick = {
                                onSeriesClick(series.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
