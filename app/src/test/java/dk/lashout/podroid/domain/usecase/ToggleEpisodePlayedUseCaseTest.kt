package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.AutoplayOrder
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ToggleEpisodePlayedUseCaseTest {

    private lateinit var fakeRepository: FakeEpisodeRepository
    private lateinit var useCase: ToggleEpisodePlayedUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeEpisodeRepository()
        useCase = ToggleEpisodePlayedUseCase(fakeRepository)
    }

    @Test
    fun `marks unplayed episode as played`() = runTest {
        val episode = episode(isPlayed = false)

        useCase(episode)

        assertEquals(episode.id, fakeRepository.lastMarkedPlayedId)
        assertEquals(null, fakeRepository.lastMarkedUnplayedId)
    }

    @Test
    fun `marks played episode as unplayed`() = runTest {
        val episode = episode(isPlayed = true)

        useCase(episode)

        assertEquals(episode.id, fakeRepository.lastMarkedUnplayedId)
        assertEquals(null, fakeRepository.lastMarkedPlayedId)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun episode(isPlayed: Boolean) = Episode(
        id = "ep-1",
        podcastId = "pod-1",
        title = "Test Episode",
        description = "",
        audioUrl = "https://example.com/audio.mp3",
        durationSeconds = 1800,
        publishedAt = 0L,
        isPlayed = isPlayed
    )

    private class FakeEpisodeRepository : EpisodeRepository {
        var lastMarkedPlayedId: String? = null
        var lastMarkedUnplayedId: String? = null

        override suspend fun markAsPlayed(episodeId: String) { lastMarkedPlayedId = episodeId }
        override suspend fun markAsUnplayed(episodeId: String) { lastMarkedUnplayedId = episodeId }

        override suspend fun fetchAndStoreEpisodes(podcastId: String, feedUrl: String) = emptyList<Episode>()
        override fun getEpisodesForPodcast(podcastId: String): Flow<List<Episode>> = throw NotImplementedError()
        override fun getRecentEpisodesForSubscriptions(): Flow<List<Episode>> = throw NotImplementedError()
        override fun getPlayedEpisodes(): Flow<List<Episode>> = throw NotImplementedError()
        override suspend fun getEpisodeById(episodeId: String): Episode? = null
        override suspend fun getEpisodeWithPodcast(episodeId: String): Episode? = null
        override suspend fun updatePlaybackPosition(episodeId: String, positionMs: Long) = Unit
        override suspend fun getNextEpisodeInPodcast(
            podcastId: String, currentPublishedAt: Long, order: AutoplayOrder
        ): Episode? = null
    }
}
