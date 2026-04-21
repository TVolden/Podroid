package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.repository.PlaylistRepository
import javax.inject.Inject

class RemoveFromPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(entryId: String) = playlistRepository.removeEntry(entryId)
}
