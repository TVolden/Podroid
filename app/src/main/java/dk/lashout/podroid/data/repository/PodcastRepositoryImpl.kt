package dk.lashout.podroid.data.repository

import dk.lashout.podroid.data.local.dao.PodcastDao
import dk.lashout.podroid.data.local.entity.PodcastEntity
import dk.lashout.podroid.data.remote.api.ItunesApiService
import dk.lashout.podroid.data.rss.RssParserWrapper
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepositoryImpl @Inject constructor(
    private val itunesApi: ItunesApiService,
    private val podcastDao: PodcastDao,
    private val rssParser: RssParserWrapper
) : PodcastRepository {

    override suspend fun searchPodcasts(query: String): List<Podcast> {
        val response = itunesApi.searchPodcasts(query)
        return response.results
            .filter { it.feedUrl != null }
            .map { result ->
                Podcast(
                    id = result.collectionId.toString(),
                    title = result.collectionName ?: "",
                    author = result.artistName ?: "",
                    description = result.description ?: "",
                    artworkUrl = result.artworkUrl600 ?: result.artworkUrl100 ?: "",
                    feedUrl = result.feedUrl ?: "",
                    isSubscribed = podcastDao.getById(result.collectionId.toString()) != null
                )
            }
    }

    override fun getSubscriptions(): Flow<List<Podcast>> =
        podcastDao.getSubscriptions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPodcastById(id: String): Podcast? =
        podcastDao.getById(id)?.toDomain()

    override suspend fun subscribe(podcast: Podcast) {
        podcastDao.upsert(podcast.toEntity(isSubscribed = true))
    }

    override suspend fun unsubscribe(podcastId: String) {
        podcastDao.unsubscribe(podcastId)
    }

    override suspend fun upsertPodcast(podcast: Podcast) {
        podcastDao.upsert(podcast.toEntity())
    }

    override suspend fun importFromFeedUrl(feedUrl: String): Podcast {
        val normalizedUrl = feedUrl.trim()
        val id = "feed:${normalizedUrl.hashCode()}"
        val existing = podcastDao.getById(id)
        if (existing != null) {
            if (!existing.isSubscribed) podcastDao.subscribe(id)
            return existing.toDomain().copy(isSubscribed = true)
        }
        val info = rssParser.fetchPodcastInfo(normalizedUrl)
        val podcast = Podcast(
            id = id,
            title = info.title,
            author = info.author,
            description = info.description,
            artworkUrl = info.artworkUrl,
            feedUrl = normalizedUrl,
            isSubscribed = true
        )
        podcastDao.upsert(podcast.toEntity(isSubscribed = true))
        return podcast
    }

    private fun PodcastEntity.toDomain() = Podcast(
        id = id,
        title = title,
        author = author,
        description = description,
        artworkUrl = artworkUrl,
        feedUrl = feedUrl,
        isSubscribed = isSubscribed
    )

    private fun Podcast.toEntity(isSubscribed: Boolean = this.isSubscribed) = PodcastEntity(
        id = id,
        title = title,
        author = author,
        description = description,
        artworkUrl = artworkUrl,
        feedUrl = feedUrl,
        isSubscribed = isSubscribed
    )
}
