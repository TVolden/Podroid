package dk.lashout.podroid.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.EpisodeGroup
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.EpisodeRepository
import dk.lashout.podroid.domain.repository.PodcastRepository
import dk.lashout.podroid.domain.usecase.AddEpisodeToPlaylistUseCase
import dk.lashout.podroid.domain.usecase.CreatePlaylistUseCase
import dk.lashout.podroid.domain.usecase.GetPlaylistsUseCase
import dk.lashout.podroid.domain.usecase.ImportPodcastUseCase
import dk.lashout.podroid.domain.usecase.SearchEpisodesUseCase
import dk.lashout.podroid.domain.usecase.SearchPodcastsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchMode { PODCASTS, EPISODES }

sealed interface EpisodeSearchState {
    object Idle : EpisodeSearchState
    object Loading : EpisodeSearchState
    data class Results(val groups: List<EpisodeGroup>) : EpisodeSearchState
    data class Error(val message: String) : EpisodeSearchState
}

sealed interface ImportState {
    object Idle : ImportState
    object Loading : ImportState
    data class Success(val podcast: Podcast) : ImportState
    data class Error(val message: String) : ImportState
}

sealed interface ExploreUiState {
    object Idle : ExploreUiState
    object Loading : ExploreUiState
    data class Results(val podcasts: List<Podcast>) : ExploreUiState
    data class Error(val message: String) : ExploreUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val searchPodcasts: SearchPodcastsUseCase,
    private val importPodcast: ImportPodcastUseCase,
    private val searchEpisodes: SearchEpisodesUseCase,
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
    private val addEpisodeToPlaylist: AddEpisodeToPlaylistUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
    getPlaylists: GetPlaylistsUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.PODCASTS)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Idle)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _episodeState = MutableStateFlow<EpisodeSearchState>(EpisodeSearchState.Idle)
    val episodeState: StateFlow<EpisodeSearchState> = _episodeState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _episodeForPlaylistDialog = MutableStateFlow<Pair<Episode, Podcast>?>(null)
    val episodeForPlaylistDialog: StateFlow<Pair<Episode, Podcast>?> = _episodeForPlaylistDialog.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = getPlaylists()
        .map { it.filter { p -> !p.isTemporary } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val debouncedQuery = _query.debounce(300).distinctUntilChanged()

        combine(debouncedQuery.filter { it.isNotBlank() }, _searchMode) { q, mode -> q to mode }
            .onEach { (q, mode) ->
                when (mode) {
                    SearchMode.PODCASTS -> {
                        _uiState.value = ExploreUiState.Loading
                        searchPodcasts(q).fold(
                            onSuccess = { _uiState.value = ExploreUiState.Results(it) },
                            onFailure = { _uiState.value = ExploreUiState.Error(it.message ?: "Search failed") }
                        )
                    }
                    SearchMode.EPISODES -> {
                        _episodeState.value = EpisodeSearchState.Loading
                        searchEpisodes(q).fold(
                            onSuccess = { _episodeState.value = EpisodeSearchState.Results(it) },
                            onFailure = { _episodeState.value = EpisodeSearchState.Error(it.message ?: "Search failed") }
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(q: String) {
        _query.value = q
        if (q.isBlank()) {
            _uiState.value = ExploreUiState.Idle
            _episodeState.value = EpisodeSearchState.Idle
        }
    }

    fun setSearchMode(mode: SearchMode) {
        if (_searchMode.value == mode) return
        when (mode) {
            SearchMode.EPISODES -> _uiState.value = ExploreUiState.Idle
            SearchMode.PODCASTS -> _episodeState.value = EpisodeSearchState.Idle
        }
        _searchMode.value = mode
    }

    // ── episode play/playlist ────────────────────────────────────────────────

    fun prepareEpisode(episode: Episode, podcast: Podcast) {
        viewModelScope.launch(Dispatchers.IO) {
            podcastRepository.upsertPodcast(podcast)
            episodeRepository.storeEpisode(episode)
        }
    }

    fun showEpisodePlaylistDialog(episode: Episode, podcast: Podcast) {
        viewModelScope.launch(Dispatchers.IO) {
            podcastRepository.upsertPodcast(podcast)
            episodeRepository.storeEpisode(episode)
        }
        _episodeForPlaylistDialog.value = episode to podcast
    }

    fun dismissEpisodePlaylistDialog() { _episodeForPlaylistDialog.value = null }

    fun addEpisodeToPlaylist(playlistId: String) {
        val ep = _episodeForPlaylistDialog.value?.first ?: return
        viewModelScope.launch {
            addEpisodeToPlaylist(playlistId, ep.id)
            dismissEpisodePlaylistDialog()
        }
    }

    fun createPlaylistAndAddEpisode(name: String) {
        val ep = _episodeForPlaylistDialog.value?.first ?: return
        viewModelScope.launch {
            val playlist = createPlaylist(name)
            addEpisodeToPlaylist(playlist.id, ep.id)
            dismissEpisodePlaylistDialog()
        }
    }

    // ── import by URL ────────────────────────────────────────────────────────

    fun importFromUrl(url: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            importPodcast(url).fold(
                onSuccess = { _importState.value = ImportState.Success(it) },
                onFailure = { _importState.value = ImportState.Error(it.message ?: "Import failed") }
            )
        }
    }

    fun clearImportState() { _importState.value = ImportState.Idle }
}
