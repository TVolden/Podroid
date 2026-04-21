package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.PlaylistEntry
import dk.lashout.podroid.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaylistEntriesUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(playlistId: String): Flow<List<PlaylistEntry>> =
        playlistRepository.getPlaylistEntries(playlistId)
}
