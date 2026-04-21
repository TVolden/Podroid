package dk.lashout.podroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dk.lashout.podroid.data.local.entity.PlaylistEpisodeEntity
import dk.lashout.podroid.data.local.entity.PlaylistEntryProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistEpisodeDao {

    @Query("""
        SELECT pe.id, pe.playlistId, pe.episodeId, pe.position, pe.addedAt, pe.isPlayedInPlaylist,
               e.title         AS ep_title,
               e.podcastId     AS ep_podcastId,
               e.description   AS ep_description,
               e.audioUrl      AS ep_audioUrl,
               e.durationSeconds AS ep_durationSeconds,
               e.publishedAt   AS ep_publishedAt,
               e.isPlayed      AS ep_isPlayed,
               e.playbackPositionMs AS ep_playbackPositionMs,
               p.title         AS pod_title,
               p.artworkUrl    AS pod_artworkUrl
        FROM playlist_episodes pe
        LEFT JOIN episodes e ON pe.episodeId = e.id
        LEFT JOIN podcasts p ON e.podcastId = p.id
        WHERE pe.playlistId = :playlistId
        ORDER BY pe.position ASC
    """)
    fun getEntriesForPlaylist(playlistId: String): Flow<List<PlaylistEntryProjection>>

    @Query("""
        SELECT pe.id, pe.playlistId, pe.episodeId, pe.position, pe.addedAt, pe.isPlayedInPlaylist,
               e.title         AS ep_title,
               e.podcastId     AS ep_podcastId,
               e.description   AS ep_description,
               e.audioUrl      AS ep_audioUrl,
               e.durationSeconds AS ep_durationSeconds,
               e.publishedAt   AS ep_publishedAt,
               e.isPlayed      AS ep_isPlayed,
               e.playbackPositionMs AS ep_playbackPositionMs,
               p.title         AS pod_title,
               p.artworkUrl    AS pod_artworkUrl
        FROM playlist_episodes pe
        LEFT JOIN episodes e ON pe.episodeId = e.id
        LEFT JOIN podcasts p ON e.podcastId = p.id
        WHERE pe.playlistId = :playlistId
        ORDER BY pe.position ASC
    """)
    suspend fun getEntriesForPlaylistSync(playlistId: String): List<PlaylistEntryProjection>

    @Query("SELECT MAX(position) FROM playlist_episodes WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PlaylistEpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PlaylistEpisodeEntity>)

    @Query("DELETE FROM playlist_episodes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM playlist_episodes WHERE playlistId = :playlistId")
    suspend fun deleteAll(playlistId: String)

    @Query("UPDATE playlist_episodes SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)

    @Query("UPDATE playlist_episodes SET isPlayedInPlaylist = 1 WHERE playlistId = :playlistId AND episodeId = :episodeId")
    suspend fun markPlayedInPlaylist(playlistId: String, episodeId: String)
}
