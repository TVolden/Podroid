package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.data.local.entity.TranscriptSearchResult
import dk.lashout.podroid.domain.repository.TranscriptRepository
import javax.inject.Inject

class SearchTranscriptsUseCase @Inject constructor(
    private val transcriptRepository: TranscriptRepository
) {
    suspend operator fun invoke(query: String): List<TranscriptSearchResult> =
        transcriptRepository.search(query)
}
