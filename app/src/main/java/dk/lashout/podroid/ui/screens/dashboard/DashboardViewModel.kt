package dk.lashout.podroid.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.usecase.AddEpisodeToPlaylistUseCase
import dk.lashout.podroid.domain.usecase.CreatePlaylistUseCase
import dk.lashout.podroid.domain.usecase.GetDashboardEpisodesUseCase
import dk.lashout.podroid.domain.usecase.GetPlaylistsUseCase
import dk.lashout.podroid.domain.usecase.MarkEpisodesPlayedUseCase
import dk.lashout.podroid.domain.usecase.MarkEpisodesUnplayedUseCase
import dk.lashout.podroid.domain.usecase.ToggleEpisodePlayedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboardEpisodes: GetDashboardEpisodesUseCase,
    getPlaylists: GetPlaylistsUseCase,
    private val toggleEpisodePlayed: ToggleEpisodePlayedUseCase,
    private val addEpisodeToPlaylist: AddEpisodeToPlaylistUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
    private val markEpisodesPlayed: MarkEpisodesPlayedUseCase,
    private val markEpisodesUnplayed: MarkEpisodesUnplayedUseCase
) : ViewModel() {

    val episodes: StateFlow<List<Episode>> = getDashboardEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Non-temporary playlists only (for the "add to playlist" dialog)
    val playlists: StateFlow<List<Playlist>> = getPlaylists()
        .map { it.filter { p -> !p.isTemporary } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _episodeForPlaylistDialog = MutableStateFlow<Episode?>(null)
    val episodeForPlaylistDialog: StateFlow<Episode?> = _episodeForPlaylistDialog

    // ── playlist dialog ───────────────────────────────────────────────────────

    fun showPlaylistDialog(episode: Episode) { _episodeForPlaylistDialog.value = episode }
    fun dismissPlaylistDialog() { _episodeForPlaylistDialog.value = null }

    fun addToPlaylist(playlistId: String) {
        val ep = _episodeForPlaylistDialog.value ?: return
        viewModelScope.launch { addEpisodeToPlaylist(playlistId, ep.id); dismissPlaylistDialog() }
    }

    fun createPlaylistAndAdd(name: String) {
        val ep = _episodeForPlaylistDialog.value ?: return
        viewModelScope.launch {
            val p = createPlaylist(name)
            addEpisodeToPlaylist(p.id, ep.id)
            dismissPlaylistDialog()
        }
    }

    // ── played toggle ─────────────────────────────────────────────────────────

    fun togglePlayed(episode: Episode) {
        viewModelScope.launch { toggleEpisodePlayed(episode) }
    }

    // ── multi-select ──────────────────────────────────────────────────────────

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds

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
