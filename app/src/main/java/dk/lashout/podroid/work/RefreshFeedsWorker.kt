package dk.lashout.podroid.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dk.lashout.podroid.domain.repository.EpisodeRepository
import dk.lashout.podroid.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

@HiltWorker
class RefreshFeedsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
    private val notifier: NewEpisodeNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val subscriptions = podcastRepository.getSubscriptions().first()
            subscriptions.forEach { podcast ->
                val newEpisodes = episodeRepository.fetchAndStoreEpisodes(podcast.id, podcast.feedUrl)
                notifier.notify(podcast.title, newEpisodes)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
