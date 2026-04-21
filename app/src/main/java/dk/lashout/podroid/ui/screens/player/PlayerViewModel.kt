package dk.lashout.podroid.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.model.PlayerState
import dk.lashout.podroid.domain.repository.PlaylistRepository
import dk.lashout.podroid.domain.usecase.GetPlaylistsUseCase
import dk.lashout.podroid.domain.usecase.UpdatePlaybackPositionUseCase
import dk.lashout.podroid.service.CurrentPlaybackRepository
import dk.lashout.podroid.service.PlaybackController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class QueueEntry(val episode: Episode, val isCurrent: Boolean)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaController: PlaybackController,
    private val updatePlaybackPosition: UpdatePlaybackPositionUseCase,
    private val currentPlayback: CurrentPlaybackRepository,
    private val playlistRepository: PlaylistRepository,
    getPlaylists: GetPlaylistsUseCase
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = mediaController.playerState

    val activePlaylist: StateFlow<Playlist?> = combine(
        currentPlayback.activePlaylistId,
        getPlaylists()
    ) { activeId, playlists ->
        if (activeId == null) null else playlists.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val queue: StateFlow<List<QueueEntry>> = currentPlayback.activePlaylistId
        .flatMapLatest { playlistId ->
            if (playlistId == null) flowOf(emptyList())
            else combine(
                playlistRepository.getPlaylistEntries(playlistId),
                mediaController.playerState.map { it.currentEpisode?.id }.distinctUntilChanged()
            ) { entries, currentId ->
                if (currentId == null) return@combine emptyList()
                val idx = entries.indexOfFirst { it.episode.id == currentId }
                if (idx < 0) return@combine emptyList()
                entries.drop(idx).map { QueueEntry(it.episode, it.episode.id == currentId) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        mediaController.playerState
            .debounce(2_000)
            .distinctUntilChangedBy { it.positionMs / 5_000 }
            .onEach { state ->
                val episode = state.currentEpisode ?: return@onEach
                updatePlaybackPosition(episode.id, state.positionMs)
            }
            .launchIn(viewModelScope)
    }

    fun playPause() {
        if (playerState.value.isPlaying) mediaController.pause() else mediaController.play()
    }

    fun skipBack() = mediaController.skipBack()
    fun skipForward() = mediaController.skipForward()
    fun seekTo(positionMs: Long) = mediaController.seekTo(positionMs)
    fun setSpeed(speed: Float) = mediaController.setSpeed(speed)
    fun savePlaylist(name: String) = mediaController.saveCurrentPlaylist(name)
}
