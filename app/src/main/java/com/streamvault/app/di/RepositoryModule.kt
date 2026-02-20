package com.streamvault.app.di

import android.content.Context
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.data.repository.*
import com.streamvault.domain.repository.*
import com.streamvault.player.Media3PlayerEngine
import com.streamvault.player.PlayerEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindProviderRepository(impl: ProviderRepositoryImpl): ProviderRepository

    @Binds @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds @Singleton
    abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindPlayerEngine(impl: Media3PlayerEngine): PlayerEngine

    companion object {
        // PreferencesRepository is provided by @Inject constructor

        @Provides
        @Singleton
        fun provideM3uParser(): com.streamvault.data.parser.M3uParser {
            return com.streamvault.data.parser.M3uParser()
        }
    }
}
