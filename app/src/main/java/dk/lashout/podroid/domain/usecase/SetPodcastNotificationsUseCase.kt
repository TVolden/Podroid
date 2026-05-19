package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.repository.PodcastRepository
import javax.inject.Inject

class SetPodcastNotificationsUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(podcastId: String, enabled: Boolean) =
        podcastRepository.setNotificationsEnabled(podcastId, enabled)
}
