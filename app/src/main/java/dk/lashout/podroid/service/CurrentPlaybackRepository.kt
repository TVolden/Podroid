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

    var activeEpisodeIndex: Int = 0
        private set

    fun setActive(playlistId: String, startIndex: Int = 0) {
        _activePlaylistId.value = playlistId
        activeEpisodeIndex = startIndex
    }

    fun advanceIndex() { activeEpisodeIndex++ }

    fun clear() {
        _activePlaylistId.value = null
        activeEpisodeIndex = 0
    }
}
