package com.streamvault.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Episode
import com.streamvault.domain.model.Movie
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.app.ui.screens.dashboard.DashboardScreen
import com.streamvault.app.ui.screens.favorites.FavoritesScreen
import com.streamvault.app.ui.screens.favorites.FavoriteUiModel
import com.streamvault.app.ui.screens.multiview.MultiViewScreen
import com.streamvault.app.ui.screens.home.HomeScreen
import com.streamvault.app.ui.screens.movies.MoviesScreen
import com.streamvault.app.ui.screens.player.PlayerScreen
import com.streamvault.app.ui.screens.provider.ProviderSetupScreen
import com.streamvault.app.ui.screens.series.SeriesScreen
import com.streamvault.app.ui.screens.settings.SettingsScreen
import com.streamvault.app.ui.screens.welcome.WelcomeScreen


object Routes {
    const val PROVIDER_SETUP = "provider_setup?providerId={providerId}"
    const val HOME = "home"
    const val LIVE_TV = "live_tv"
    const val LIVE_TV_DESTINATION = "live_tv?categoryId={categoryId}"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val FAVORITES = "favorites"
    const val EPG = "epg"
    const val EPG_DESTINATION = "epg?categoryId={categoryId}&anchorTime={anchorTime}&favoritesOnly={favoritesOnly}"
    const val SETTINGS = "settings"
    const val PLAYER = "player/{streamUrl}?title={title}&channelId={channelId}&internalId={internalId}&categoryId={categoryId}&providerId={providerId}&isVirtual={isVirtual}&contentType={contentType}&archiveStartMs={archiveStartMs}&archiveEndMs={archiveEndMs}&archiveTitle={archiveTitle}&returnRoute={returnRoute}"
    const val SEARCH = "search"
    const val SERIES_DETAIL = "series_detail/{seriesId}"
    const val WELCOME = "welcome"
    const val PARENTAL_CONTROL_GROUPS = "parental_control_groups/{providerId}"
    const val MULTI_VIEW = "multi_view"


    fun providerSetup(providerId: Long? = null) = "provider_setup?providerId=${providerId ?: -1L}"
    fun liveTv(categoryId: Long? = null) = if (categoryId == null) LIVE_TV else "$LIVE_TV?categoryId=$categoryId"
    fun epg(categoryId: Long? = null, anchorTime: Long? = null, favoritesOnly: Boolean? = null): String {
        val resolvedCategoryId = categoryId ?: -1L
        val resolvedAnchorTime = anchorTime ?: -1L
        val resolvedFavoritesOnly = favoritesOnly ?: false
        return "$EPG?categoryId=$resolvedCategoryId&anchorTime=$resolvedAnchorTime&favoritesOnly=$resolvedFavoritesOnly"
    }

    fun livePlayer(
        channel: Channel,
        categoryId: Long? = channel.categoryId,
        providerId: Long? = channel.providerId,
        isVirtual: Boolean = false,
        returnRoute: String? = null
    ): String {
        val effectiveCategoryId = categoryId ?: ChannelRepository.ALL_CHANNELS_ID
        return player(
            streamUrl = channel.streamUrl,
            title = channel.name,
            channelId = channel.epgChannelId,
            internalId = channel.id,
            categoryId = effectiveCategoryId,
            providerId = providerId,
            isVirtual = isVirtual,
            contentType = "LIVE",
            returnRoute = returnRoute
        )
    }

    fun moviePlayer(movie: Movie): String {
        return player(
            streamUrl = movie.streamUrl,
            title = movie.name,
            internalId = movie.id,
            categoryId = movie.categoryId,
            providerId = movie.providerId,
            contentType = "MOVIE"
        )
    }

    fun episodePlayer(episode: Episode): String {
        return player(
            streamUrl = episode.streamUrl,
            title = "${episode.title} - S${episode.seasonNumber}E${episode.episodeNumber}",
            internalId = episode.id,
            providerId = episode.providerId,
            contentType = "SERIES_EPISODE"
        )
    }

