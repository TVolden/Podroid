package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.repository.PlaylistRepository
import javax.inject.Inject

class AddEpisodeToPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, episodeId: String) =
        playlistRepository.addEpisodeToPlaylist(playlistId, episodeId)
}
