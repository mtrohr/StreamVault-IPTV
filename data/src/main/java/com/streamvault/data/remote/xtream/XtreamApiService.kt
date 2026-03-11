package com.streamvault.data.remote.xtream

import com.streamvault.data.remote.dto.XtreamAuthResponse
import com.streamvault.data.remote.dto.XtreamCategory
import com.streamvault.data.remote.dto.XtreamEpgResponse
import com.streamvault.data.remote.dto.XtreamSeriesInfoResponse
import com.streamvault.data.remote.dto.XtreamSeriesItem
import com.streamvault.data.remote.dto.XtreamStream
import com.streamvault.data.remote.dto.XtreamVodInfoResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit interface for Xtream Codes player API.
 *
 * Calls use dynamic @Url so each provider can target its own server host.
 */
interface XtreamApiService {

    @GET
    suspend fun authenticate(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): XtreamAuthResponse

    @GET
    suspend fun getLiveCategories(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getLiveStreams(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamStream>

    @GET
    suspend fun getVodCategories(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getVodStreams(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamStream>

    @GET
    suspend fun getVodInfo(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") vodId: Long
    ): XtreamVodInfoResponse

    @GET
    suspend fun getSeriesCategories(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getSeriesList(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamSeriesItem>

    @GET
    suspend fun getSeriesInfo(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Long
    ): XtreamSeriesInfoResponse

    @GET
    suspend fun getShortEpg(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_short_epg",
        @Query("stream_id") streamId: Long,
        @Query("limit") limit: Int = 4
    ): XtreamEpgResponse

    @GET
    suspend fun getFullEpg(
        @Url endpoint: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_simple_data_table",
        @Query("stream_id") streamId: Long
    ): XtreamEpgResponse
}
