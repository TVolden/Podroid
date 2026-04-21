package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.repository.EpisodeRepository
import javax.inject.Inject

class UpdatePlaybackPositionUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {
    suspend operator fun invoke(episodeId: String, positionMs: Long) {
        episodeRepository.updatePlaybackPosition(episodeId, positionMs)
    }
}
