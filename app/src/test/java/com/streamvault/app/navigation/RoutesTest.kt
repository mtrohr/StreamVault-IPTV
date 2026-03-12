package com.streamvault.app.navigation

import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Episode
import com.streamvault.domain.model.Movie
import com.streamvault.domain.repository.ChannelRepository
import org.junit.Test

class RoutesTest {

    @Test
    fun `liveTv route supports category deep links`() {
        assertThat(Routes.liveTv()).isEqualTo(Routes.LIVE_TV)
        assertThat(Routes.liveTv(42L)).isEqualTo("${Routes.LIVE_TV}?categoryId=42")
    }

    @Test
    fun `livePlayer preserves playback context`() {
        val route = Routes.livePlayer(
            channel = Channel(
                id = 42L,
                name = "News HD",
                streamUrl = "https://example.com/live.m3u8",
                epgChannelId = "news.hd",
                categoryId = 9L,
                providerId = 7L
            ),
            categoryId = 9L,
            providerId = 7L,
            isVirtual = false
        )

        assertThat(route).contains("internalId=42")
        assertThat(route).contains("categoryId=9")
        assertThat(route).contains("providerId=7")
        assertThat(route).contains("channelId=news.hd")
        assertThat(route).contains("contentType=LIVE")
        assertThat(route).contains("isVirtual=false")
    }

    @Test
    fun `livePlayer falls back to all channels when category is missing`() {
        val route = Routes.livePlayer(
            channel = Channel(
                id = 8L,
                name = "Sports",
                streamUrl = "https://example.com/sports.m3u8",
                providerId = 3L
            ),
            categoryId = null,
            providerId = 3L
        )

        assertThat(route).contains("categoryId=${ChannelRepository.ALL_CHANNELS_ID}")
        assertThat(route).contains("providerId=3")
    }

    @Test
    fun `moviePlayer preserves provider and category context`() {
        val route = Routes.moviePlayer(
            Movie(
                id = 15L,
                name = "Film",
                streamUrl = "https://example.com/movie.mp4",
                categoryId = 21L,
                providerId = 5L
            )
        )

        assertThat(route).contains("internalId=15")
        assertThat(route).contains("categoryId=21")
        assertThat(route).contains("providerId=5")
        assertThat(route).contains("contentType=MOVIE")
    }

    @Test
    fun `episodePlayer preserves episode playback context`() {
        val route = Routes.episodePlayer(
            Episode(
                id = 33L,
                title = "Pilot",
                episodeNumber = 1,
                seasonNumber = 1,
                streamUrl = "https://example.com/episode.mp4",
                providerId = 11L
            )
        )

        assertThat(route).contains("internalId=33")
        assertThat(route).contains("providerId=11")
        assertThat(route).contains("contentType=SERIES_EPISODE")
    }

    @Test
    fun `player route supports archive playback context`() {
        val route = Routes.player(
            streamUrl = "https://example.com/live.m3u8",
            title = "News HD",
            internalId = 42L,
            providerId = 7L,
            contentType = "LIVE",
            archiveStartMs = 1_700_000_000_000L,
            archiveEndMs = 1_700_000_360_000L,
            archiveTitle = "News HD: Morning Show"
        )

        assertThat(route).contains("archiveStartMs=1700000000000")
        assertThat(route).contains("archiveEndMs=1700000360000")
        assertThat(route).contains("archiveTitle=News+HD%3A+Morning+Show")
    }

    @Test
    fun `epg route preserves guide context`() {
        val route = Routes.epg(
            categoryId = 21L,
            anchorTime = 1_700_000_000_000L,
            favoritesOnly = true
        )

        assertThat(route).isEqualTo(
            "epg?categoryId=21&anchorTime=1700000000000&favoritesOnly=true"
        )
    }

    @Test
    fun `player route preserves return route back to guide`() {
        val returnRoute = Routes.epg(
            categoryId = 9L,
            anchorTime = 1_700_000_360_000L,
            favoritesOnly = false
        )
        val route = Routes.livePlayer(
            channel = Channel(
                id = 42L,
                name = "News HD",
                streamUrl = "https://example.com/live.m3u8",
                epgChannelId = "news.hd",
                categoryId = 9L,
                providerId = 7L
            ),
            categoryId = 9L,
            providerId = 7L,
            returnRoute = returnRoute
        )

        assertThat(route).contains(
            "returnRoute=epg%3FcategoryId%3D9%26anchorTime%3D1700000360000%26favoritesOnly%3Dfalse"
        )
    }
}
