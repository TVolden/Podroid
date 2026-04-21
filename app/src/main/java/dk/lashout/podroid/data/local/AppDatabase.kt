package dk.lashout.podroid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dk.lashout.podroid.data.local.dao.EpisodeDao
import dk.lashout.podroid.data.local.dao.PlaylistDao
import dk.lashout.podroid.data.local.dao.PlaylistEpisodeDao
import dk.lashout.podroid.data.local.dao.PodcastDao
import dk.lashout.podroid.data.local.entity.EpisodeEntity
import dk.lashout.podroid.data.local.entity.PlaylistEntity
import dk.lashout.podroid.data.local.entity.PlaylistEpisodeEntity
import dk.lashout.podroid.data.local.entity.PodcastEntity

@Database(
    entities = [
        PodcastEntity::class,
        EpisodeEntity::class,
        PlaylistEntity::class,
        PlaylistEpisodeEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistEpisodeDao(): PlaylistEpisodeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE episodes ADD COLUMN playedAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlists (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        isTemporary INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_episodes (
                        id TEXT PRIMARY KEY NOT NULL,
                        playlistId TEXT NOT NULL,
                        episodeId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        isPlayedInPlaylist INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_episodes_playlistId ON playlist_episodes(playlistId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_episodes_episodeId ON playlist_episodes(episodeId)")

                // Migrate existing playlist_items into a named playlist
                val now = System.currentTimeMillis()
                database.execSQL("""
                    INSERT OR IGNORE INTO playlists VALUES (
                        'migrated_playlist', 'My Playlist', 0, $now
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO playlist_episodes (id, playlistId, episodeId, position, addedAt, isPlayedInPlaylist)
                    SELECT id, 'migrated_playlist', episodeId, position, addedAt, 0
                    FROM playlist_items
                """.trimIndent())
                database.execSQL("DROP TABLE IF EXISTS playlist_items")
            }
        }
    }
}