    fun player(
        streamUrl: String, 
        title: String, 
        channelId: String? = null,
        internalId: Long = -1L,
        categoryId: Long? = null,
        providerId: Long? = null,
        isVirtual: Boolean = false,
        contentType: String = "LIVE",
        archiveStartMs: Long? = null,
        archiveEndMs: Long? = null,
        archiveTitle: String? = null,
        returnRoute: String? = null
    ): String {
        val encodedUrl = java.net.URLEncoder.encode(streamUrl, "UTF-8")
        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
        val encodedArchiveTitle = java.net.URLEncoder.encode(archiveTitle ?: "", "UTF-8")
        val encodedReturnRoute = java.net.URLEncoder.encode(returnRoute ?: "", "UTF-8")
        return "player/$encodedUrl?title=$encodedTitle&channelId=${channelId ?: ""}&internalId=$internalId&categoryId=${categoryId ?: -1L}&providerId=${providerId ?: -1L}&isVirtual=$isVirtual&contentType=$contentType&archiveStartMs=${archiveStartMs ?: -1L}&archiveEndMs=${archiveEndMs ?: -1L}&archiveTitle=$encodedArchiveTitle&returnRoute=$encodedReturnRoute"
    }

    fun seriesDetail(seriesId: Long) = "series_detail/$seriesId"
    fun parentalControlGroups(providerId: Long) = "parental_control_groups/$providerId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onNavigateToSetup = {
                    navController.navigate(Routes.PROVIDER_SETUP) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.PROVIDER_SETUP,
            arguments = listOf(
                navArgument("providerId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getLong("providerId")?.takeIf { it != -1L }
            
            ProviderSetupScreen(
                editProviderId = providerId,
                onProviderAdded = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PROVIDER_SETUP) { inclusive = true }
                    }
                }
            )
        }
