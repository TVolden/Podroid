package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.repository.EpisodeRepository
import javax.inject.Inject

class MarkEpisodesUnplayedUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {
    suspend operator fun invoke(episodeIds: Set<String>) {
        episodeIds.forEach { episodeRepository.markAsUnplayed(it) }
    }
}
