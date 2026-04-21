package dk.lashout.podroid.domain.repository

import dk.lashout.podroid.domain.model.Podcast
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    suspend fun searchPodcasts(query: String): List<Podcast>
    fun getSubscriptions(): Flow<List<Podcast>>
    suspend fun getPodcastById(id: String): Podcast?
    suspend fun subscribe(podcast: Podcast)
    suspend fun unsubscribe(podcastId: String)
    suspend fun upsertPodcast(podcast: Podcast)
    suspend fun importFromFeedUrl(feedUrl: String): Podcast
}
