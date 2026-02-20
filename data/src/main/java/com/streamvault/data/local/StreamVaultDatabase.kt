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
    version = 3,
    exportSchema = false
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

    companion object {
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE categories ADD COLUMN is_user_protected INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE channels ADD COLUMN is_user_protected INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE movies ADD COLUMN is_adult INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE movies ADD COLUMN is_user_protected INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE series ADD COLUMN is_adult INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE series ADD COLUMN is_user_protected INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE episodes ADD COLUMN is_adult INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE episodes ADD COLUMN is_user_protected INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
