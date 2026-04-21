package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.repository.PlaylistRepository
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(name: String): Playlist = playlistRepository.createPlaylist(name)
}
