package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.repository.PlaylistRepository
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String) = playlistRepository.deletePlaylist(playlistId)
}
