package dk.lashout.podroid.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dk.lashout.podroid.domain.repository.EpisodeRepository
import dk.lashout.podroid.domain.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
        val subscriptions = try {
            podcastRepository.getSubscriptions().first()
        } catch (e: Exception) {
            return Result.retry()
        }
        coroutineScope {
            subscriptions.map { podcast ->
                async {
                    try {
                        val newEpisodes = episodeRepository.fetchAndStoreEpisodes(podcast.id, podcast.feedUrl)
                        if (podcast.notificationsEnabled) notifier.notify(podcast.title, newEpisodes)
                    } catch (_: Exception) { }
                }
            }.awaitAll()
        }
        return Result.success()
    }
}