// ...

        composable(Routes.HOME) {
            DashboardScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAddProvider = {
                    navController.navigate(Routes.providerSetup(null))
                },
                onChannelClick = { channel ->
                    navController.navigate(
                        Routes.livePlayer(
                            channel = channel,
                            categoryId = channel.categoryId ?: ChannelRepository.ALL_CHANNELS_ID,
                            providerId = channel.providerId,
                            isVirtual = false
                        )
                    )
                },
                onMovieClick = { movie ->
                    navController.navigate(Routes.moviePlayer(movie))
                },
                onSeriesClick = { series ->
                    navController.navigate(Routes.seriesDetail(series.id))
                },
                onPlaybackHistoryClick = { history ->
                    val route = when (history.contentType) {
                        com.streamvault.domain.model.ContentType.LIVE -> {
                            Routes.player(
                                streamUrl = history.streamUrl,
                                title = history.title,
                                internalId = history.contentId,
                                providerId = history.providerId,
                                contentType = history.contentType.name
                            )
                        }
                        com.streamvault.domain.model.ContentType.MOVIE -> {
                            Routes.player(
                                streamUrl = history.streamUrl,
                                title = history.title,
                                internalId = history.contentId,
                                providerId = history.providerId,
                                contentType = history.contentType.name
                            )
                        }
                        com.streamvault.domain.model.ContentType.SERIES -> {
                            Routes.seriesDetail(history.seriesId ?: history.contentId)
                        }
                        com.streamvault.domain.model.ContentType.SERIES_EPISODE -> {
                            Routes.player(
                                streamUrl = history.streamUrl,
                                title = history.title,
                                internalId = history.contentId,
                                providerId = history.providerId,
                                contentType = history.contentType.name
                            )
                        }
                    }
                    navController.navigate(route)
                },
                currentRoute = Routes.HOME
            )
        }

        composable(
            route = Routes.LIVE_TV_DESTINATION,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val initialCategoryId = backStackEntry.arguments?.getLong("categoryId")?.takeIf { it != -1L }
            HomeScreen(
                onChannelClick = { channel, category, provider ->
                    navController.navigate(
                        Routes.livePlayer(
                            channel = channel,
                            categoryId = category?.id,
                            providerId = provider?.id,
                            isVirtual = category?.isVirtual == true
                        )
                    )
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                currentRoute = Routes.LIVE_TV,
                initialCategoryId = initialCategoryId
            )
        }
// ... (rest of file)

        composable(Routes.MOVIES) {
            MoviesScreen(
                onMovieClick = { movie ->
                    navController.navigate(Routes.moviePlayer(movie))
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                currentRoute = Routes.MOVIES
            )
        }

        composable(Routes.SERIES) {
            SeriesScreen(
                onSeriesClick = { seriesId ->
                    navController.navigate(Routes.seriesDetail(seriesId))
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                currentRoute = Routes.SERIES
            )
        }

        composable(Routes.FAVORITES) {
            FavoritesScreen(
                onItemClick = { item ->
                    val route = when (item.favorite.contentType) {
                        com.streamvault.domain.model.ContentType.LIVE -> {
                            Routes.player(
                                streamUrl = item.streamUrl,
                                title = item.title,
                                channelId = item.epgChannelId,
                                internalId = item.favorite.contentId,
                                categoryId = item.launchCategoryId,
                                providerId = item.providerId,
                                isVirtual = item.launchIsVirtual,
                                contentType = item.favorite.contentType.name
                            )
                        }
                        com.streamvault.domain.model.ContentType.MOVIE -> {
                            Routes.player(
                                streamUrl = item.streamUrl,
                                title = item.title,
                                internalId = item.favorite.contentId,
                                categoryId = item.categoryId,
                                providerId = item.providerId,
                                contentType = item.favorite.contentType.name
                            )
                        }
                        else -> Routes.seriesDetail(item.favorite.contentId)
                    }
                    navController.navigate(route)
                },
                onHistoryClick = { item ->
                    val route = when (item.history.contentType) {
                        com.streamvault.domain.model.ContentType.LIVE -> {
                            Routes.player(
                                streamUrl = item.history.streamUrl,
                                title = item.title,
                                channelId = item.epgChannelId,
                                internalId = item.history.contentId,
                                categoryId = item.categoryId,
                                providerId = item.providerId,
                                isVirtual = item.launchIsVirtual,
                                contentType = item.history.contentType.name
                            )
                        }
                        com.streamvault.domain.model.ContentType.MOVIE -> {
                            Routes.player(
                                streamUrl = item.history.streamUrl,
                                title = item.title,
                                internalId = item.history.contentId,
                                providerId = item.providerId,
                                contentType = item.history.contentType.name
                            )
                        }
                        com.streamvault.domain.model.ContentType.SERIES -> {
                            Routes.seriesDetail(item.history.seriesId ?: item.history.contentId)
                        }
                        com.streamvault.domain.model.ContentType.SERIES_EPISODE -> {
                            Routes.player(
                                streamUrl = item.history.streamUrl,
                                title = item.title,
                                internalId = item.history.contentId,
                                providerId = item.providerId,
                                contentType = item.history.contentType.name
                            )
                        }
                    }
                    navController.navigate(route)
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                currentRoute = Routes.FAVORITES
            )
        }

        composable(
            route = Routes.EPG_DESTINATION,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("anchorTime") { type = NavType.LongType; defaultValue = -1L },
                navArgument("favoritesOnly") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val epgCategoryId = backStackEntry.arguments?.getLong("categoryId")?.takeIf { it != -1L }
            val epgAnchorTime = backStackEntry.arguments?.getLong("anchorTime")?.takeIf { it != -1L }
            val epgFavoritesOnly = backStackEntry.arguments?.getBoolean("favoritesOnly") ?: false
            com.streamvault.app.ui.screens.epg.FullEpgScreen(
                currentRoute = Routes.EPG,
                initialCategoryId = epgCategoryId,
                initialAnchorTime = epgAnchorTime,
                initialFavoritesOnly = epgFavoritesOnly,
                onPlayChannel = { channel, returnRoute ->
                    navController.navigate(
                        Routes.livePlayer(
                            channel = channel,
                            categoryId = channel.categoryId ?: ChannelRepository.ALL_CHANNELS_ID,
                            providerId = channel.providerId,
                            isVirtual = false,
                            returnRoute = returnRoute
                        )
                    )
                },
                onPlayArchive = { channel, program, returnRoute ->
                    navController.navigate(
                        Routes.player(
                            streamUrl = channel.streamUrl,
                            title = channel.name,
                            channelId = channel.epgChannelId,
                            internalId = channel.id,
                            categoryId = channel.categoryId ?: ChannelRepository.ALL_CHANNELS_ID,
                            providerId = channel.providerId,
                            isVirtual = false,
                            contentType = "LIVE",
                            archiveStartMs = program.startTime,
                            archiveEndMs = program.endTime,
                            archiveTitle = "${channel.name}: ${program.title}",
                            returnRoute = returnRoute
                        )
                    )
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAddProvider = {
                    navController.navigate(Routes.providerSetup(null))
                },
                onEditProvider = { provider ->
                    navController.navigate(Routes.providerSetup(provider.id))
                },
                onNavigateToParentalControl = { providerId ->
                    navController.navigate(Routes.parentalControlGroups(providerId))
                },
                currentRoute = Routes.SETTINGS
            )
        }

        composable(
            route = Routes.PARENTAL_CONTROL_GROUPS,
            arguments = listOf(
                navArgument("providerId") { type = NavType.LongType }
            )
        ) {
            com.streamvault.app.ui.screens.settings.parental.ParentalControlGroupScreen(
                currentRoute = Routes.SETTINGS,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SEARCH) {
            com.streamvault.app.ui.screens.search.SearchScreen(
                onChannelClick = { channel ->
                    navController.navigate(
                        Routes.livePlayer(
                            channel = channel,
                            categoryId = channel.categoryId ?: ChannelRepository.ALL_CHANNELS_ID,
                            providerId = channel.providerId,
                            isVirtual = false
                        )
                    )
                },
                onMovieClick = { movie ->
                     navController.navigate(Routes.moviePlayer(movie))
                },
                onSeriesClick = { series ->
                     navController.navigate(Routes.seriesDetail(series.id))
                }
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("streamUrl") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("channelId") { type = NavType.StringType; defaultValue = "" },
                navArgument("internalId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("providerId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("isVirtual") { type = NavType.BoolType; defaultValue = false },
                navArgument("contentType") { type = NavType.StringType; defaultValue = "LIVE" },
                navArgument("archiveStartMs") { type = NavType.LongType; defaultValue = -1L },
                navArgument("archiveEndMs") { type = NavType.LongType; defaultValue = -1L },
                navArgument("archiveTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("returnRoute") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val channelId = backStackEntry.arguments?.getString("channelId")?.takeIf { it.isNotBlank() }
            val internalId = backStackEntry.arguments?.getLong("internalId") ?: -1L
            val categoryId = backStackEntry.arguments?.getLong("categoryId")?.takeIf { it != -1L }
            val providerId = backStackEntry.arguments?.getLong("providerId")?.takeIf { it != -1L }
            val isVirtual = backStackEntry.arguments?.getBoolean("isVirtual") ?: false
            val contentType = backStackEntry.arguments?.getString("contentType") ?: "LIVE"
            val archiveStartMs = backStackEntry.arguments?.getLong("archiveStartMs")?.takeIf { it != -1L }
            val archiveEndMs = backStackEntry.arguments?.getLong("archiveEndMs")?.takeIf { it != -1L }
            val archiveTitle = backStackEntry.arguments?.getString("archiveTitle")?.takeIf { it.isNotBlank() }
            val returnRoute = backStackEntry.arguments?.getString("returnRoute")?.takeIf { it.isNotBlank() }
            
            PlayerScreen(
                streamUrl = streamUrl,
                title = title,
                epgChannelId = channelId,
                internalChannelId = internalId,
                categoryId = categoryId,
                providerId = providerId,
                isVirtual = isVirtual,
                contentType = contentType,
                archiveStartMs = archiveStartMs,
                archiveEndMs = archiveEndMs,
                archiveTitle = archiveTitle,
                returnRoute = returnRoute,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = Routes.SERIES_DETAIL,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.LongType }
            )
        ) {
            com.streamvault.app.ui.screens.series.SeriesDetailScreen(
                onEpisodeClick = { episode ->
                     navController.navigate(Routes.episodePlayer(episode))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MULTI_VIEW) {
            MultiViewScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
