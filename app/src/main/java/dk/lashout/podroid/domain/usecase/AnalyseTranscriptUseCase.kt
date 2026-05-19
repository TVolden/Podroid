package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.repository.TranscriptRepository
import javax.inject.Inject

class AnalyseTranscriptUseCase @Inject constructor(
    private val transcriptRepository: TranscriptRepository
) {
    suspend operator fun invoke(episodeId: String): Result<Unit> =
        runCatching { transcriptRepository.analyse(episodeId) }
}
