package dk.lashout.podroid.data.repository

import dk.lashout.podroid.data.local.dao.EpisodeDao
import dk.lashout.podroid.data.local.dao.PodcastDao
import dk.lashout.podroid.data.local.entity.EpisodeEntity
import dk.lashout.podroid.data.local.entity.EpisodeWithPodcast
import dk.lashout.podroid.data.rss.RssParserWrapper
import dk.lashout.podroid.domain.model.AutoplayOrder
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val rssParser: RssParserWrapper
) : EpisodeRepository {

    override suspend fun fetchAndStoreEpisodes(podcastId: String, feedUrl: String): List<Episode> {
        val podcast = podcastDao.getById(podcastId)
        val episodes = rssParser.fetchEpisodes(podcastId, feedUrl)
        val insertResults = episodeDao.insertNewEpisodes(episodes.map { it.toEntity() })
        episodes.forEach { ep ->
            episodeDao.updateRssMeta(ep.id, ep.title, ep.description, ep.audioUrl, ep.durationSeconds, ep.publishedAt)
        }
        // Return only genuinely new episodes (insertNewEpisodes returns -1 for pre-existing rows)
        return episodes.zip(insertResults)
            .filter { (_, rowId) -> rowId != -1L }
            .map { (ep, _) -> ep.copy(
                podcastTitle = podcast?.title ?: "",
                podcastArtworkUrl = podcast?.artworkUrl ?: ""
            )}
    }

    override fun getEpisodesForPodcast(podcastId: String): Flow<List<Episode>> =
        episodeDao.getByPodcastId(podcastId).map { entities -> entities.map { it.toDomain() } }

    override fun getRecentEpisodesForSubscriptions(): Flow<List<Episode>> =
        episodeDao.getRecentFromSubscriptionsWithPodcast().map { list -> list.map { it.toDomain() } }

    override fun getPlayedEpisodes(): Flow<List<Episode>> =
        episodeDao.getPlayedEpisodesWithPodcast().map { list -> list.map { it.toDomain() } }

    override suspend fun getEpisodeById(episodeId: String): Episode? =
        episodeDao.getByIdWithPodcast(episodeId)?.toDomain()

    override suspend fun updatePlaybackPosition(episodeId: String, positionMs: Long) {
        episodeDao.updatePlaybackPosition(episodeId, positionMs)
    }

    override suspend fun markAsPlayed(episodeId: String) {
        episodeDao.markAsPlayed(episodeId, System.currentTimeMillis())
    }

    override suspend fun markAsUnplayed(episodeId: String) {
        episodeDao.markAsUnplayed(episodeId)
    }

    override suspend fun getEpisodeWithPodcast(episodeId: String): Episode? =
        episodeDao.getByIdWithPodcast(episodeId)?.toDomain()

    override suspend fun getNextEpisodeInPodcast(
        podcastId: String,
        currentPublishedAt: Long,
        order: AutoplayOrder
    ): Episode? = when (order) {
        AutoplayOrder.NEWER_FIRST ->
            episodeDao.getNextEpisodeNewerFirst(podcastId, currentPublishedAt)?.toDomain()
        AutoplayOrder.OLDER_FIRST ->
            episodeDao.getNextEpisodeOlderFirst(podcastId, currentPublishedAt)?.toDomain()
    }

    // ── mappers ──────────────────────────────────────────────────────────────

    private fun EpisodeWithPodcast.toDomain() = Episode(
        id = id,
        podcastId = podcastId,
        title = title,
        description = description,
        audioUrl = audioUrl,
        durationSeconds = durationSeconds,
        publishedAt = publishedAt,
        isPlayed = isPlayed,
        playbackPositionMs = playbackPositionMs,
        podcastTitle = podcastTitle,
        podcastArtworkUrl = podcastArtworkUrl
    )

    private fun EpisodeEntity.toDomain() = Episode(
        id = id,
        podcastId = podcastId,
        title = title,
        description = description,
        audioUrl = audioUrl,
        durationSeconds = durationSeconds,
        publishedAt = publishedAt,
        isPlayed = isPlayed,
        playbackPositionMs = playbackPositionMs
    )

    private fun Episode.toEntity() = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        title = title,
        description = description,
        audioUrl = audioUrl,
        durationSeconds = durationSeconds,
        publishedAt = publishedAt,
        isPlayed = isPlayed,
        playbackPositionMs = playbackPositionMs
    )
}
