package dk.lashout.podroid.domain.repository

import dk.lashout.podroid.data.local.entity.TranscriptEntity
import dk.lashout.podroid.data.local.entity.TranscriptSearchResult
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow

interface TranscriptRepository {
    fun observeTranscript(episodeId: String): Flow<TranscriptEntity?>
    suspend fun getSegments(episodeId: String): List<TranscriptSegment>
    suspend fun fetchAndStore(episode: Episode)
    suspend fun analyse(episodeId: String)
    suspend fun search(query: String): List<TranscriptSearchResult>
}
