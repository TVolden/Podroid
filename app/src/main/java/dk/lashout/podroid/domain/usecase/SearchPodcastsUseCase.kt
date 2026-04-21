package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.PodcastRepository
import javax.inject.Inject

class SearchPodcastsUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(query: String): Result<List<Podcast>> = runCatching {
        podcastRepository.searchPodcasts(query)
    }
}
