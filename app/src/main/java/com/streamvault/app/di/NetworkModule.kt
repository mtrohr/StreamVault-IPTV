package com.streamvault.app.di

import com.streamvault.data.remote.xtream.XtreamApiService
import com.streamvault.data.parser.XmltvParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    @Provides
    @Singleton
    fun provideXtreamApiService(okHttpClient: OkHttpClient): XtreamApiService =
        Retrofit.Builder()
            // Base URL will be overridden per-request by the provider
            .baseUrl("https://placeholder.invalid/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(XtreamApiService::class.java)

    @Provides
    @Singleton
    fun provideXmltvParser(): XmltvParser = XmltvParser()
}
