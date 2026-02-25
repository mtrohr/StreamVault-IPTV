package com.streamvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        VirtualGroupEntity::class,
        PlaybackHistoryEntity::class,
        SyncMetadataEntity::class
    ],
    version = 5,
    exportSchema = true   // ← was false; schema JSON now tracked in version control
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
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        /**
         * Migration 2 → 3: added parental-control protection columns.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create playback_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playback_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content_id INTEGER NOT NULL,
                        content_type TEXT NOT NULL,
                        provider_id INTEGER NOT NULL,
                        title TEXT NOT NULL DEFAULT '',
                        poster_url TEXT,
                        stream_url TEXT NOT NULL DEFAULT '',
                        resume_position_ms INTEGER NOT NULL DEFAULT 0,
                        total_duration_ms INTEGER NOT NULL DEFAULT 0,
                        last_watched_at INTEGER NOT NULL DEFAULT 0,
                        watch_count INTEGER NOT NULL DEFAULT 1,
                        series_id INTEGER,
                        season_number INTEGER,
                        episode_number INTEGER
                    )
                """)
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_history_unique ON playback_history(content_id, content_type, provider_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_history_last_watched ON playback_history(last_watched_at DESC)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_history_provider ON playback_history(provider_id)")
                
                // 2. Create sync_metadata table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_metadata (
                        provider_id INTEGER PRIMARY KEY NOT NULL,
                        last_live_sync INTEGER NOT NULL DEFAULT 0,
                        last_movie_sync INTEGER NOT NULL DEFAULT 0,
                        last_series_sync INTEGER NOT NULL DEFAULT 0,
                        last_epg_sync INTEGER NOT NULL DEFAULT 0,
                        live_count INTEGER NOT NULL DEFAULT 0,
                        movie_count INTEGER NOT NULL DEFAULT 0,
                        series_count INTEGER NOT NULL DEFAULT 0,
                        epg_count INTEGER NOT NULL DEFAULT 0,
                        last_sync_status TEXT NOT NULL DEFAULT 'NONE'
                    )
                """)
                
                // 3. Migrate existing watch progress to history
                database.execSQL("""
                    INSERT OR IGNORE INTO playback_history (content_id, content_type, provider_id, title, resume_position_ms, last_watched_at)
                    SELECT id, 'MOVIE', provider_id, name, watch_progress, last_watched_at
                    FROM movies WHERE watch_progress > 0
                """)
                database.execSQL("""
                    INSERT OR IGNORE INTO playback_history (content_id, content_type, provider_id, title, resume_position_ms, last_watched_at, series_id, season_number, episode_number)
                    SELECT id, 'SERIES_EPISODE', provider_id, title, watch_progress, last_watched_at, series_id, season_number, episode_number
                    FROM episodes WHERE watch_progress > 0
                """)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Note: Room auto-generates index names as 'index_tableName_columnNames'
                // Channels
                database.execSQL("DROP INDEX IF EXISTS index_channels_category_id")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_channels_provider_id_category_id ON channels(provider_id, category_id)")
                
                // Movies
                database.execSQL("DROP INDEX IF EXISTS index_movies_category_id")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_movies_provider_id_category_id ON movies(provider_id, category_id)")
                
                // Series
                database.execSQL("DROP INDEX IF EXISTS index_series_category_id")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_series_provider_id_category_id ON series(provider_id, category_id)")
                
                // Favorites
                database.execSQL("DROP INDEX IF EXISTS index_favorites_group_id")
                database.execSQL("DROP INDEX IF EXISTS index_favorites_position")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorites_content_type_group_id ON favorites(content_type, group_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_favorites_group_id_position ON favorites(group_id, position)")
            }
        }
    }
}
