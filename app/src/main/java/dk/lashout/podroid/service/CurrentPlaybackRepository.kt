package dk.lashout.podroid.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentPlaybackRepository @Inject constructor() {

    private val _activePlaylistId = MutableStateFlow<String?>(null)
    val activePlaylistId: StateFlow<String?> = _activePlaylistId.asStateFlow()

    private val _activeEpisodeIndex = MutableStateFlow(0)
    val activeEpisodeIndex: StateFlow<Int> = _activeEpisodeIndex.asStateFlow()

    fun setActive(playlistId: String, startIndex: Int = 0) {
        _activePlaylistId.value = playlistId
        _activeEpisodeIndex.value = startIndex
    }

    fun advanceIndex() { _activeEpisodeIndex.value++ }

    fun clear() {
        _activePlaylistId.value = null
        _activeEpisodeIndex.value = 0
    }
}
