package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubscriptionsUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository
) {
    operator fun invoke(): Flow<List<Podcast>> = podcastRepository.getSubscriptions()
}
