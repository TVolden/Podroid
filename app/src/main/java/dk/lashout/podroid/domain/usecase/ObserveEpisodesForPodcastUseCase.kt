package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEpisodesForPodcastUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {
    operator fun invoke(podcastId: String): Flow<List<Episode>> =
        episodeRepository.getEpisodesForPodcast(podcastId)
}
