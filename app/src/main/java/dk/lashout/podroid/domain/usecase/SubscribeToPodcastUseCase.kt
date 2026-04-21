package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.PodcastRepository
import javax.inject.Inject

class SubscribeToPodcastUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(podcast: Podcast) {
        podcastRepository.subscribe(podcast)
    }
}
