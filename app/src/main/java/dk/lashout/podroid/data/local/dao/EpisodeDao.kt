package dk.lashout.podroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dk.lashout.podroid.data.local.entity.EpisodeEntity
import dk.lashout.podroid.data.local.entity.EpisodeWithPodcast
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNewEpisodes(episodes: List<EpisodeEntity>): List<Long>

    @Query("""
        UPDATE episodes
        SET title = :title, description = :description, audioUrl = :audioUrl,
            durationSeconds = :durationSeconds, publishedAt = :publishedAt
        WHERE id = :id
    """)
    suspend fun updateRssMeta(
        id: String, title: String, description: String,
        audioUrl: String, durationSeconds: Long, publishedAt: Long
    )

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishedAt DESC")
    fun getByPodcastId(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("""
        SELECT e.id, e.podcastId, e.title, e.description, e.audioUrl,
               e.durationSeconds, e.publishedAt, e.isPlayed, e.playbackPositionMs, e.playedAt,
               p.title AS podcast_title, p.artworkUrl AS podcast_artwork_url
        FROM episodes e
        INNER JOIN podcasts p ON e.podcastId = p.id
        WHERE p.isSubscribed = 1
        ORDER BY e.publishedAt DESC
        LIMIT 100
    """)
    fun getRecentFromSubscriptionsWithPodcast(): Flow<List<EpisodeWithPodcast>>

    @Query("""
        SELECT e.id, e.podcastId, e.title, e.description, e.audioUrl,
               e.durationSeconds, e.publishedAt, e.isPlayed, e.playbackPositionMs, e.playedAt,
               p.title AS podcast_title, p.artworkUrl AS podcast_artwork_url
        FROM episodes e
        INNER JOIN podcasts p ON e.podcastId = p.id
        WHERE e.isPlayed = 1
        ORDER BY e.playedAt DESC
        LIMIT 200
    """)
    fun getPlayedEpisodesWithPodcast(): Flow<List<EpisodeWithPodcast>>

    @Query("""
        SELECT e.id, e.podcastId, e.title, e.description, e.audioUrl,
               e.durationSeconds, e.publishedAt, e.isPlayed, e.playbackPositionMs, e.playedAt,
               p.title AS podcast_title, p.artworkUrl AS podcast_artwork_url
        FROM episodes e
        INNER JOIN podcasts p ON e.podcastId = p.id
        WHERE e.id = :episodeId
        LIMIT 1
    """)
    suspend fun getByIdWithPodcast(episodeId: String): EpisodeWithPodcast?

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    suspend fun getById(episodeId: String): EpisodeEntity?

    @Query("UPDATE episodes SET playbackPositionMs = :positionMs WHERE id = :episodeId")
    suspend fun updatePlaybackPosition(episodeId: String, positionMs: Long): Int

    @Query("UPDATE episodes SET isPlayed = 1, playedAt = :playedAt WHERE id = :episodeId")
    suspend fun markAsPlayed(episodeId: String, playedAt: Long): Int

    @Query("UPDATE episodes SET isPlayed = 0, playedAt = 0 WHERE id = :episodeId")
    suspend fun markAsUnplayed(episodeId: String): Int

    // Autoplay: next episode in same podcast for NEWER_FIRST order (play older episodes after newer ones)
    @Query("""
        SELECT * FROM episodes
        WHERE podcastId = :podcastId AND isPlayed = 0 AND publishedAt < :currentPublishedAt
        ORDER BY publishedAt DESC LIMIT 1
    """)
    suspend fun getNextEpisodeNewerFirst(podcastId: String, currentPublishedAt: Long): EpisodeEntity?

    // Autoplay: next episode in same podcast for OLDER_FIRST order (play newer episodes after older ones)
    @Query("""
        SELECT * FROM episodes
        WHERE podcastId = :podcastId AND isPlayed = 0 AND publishedAt > :currentPublishedAt
        ORDER BY publishedAt ASC LIMIT 1
    """)
    suspend fun getNextEpisodeOlderFirst(podcastId: String, currentPublishedAt: Long): EpisodeEntity?

    // Keep the old simple Flow for the playlist repo's workaround
    @Query("""
        SELECT e.* FROM episodes e
        INNER JOIN podcasts p ON e.podcastId = p.id
        WHERE p.isSubscribed = 1
        ORDER BY e.publishedAt DESC
        LIMIT 100
    """)
    fun getRecentFromSubscriptions(): Flow<List<EpisodeEntity>>
}
