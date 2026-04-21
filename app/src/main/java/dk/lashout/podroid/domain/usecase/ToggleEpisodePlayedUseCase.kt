package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.repository.EpisodeRepository
import javax.inject.Inject

class ToggleEpisodePlayedUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {
    suspend operator fun invoke(episode: Episode) {
        if (episode.isPlayed) {
            episodeRepository.markAsUnplayed(episode.id)
        } else {
            episodeRepository.markAsPlayed(episode.id)
        }
    }
}
