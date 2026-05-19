package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.data.local.entity.TranscriptEntity
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.TranscriptSegment
import dk.lashout.podroid.domain.repository.TranscriptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTranscriptUseCase @Inject constructor(
    private val transcriptRepository: TranscriptRepository
) {
    fun observe(episodeId: String): Flow<TranscriptEntity?> =
        transcriptRepository.observeTranscript(episodeId)

    suspend fun segments(episodeId: String): List<TranscriptSegment> =
        transcriptRepository.getSegments(episodeId)

    suspend fun fetchAndStore(episode: Episode) =
        transcriptRepository.fetchAndStore(episode)
}
