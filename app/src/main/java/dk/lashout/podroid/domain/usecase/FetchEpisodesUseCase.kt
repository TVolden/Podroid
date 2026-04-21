package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.repository.EpisodeRepository
import javax.inject.Inject

class FetchEpisodesUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {
    suspend operator fun invoke(podcastId: String, feedUrl: String): Result<List<Episode>> =
        runCatching { episodeRepository.fetchAndStoreEpisodes(podcastId, feedUrl) }
}
