package dk.lashout.podroid.data.repository

import dk.lashout.podroid.data.local.entity.EpisodeEntity
import dk.lashout.podroid.data.local.entity.EpisodeWithPodcast
import dk.lashout.podroid.data.local.dao.EpisodeDao
import dk.lashout.podroid.data.local.dao.PodcastDao
import dk.lashout.podroid.data.local.entity.PodcastEntity
import dk.lashout.podroid.data.rss.RssParserWrapper
import dk.lashout.podroid.domain.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression: browsing a podcast detail used to reset isPlayed / playbackPositionMs because
 * upsertAll used OnConflictStrategy.REPLACE (DELETE + INSERT), overwriting user state with
 * the RSS-fresh defaults (isPlayed=false, playbackPositionMs=0).
 *
 * Fix: insertNewEpisodes uses IGNORE (won't touch existing rows), updateRssMeta updates only
 * RSS-derived columns and never touches isPlayed / playbackPositionMs / playedAt.
 */
class EpisodeRepositoryImplTest {

    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var podcastDao: FakePodcastDao
    private lateinit var repository: EpisodeRepositoryImpl

    @Before
    fun setUp() {
        episodeDao = FakeEpisodeDao()
        podcastDao = FakePodcastDao()
        repository = EpisodeRepositoryImpl(episodeDao, podcastDao, FakeRssParser())
    }

    @Test
    fun `fetchAndStoreEpisodes does not reset isPlayed for existing episodes`() = runTest {
        episodeDao.seed(episode(id = "ep-1", isPlayed = true, playbackPositionMs = 45_000))

        repository.fetchAndStoreEpisodes("pod-1", "https://example.com/feed.xml")

        assertTrue(episodeDao.store["ep-1"]!!.isPlayed)
    }

    @Test
    fun `fetchAndStoreEpisodes does not reset playbackPositionMs for existing episodes`() = runTest {
        episodeDao.seed(episode(id = "ep-1", isPlayed = false, playbackPositionMs = 120_000))

        repository.fetchAndStoreEpisodes("pod-1", "https://example.com/feed.xml")

        assertEquals(120_000L, episodeDao.store["ep-1"]!!.playbackPositionMs)
    }

    @Test
    fun `fetchAndStoreEpisodes inserts a brand new episode with isPlayed false`() = runTest {
        repository.fetchAndStoreEpisodes("pod-1", "https://example.com/feed.xml")

        assertFalse(episodeDao.store["ep-1"]!!.isPlayed)
        assertEquals(0L, episodeDao.store["ep-1"]!!.playbackPositionMs)
    }

    @Test
    fun `fetchAndStoreEpisodes updates RSS metadata without touching played state`() = runTest {
        episodeDao.seed(episode(id = "ep-1", title = "Old Title", isPlayed = true))

        repository.fetchAndStoreEpisodes("pod-1", "https://example.com/feed.xml")

        val stored = episodeDao.store["ep-1"]!!
        assertEquals("RSS Episode Title", stored.title)
        assertTrue(stored.isPlayed)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun episode(
        id: String = "ep-1",
        title: String = "Old Title",
        isPlayed: Boolean = false,
        playbackPositionMs: Long = 0L
    ) = EpisodeEntity(
        id = id,
        podcastId = "pod-1",
        title = title,
        description = "desc",
        audioUrl = "https://example.com/audio.mp3",
        durationSeconds = 1800,
        publishedAt = 1_000_000L,
        isPlayed = isPlayed,
        playbackPositionMs = playbackPositionMs
    )

    // ── fakes ─────────────────────────────────────────────────────────────────

    /** Returns the episode that the RSS feed always delivers: same ID, fresh defaults. */
    private inner class FakeRssParser : RssParserWrapper() {
        override suspend fun fetchEpisodes(podcastId: String, feedUrl: String) = listOf(
            Episode(
                id = "ep-1",
                podcastId = podcastId,
                title = "RSS Episode Title",
                description = "rss desc",
                audioUrl = "https://example.com/audio.mp3",
                durationSeconds = 1900,
                publishedAt = 2_000_000L,
                isPlayed = false,
                playbackPositionMs = 0L
            )
        )
    }

    private class FakeEpisodeDao : EpisodeDao {
        val store = mutableMapOf<String, EpisodeEntity>()

        fun seed(entity: EpisodeEntity) { store[entity.id] = entity }

        override suspend fun insertNewEpisodes(episodes: List<EpisodeEntity>): List<Long> {
            episodes.forEach { if (!store.containsKey(it.id)) store[it.id] = it }
            return episodes.map { if (store.containsKey(it.id)) -1L else 1L }
        }

        override suspend fun updateRssMeta(
            id: String, title: String, description: String,
            audioUrl: String, durationSeconds: Long, publishedAt: Long
        ) {
            store[id] = store[id]?.copy(
                title = title, description = description,
                audioUrl = audioUrl, durationSeconds = durationSeconds, publishedAt = publishedAt
            ) ?: return
        }

        override suspend fun getById(episodeId: String): EpisodeEntity? = store[episodeId]

        // ── unused stubs ──────────────────────────────────────────────────────
        override fun getByPodcastId(podcastId: String): Flow<List<EpisodeEntity>> = flowOf(emptyList())
        override fun getRecentFromSubscriptionsWithPodcast(): Flow<List<EpisodeWithPodcast>> = flowOf(emptyList())
        override fun getPlayedEpisodesWithPodcast(): Flow<List<EpisodeWithPodcast>> = flowOf(emptyList())
        override suspend fun getByIdWithPodcast(episodeId: String): EpisodeWithPodcast? = null
        override suspend fun updatePlaybackPosition(episodeId: String, positionMs: Long) = 0
        override suspend fun markAsPlayed(episodeId: String, playedAt: Long) = 0
        override suspend fun markAsUnplayed(episodeId: String) = 0
        override suspend fun getNextEpisodeNewerFirst(podcastId: String, currentPublishedAt: Long): EpisodeEntity? = null
        override suspend fun getNextEpisodeOlderFirst(podcastId: String, currentPublishedAt: Long): EpisodeEntity? = null
        override fun getRecentFromSubscriptions(): Flow<List<EpisodeEntity>> = flowOf(emptyList())
    }

    private class FakePodcastDao : PodcastDao {
        override suspend fun upsert(podcast: PodcastEntity) = 0L
        override fun getSubscriptions(): Flow<List<PodcastEntity>> = flowOf(emptyList())
        override suspend fun getById(id: String): PodcastEntity? = null
        override suspend fun subscribe(podcastId: String) = 0
        override suspend fun unsubscribe(podcastId: String) = 0
    }
}
