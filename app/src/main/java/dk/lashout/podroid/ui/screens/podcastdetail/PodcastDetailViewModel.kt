package dk.lashout.podroid.ui.screens.podcastdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.PodcastRepository
import dk.lashout.podroid.domain.usecase.AddEpisodeToPlaylistUseCase
import dk.lashout.podroid.domain.usecase.CreatePlaylistUseCase
import dk.lashout.podroid.domain.usecase.FetchEpisodesUseCase
import dk.lashout.podroid.domain.usecase.GetPlaylistsUseCase
import dk.lashout.podroid.domain.usecase.MarkEpisodesPlayedUseCase
import dk.lashout.podroid.domain.usecase.MarkEpisodesUnplayedUseCase
import dk.lashout.podroid.domain.usecase.ObserveEpisodesForPodcastUseCase
import dk.lashout.podroid.domain.usecase.SubscribeToPodcastUseCase
import dk.lashout.podroid.domain.usecase.ToggleEpisodePlayedUseCase
import dk.lashout.podroid.domain.usecase.UnsubscribeFromPodcastUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastRepository: PodcastRepository,
    private val fetchEpisodes: FetchEpisodesUseCase,
    private val observeEpisodes: ObserveEpisodesForPodcastUseCase,
    private val subscribeToPodcast: SubscribeToPodcastUseCase,
    private val unsubscribeFromPodcast: UnsubscribeFromPodcastUseCase,
    private val toggleEpisodePlayed: ToggleEpisodePlayedUseCase,
    private val addEpisodeToPlaylist: AddEpisodeToPlaylistUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
    getPlaylists: GetPlaylistsUseCase,
    private val markEpisodesPlayed: MarkEpisodesPlayedUseCase,
    private val markEpisodesUnplayed: MarkEpisodesUnplayedUseCase
) : ViewModel() {

    private val podcastId: String = run {
        val raw = checkNotNull(savedStateHandle.get<String>("podcastId"))
        try { Gson().fromJson(raw, Podcast::class.java).id } catch (_: Exception) { raw }
    }

    private val _podcast = MutableStateFlow<Podcast?>(null)
    val podcast: StateFlow<Podcast?> = _podcast.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val episodes: StateFlow<List<Episode>> = combine(
        observeEpisodes(podcastId),
        _podcast
    ) { episodes, podcast ->
        if (podcast != null) episodes.map { it.copy(podcastTitle = podcast.title, podcastArtworkUrl = podcast.artworkUrl) }
        else episodes
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<Playlist>> = getPlaylists()
        .map { it.filter { p -> !p.isTemporary } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _episodeForPlaylistDialog = MutableStateFlow<Episode?>(null)
    val episodeForPlaylistDialog: StateFlow<Episode?> = _episodeForPlaylistDialog.asStateFlow()

    init { loadPodcast() }

    private fun loadPodcast() {
        viewModelScope.launch {
            _isLoading.value = true
            val p = podcastRepository.getPodcastById(podcastId)
            _podcast.value = p
            if (p != null) fetchEpisodes(podcastId, p.feedUrl).onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun setPodcast(podcast: Podcast) {
        if (_podcast.value == null) {
            _podcast.value = podcast
            viewModelScope.launch {
                _isLoading.value = true
                fetchEpisodes(podcast.id, podcast.feedUrl).onFailure { _error.value = it.message }
                _isLoading.value = false
            }
        }
    }

    fun subscribe() {
        val p = _podcast.value ?: return
        viewModelScope.launch { subscribeToPodcast(p); _podcast.value = p.copy(isSubscribed = true) }
    }

    fun unsubscribe() {
        val p = _podcast.value ?: return
        viewModelScope.launch { unsubscribeFromPodcast(p.id); _podcast.value = p.copy(isSubscribed = false) }
    }

    fun togglePlayed(episode: Episode) { viewModelScope.launch { toggleEpisodePlayed(episode) } }

    // ── playlist dialog ───────────────────────────────────────────────────────

    fun showPlaylistDialog(episode: Episode) { _episodeForPlaylistDialog.value = episode }
    fun dismissPlaylistDialog() { _episodeForPlaylistDialog.value = null }

    fun addToPlaylist(playlistId: String) {
        val ep = _episodeForPlaylistDialog.value ?: return
        viewModelScope.launch { addEpisodeToPlaylist(playlistId, ep.id); dismissPlaylistDialog() }
    }

    fun createPlaylistAndAdd(name: String) {
        val ep = _episodeForPlaylistDialog.value ?: return
        viewModelScope.launch { val p = createPlaylist(name); addEpisodeToPlaylist(p.id, ep.id); dismissPlaylistDialog() }
    }

    // ── multi-select ──────────────────────────────────────────────────────────

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun toggleSelection(episodeId: String) {
        _selectedIds.update { if (episodeId in it) it - episodeId else it + episodeId }
    }
    fun selectAll() { _selectedIds.value = episodes.value.map { it.id }.toSet() }
    fun clearSelection() { _selectedIds.value = emptySet() }
    fun markSelectedAsPlayed() {
        val ids = _selectedIds.value
        viewModelScope.launch { markEpisodesPlayed(ids); clearSelection() }
    }
    fun markSelectedAsUnplayed() {
        val ids = _selectedIds.value
        viewModelScope.launch { markEpisodesUnplayed(ids); clearSelection() }
    }
}
