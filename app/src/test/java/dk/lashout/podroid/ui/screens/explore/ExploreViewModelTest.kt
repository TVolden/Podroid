package dk.lashout.podroid.ui.screens.explore

import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.PodcastRepository
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
    ) = ExploreViewModel(
        SearchPodcastsUseCase(FakePodcastRepository(results, error, onSearch))
    )

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
    }
}
