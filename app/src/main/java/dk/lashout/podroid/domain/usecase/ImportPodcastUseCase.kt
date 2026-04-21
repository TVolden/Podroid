package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.PodcastRepository
import javax.inject.Inject

class ImportPodcastUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(feedUrl: String): Result<Podcast> =
        runCatching { podcastRepository.importFromFeedUrl(feedUrl) }
}
