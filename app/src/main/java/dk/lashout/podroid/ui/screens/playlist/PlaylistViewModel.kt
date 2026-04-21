package dk.lashout.podroid.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.usecase.CreatePlaylistUseCase
import dk.lashout.podroid.domain.usecase.DeletePlaylistUseCase
import dk.lashout.podroid.domain.usecase.GetPlaylistsUseCase
import dk.lashout.podroid.service.CurrentPlaybackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    getPlaylists: GetPlaylistsUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
    private val deletePlaylist: DeletePlaylistUseCase,
    private val currentPlayback: CurrentPlaybackRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = combine(
        getPlaylists(),
        currentPlayback.activePlaylistId,
        currentPlayback.activeEpisodeIndex
    ) { playlists, activeId, activeIndex ->
        playlists.map { playlist ->
            if (playlist.id == activeId && playlist.isTemporary)
                playlist.copy(episodeCount = (playlist.episodeCount - activeIndex).coerceAtLeast(0))
            else
                playlist
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) {
        viewModelScope.launch { createPlaylist(name) }
    }

    fun delete(playlistId: String) {
        viewModelScope.launch { deletePlaylist(playlistId) }
    }
}
