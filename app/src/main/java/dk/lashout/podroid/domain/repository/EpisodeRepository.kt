package dk.lashout.podroid.domain.repository

import dk.lashout.podroid.domain.model.AutoplayOrder
import dk.lashout.podroid.domain.model.Episode
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {
    suspend fun fetchAndStoreEpisodes(podcastId: String, feedUrl: String): List<Episode>
    fun getEpisodesForPodcast(podcastId: String): Flow<List<Episode>>
    fun getRecentEpisodesForSubscriptions(): Flow<List<Episode>>
    fun getPlayedEpisodes(): Flow<List<Episode>>
    suspend fun getEpisodeById(episodeId: String): Episode?
    suspend fun updatePlaybackPosition(episodeId: String, positionMs: Long)
    suspend fun markAsPlayed(episodeId: String)
    suspend fun markAsUnplayed(episodeId: String)
    suspend fun getEpisodeWithPodcast(episodeId: String): Episode?
    suspend fun getNextEpisodeInPodcast(podcastId: String, currentPublishedAt: Long, order: AutoplayOrder): Episode?
}
