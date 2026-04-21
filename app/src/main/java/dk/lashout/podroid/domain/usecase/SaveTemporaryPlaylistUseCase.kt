package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.repository.PlaylistRepository
import javax.inject.Inject

class SaveTemporaryPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, name: String): Playlist =
        playlistRepository.makePlaylistPermanent(playlistId, name)
}
