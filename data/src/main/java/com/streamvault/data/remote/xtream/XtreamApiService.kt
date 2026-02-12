package com.streamvault.data.remote.xtream

import com.streamvault.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the Xtream Codes player API.
 * Base URL format: http://server:port/player_api.php
 *
 * All endpoints require username and password query params.
 */
interface XtreamApiService {

    // ── Authentication ──────────────────────────────────────────────

    @GET("player_api.php")
    suspend fun authenticate(
        @Query("username") username: String,
        @Query("password") password: String
    ): XtreamAuthResponse

    // ── Live TV ─────────────────────────────────────────────────────

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategory>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamStream>

    // ── VOD (Movies) ────────────────────────────────────────────────

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<XtreamCategory>

    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamStream>

    @GET("player_api.php")
    suspend fun getVodInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") vodId: Long
    ): XtreamVodInfoResponse

    // ── Series ──────────────────────────────────────────────────────

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<XtreamCategory>

    @GET("player_api.php")
    suspend fun getSeriesList(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamSeriesItem>

    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Long
    ): XtreamSeriesInfoResponse

    // ── EPG ─────────────────────────────────────────────────────────

    @GET("player_api.php")
    suspend fun getShortEpg(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_short_epg",
        @Query("stream_id") streamId: Long,
        @Query("limit") limit: Int = 4
    ): XtreamEpgResponse

    @GET("player_api.php")
    suspend fun getFullEpg(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_simple_data_table",
        @Query("stream_id") streamId: Long
    ): XtreamEpgResponse
}
