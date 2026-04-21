package dk.lashout.podroid.ui.screens.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.model.PlaylistEntry
import dk.lashout.podroid.domain.repository.PlaylistRepository
import dk.lashout.podroid.domain.usecase.GetPlaylistEntriesUseCase
import dk.lashout.podroid.domain.usecase.RemoveFromPlaylistUseCase
import dk.lashout.podroid.domain.usecase.ReorderPlaylistUseCase
import dk.lashout.podroid.domain.usecase.SaveTemporaryPlaylistUseCase
import dk.lashout.podroid.service.CurrentPlaybackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlaylistEntries: GetPlaylistEntriesUseCase,
    private val removeFromPlaylist: RemoveFromPlaylistUseCase,
    private val reorderPlaylist: ReorderPlaylistUseCase,
    private val saveTemporaryPlaylist: SaveTemporaryPlaylistUseCase,
    private val playlistRepository: PlaylistRepository,
    private val currentPlayback: CurrentPlaybackRepository
) : ViewModel() {

    val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    val entries: StateFlow<List<PlaylistEntry>> = combine(
        getPlaylistEntries(playlistId),
        currentPlayback.activePlaylistId,
        currentPlayback.activeEpisodeIndex
    ) { allEntries, activeId, activeIndex ->
        if (activeId == playlistId) allEntries.drop(activeIndex) else allEntries
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _playlist.value = playlistRepository.getPlaylistById(playlistId)
        }
    }

    fun remove(entryId: String) {
        viewModelScope.launch { removeFromPlaylist(entryId) }
    }

    fun reorder(from: Int, to: Int) {
        viewModelScope.launch { reorderPlaylist(playlistId, from, to) }
    }

    fun savePlaylist(name: String) {
        viewModelScope.launch {
            saveTemporaryPlaylist(playlistId, name)
            _playlist.value = _playlist.value?.copy(name = name, isTemporary = false)
        }
    }
}
