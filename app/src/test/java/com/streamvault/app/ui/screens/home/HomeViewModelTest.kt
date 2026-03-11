package com.streamvault.app.ui.screens.home

import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.domain.manager.ParentalControlManager
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Provider
import com.streamvault.domain.repository.*
import com.streamvault.domain.usecase.GetCustomCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val providerRepository: ProviderRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val categoryRepository: CategoryRepository = mock()
    private val favoriteRepository: FavoriteRepository = mock()
    private val preferencesRepository: PreferencesRepository = mock()
    private val epgRepository: EpgRepository = mock()
    private val getCustomCategories: GetCustomCategories = mock()
    private val parentalControlManager: ParentalControlManager = mock()

    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock default flows to prevent exceptions during init
        whenever(providerRepository.getProviders()).thenReturn(flowOf(emptyList()))
        whenever(providerRepository.getActiveProvider()).thenReturn(flowOf(null))
        whenever(preferencesRepository.parentalControlLevel).thenReturn(flowOf(0))
        whenever(favoriteRepository.getFavorites(any())).thenReturn(flowOf(emptyList()))
        whenever(preferencesRepository.defaultCategoryId).thenReturn(flowOf(null))

        viewModel = HomeViewModel(
            providerRepository,
            channelRepository,
            categoryRepository,
            favoriteRepository,
            preferencesRepository,
            epgRepository,
            getCustomCategories,
            parentalControlManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when switchProvider is called, it delegates to repository`() = runTest {
        viewModel.switchProvider(1L)
        runCurrent()
        verify(providerRepository).setActiveProvider(1L)
    }

    @Test
    fun `initial state has empty categories and is loading`() = runTest {
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isTrue()
        assertThat(state.categories).isEmpty()
        assertThat(state.filteredChannels).isEmpty()
    }

    @Test
    fun `updateCategorySearchQuery updates state`() = runTest {
        viewModel.updateCategorySearchQuery("News")
        assertThat(viewModel.uiState.value.categorySearchQuery).isEqualTo("News")
    }

    @Test
    fun `updateChannelSearchQuery updates state and triggers filtering`() = runTest {
        viewModel.updateChannelSearchQuery("CNN")
        assertThat(viewModel.uiState.value.channelSearchQuery).isEqualTo("CNN")
    }

    @Test
    fun `selectCategory sets selected category and triggers loading`() = runTest {
        val category = Category(id = 1L, name = "Sports", parentId = null)
        
        // Mock the repositories needed for loading channels
        whenever(channelRepository.getChannelsByCategory(any(), any())).thenReturn(flowOf(emptyList()))
        val provider = Provider(id = 1L, name = "Provider", type = com.streamvault.domain.model.ProviderType.M3U, serverUrl = "http://test")
        whenever(providerRepository.getActiveProvider()).thenReturn(flowOf(provider))

        viewModel.selectCategory(category)
        
        val state = viewModel.uiState.value
        assertThat(state.selectedCategory).isEqualTo(category)
        verify(parentalControlManager).clearUnlockedCategories(anyOrNull())
    }
}
