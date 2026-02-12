package com.streamvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.streamvault.data.local.dao.*
import com.streamvault.data.local.entity.*

@Database(
    entities = [
        ProviderEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        CategoryEntity::class,
        ProgramEntity::class,
        FavoriteEntity::class,
        VirtualGroupEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class StreamVaultDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun channelDao(): ChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun categoryDao(): CategoryDao
    abstract fun programDao(): ProgramDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun virtualGroupDao(): VirtualGroupDao
}
