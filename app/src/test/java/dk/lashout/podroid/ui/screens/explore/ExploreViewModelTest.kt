package dk.lashout.podroid.ui.screens.explore

import dk.lashout.podroid.data.remote.api.GeminiApiService
import dk.lashout.podroid.data.remote.api.ItunesApiService
import dk.lashout.podroid.data.remote.dto.GeminiRequest
import dk.lashout.podroid.data.remote.dto.GeminiResponse
import dk.lashout.podroid.data.remote.dto.ItunesEpisodeSearchResponse
import dk.lashout.podroid.data.remote.dto.ItunesSearchResponse
import dk.lashout.podroid.domain.model.AutoplayOrder
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.model.PlaylistEntry
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.EpisodeRepository
import dk.lashout.podroid.domain.repository.PlaylistRepository
import dk.lashout.podroid.domain.repository.PodcastRepository
import dk.lashout.podroid.domain.usecase.AddEpisodeToPlaylistUseCase
import dk.lashout.podroid.domain.usecase.CreatePlaylistUseCase
import dk.lashout.podroid.domain.usecase.GetPlaylistsUseCase
import dk.lashout.podroid.domain.usecase.ImportPodcastUseCase
import dk.lashout.podroid.domain.usecase.SearchEpisodesUseCase
import dk.lashout.podroid.domain.usecase.SearchPodcastsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        val viewModel = buildViewModel()
        assertEquals(ExploreUiState.Idle, viewModel.uiState.value)
        assertEquals("", viewModel.query.value)
    }

    @Test
    fun `blank query resets state to Idle`() = runTest {
        val viewModel = buildViewModel(results = listOf(podcast("p1")))

        viewModel.onQueryChange("kotlin")
        testDispatcher.scheduler.advanceTimeBy(400)   // past 300ms debounce
        testDispatcher.scheduler.runCurrent()

        viewModel.onQueryChange("")
        assertEquals(ExploreUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `successful search emits Results`() = runTest {
        val podcasts = listOf(podcast("p1"), podcast("p2"))
        val viewModel = buildViewModel(results = podcasts)

        viewModel.onQueryChange("kotlin")
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is ExploreUiState.Results)
        assertEquals(podcasts, (state as ExploreUiState.Results).podcasts)
    }

    @Test
    fun `failed search emits Error with message`() = runTest {
        val viewModel = buildViewModel(error = RuntimeException("network error"))

        viewModel.onQueryChange("kotlin")
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is ExploreUiState.Error)
        assertEquals("network error", (state as ExploreUiState.Error).message)
    }

    @Test
    fun `duplicate queries are deduplicated`() = runTest {
        var callCount = 0
        val viewModel = buildViewModel(onSearch = { callCount++ })

        viewModel.onQueryChange("kotlin")
        viewModel.onQueryChange("kotlin")   // same value — should not trigger a second search
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, callCount)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun buildViewModel(
        results: List<Podcast> = emptyList(),
        error: Throwable? = null,
        onSearch: () -> Unit = {}
    ): ExploreViewModel {
        val podcastRepo = FakePodcastRepository(results, error, onSearch)
        val playlistRepo = FakePlaylistRepository()
        return ExploreViewModel(
            searchPodcasts = SearchPodcastsUseCase(podcastRepo),
            importPodcast = ImportPodcastUseCase(podcastRepo),
            searchEpisodes = SearchEpisodesUseCase(FakeItunesApiService(), FakeGeminiApiService()),
            podcastRepository = podcastRepo,
            episodeRepository = FakeEpisodeRepository(),
            addEpisodeToPlaylist = AddEpisodeToPlaylistUseCase(playlistRepo),
            createPlaylist = CreatePlaylistUseCase(playlistRepo),
            getPlaylists = GetPlaylistsUseCase(playlistRepo)
        )
    }

    private fun podcast(id: String) = Podcast(
        id = id, title = "Podcast $id", author = "Author",
        description = "", artworkUrl = "", feedUrl = ""
    )

    private class FakePodcastRepository(
        private val results: List<Podcast>,
        private val error: Throwable?,
        private val onSearch: () -> Unit
    ) : PodcastRepository {
        override suspend fun searchPodcasts(query: String): List<Podcast> {
            onSearch()
            if (error != null) throw error
            return results
        }

        override fun getSubscriptions(): Flow<List<Podcast>> = flowOf(emptyList())
        override suspend fun getPodcastById(id: String): Podcast? = null
        override suspend fun subscribe(podcast: Podcast) = Unit
        override suspend fun unsubscribe(podcastId: String) = Unit
        override suspend fun upsertPodcast(podcast: Podcast) = Unit
        override suspend fun importFromFeedUrl(feedUrl: String): Podcast = throw UnsupportedOperationException()
        override suspend fun setNotificationsEnabled(podcastId: String, enabled: Boolean) = Unit
    }

    private class FakeItunesApiService : ItunesApiService {
        override suspend fun searchPodcasts(term: String, media: String, limit: Int) =
            ItunesSearchResponse(resultCount = 0, results = emptyList())
        override suspend fun searchEpisodes(term: String, entity: String, limit: Int) =
            ItunesEpisodeSearchResponse(resultCount = 0, results = emptyList())
    }

    private class FakeEpisodeRepository : EpisodeRepository {
        override suspend fun fetchAndStoreEpisodes(podcastId: String, feedUrl: String) = emptyList<Episode>()
        override fun getEpisodesForPodcast(podcastId: String) = flowOf(emptyList<Episode>())
        override fun getRecentEpisodesForSubscriptions() = flowOf(emptyList<Episode>())
        override fun getPlayedEpisodes() = flowOf(emptyList<Episode>())
        override suspend fun getEpisodeById(episodeId: String): Episode? = null
        override suspend fun updatePlaybackPosition(episodeId: String, positionMs: Long) = Unit
        override suspend fun markAsPlayed(episodeId: String) = Unit
        override suspend fun markAsUnplayed(episodeId: String) = Unit
        override suspend fun getEpisodeWithPodcast(episodeId: String): Episode? = null
        override suspend fun getNextEpisodeInPodcast(podcastId: String, currentPublishedAt: Long, order: AutoplayOrder): Episode? = null
        override suspend fun storeEpisode(episode: Episode) = Unit
    }

    private class FakeGeminiApiService : GeminiApiService {
        override suspend fun generateContent(apiKey: String, request: GeminiRequest): GeminiResponse =
            GeminiResponse(candidates = emptyList())
    }

    private class FakePlaylistRepository : PlaylistRepository {
        override fun getPlaylists(): Flow<List<Playlist>> = flowOf(emptyList())
        override suspend fun getPlaylistById(id: String): Playlist? = null
        override suspend fun createPlaylist(name: String) = Playlist(id = name, name = name, isTemporary = false, createdAt = 0L)
        override suspend fun deletePlaylist(id: String) = Unit
        override suspend fun makePlaylistPermanent(id: String, name: String) = Playlist(id = id, name = name, isTemporary = false, createdAt = 0L)
        override suspend fun getTemporaryPlaylist(): Playlist? = null
        override suspend fun replaceTemporaryPlaylist(episodes: List<Episode>) = Playlist(id = "temp", name = "temp", isTemporary = true, createdAt = 0L)
        override fun getPlaylistEntries(playlistId: String): Flow<List<PlaylistEntry>> = flowOf(emptyList())
        override suspend fun getPlaylistEntriesSync(playlistId: String) = emptyList<PlaylistEntry>()
        override suspend fun addEpisodeToPlaylist(playlistId: String, episodeId: String) = Unit
        override suspend fun removeEntry(entryId: String) = Unit
        override suspend fun reorder(playlistId: String, fromPosition: Int, toPosition: Int) = Unit
        override suspend fun markEntryPlayedInPlaylist(playlistId: String, episodeId: String) = Unit
    }
}
