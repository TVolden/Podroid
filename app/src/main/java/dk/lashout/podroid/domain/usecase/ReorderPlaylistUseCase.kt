package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.repository.PlaylistRepository
import javax.inject.Inject

class ReorderPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, fromPosition: Int, toPosition: Int) =
        playlistRepository.reorder(playlistId, fromPosition, toPosition)
}
