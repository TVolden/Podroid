package dk.lashout.podroid.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dk.lashout.podroid.BuildConfig
import dk.lashout.podroid.data.local.dao.TranscriptDao
import dk.lashout.podroid.data.local.entity.TranscriptEntity
import dk.lashout.podroid.data.local.entity.TranscriptSearchResult
import dk.lashout.podroid.data.local.entity.TranscriptSegmentEntity
import dk.lashout.podroid.data.local.entity.TranscriptSegmentFts
import dk.lashout.podroid.data.remote.api.GeminiApiService
import dk.lashout.podroid.data.remote.dto.GeminiContent
import dk.lashout.podroid.data.remote.dto.GeminiGenerationConfig
import dk.lashout.podroid.data.remote.dto.GeminiPart
import dk.lashout.podroid.data.remote.dto.GeminiRequest
import dk.lashout.podroid.data.rss.TranscriptParser
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.TranscriptSegment
import dk.lashout.podroid.domain.repository.TranscriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepositoryImpl @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val geminiApi: GeminiApiService,
    private val transcriptParser: TranscriptParser,
    private val okHttpClient: OkHttpClient
) : TranscriptRepository {

    private val gson = Gson()

    override fun observeTranscript(episodeId: String): Flow<TranscriptEntity?> =
        transcriptDao.observe(episodeId)

    override suspend fun getSegments(episodeId: String): List<TranscriptSegment> =
        transcriptDao.getSegments(episodeId).map {
            TranscriptSegment(it.startMs, it.endMs, it.text)
        }

    override suspend fun fetchAndStore(episode: Episode) {
        val url = episode.transcriptUrl ?: return
        withContext(Dispatchers.IO) {
            val (content, mimeType) = downloadTranscript(url) ?: return@withContext
            val segments = transcriptParser.parse(content, mimeType)
            if (segments.isEmpty()) return@withContext

            val entities = segments.mapIndexed { idx, seg ->
                TranscriptSegmentEntity(
                    id = "${episode.id}:$idx",
                    episodeId = episode.id,
                    startMs = seg.startMs,
                    endMs = seg.endMs,
                    text = seg.text
                )
            }
            val ftsRows = segments.mapIndexed { idx, seg ->
                TranscriptSegmentFts(
                    episodeId = episode.id,
                    startMs = seg.startMs,
                    text = seg.text
                )
            }

            transcriptDao.deleteFtsRows(episode.id)
            transcriptDao.deleteSegments(episode.id)
            transcriptDao.upsertSegments(entities)
            transcriptDao.upsertFtsRows(ftsRows)
            transcriptDao.upsert(
                TranscriptEntity(episodeId = episode.id, status = STATUS_SEGMENTS_ONLY)
            )
        }
    }

    override suspend fun analyse(episodeId: String) {
        val segments = transcriptDao.getSegments(episodeId)
        if (segments.isEmpty()) return

        val transcriptText = segments.joinToString("\n") { seg ->
            "[${formatMs(seg.startMs)}] ${seg.text}"
        }
        val prompt = buildPrompt(transcriptText)

        runCatching {
            val response = geminiApi.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig()
                )
            )
            val json = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: error("Empty Gemini response")
            parseAndStoreAnalysis(episodeId, json)
        }.onFailure {
            transcriptDao.upsert(TranscriptEntity(episodeId = episodeId, status = STATUS_ANALYSIS_FAILED))
        }
    }

    override suspend fun search(query: String): List<TranscriptSearchResult> =
        transcriptDao.search(query)

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun downloadTranscript(url: String): Pair<String, String>? = try {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val mimeType = response.header("Content-Type") ?: inferMimeType(url)
            val body = response.body?.string() ?: return null
            Pair(body, mimeType)
        }
    } catch (_: Exception) { null }

    private fun inferMimeType(url: String): String = when {
        url.endsWith(".vtt") -> "text/vtt"
        url.endsWith(".srt") -> "text/srt"
        url.endsWith(".json") -> "application/json"
        else -> "text/plain"
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun buildPrompt(transcript: String) = """
        You are analysing a podcast episode transcript. Return ONLY valid JSON with no markdown, no code blocks:
        {
          "summary": "2-3 paragraph summary of the episode",
          "key_takeaways": ["takeaway 1", "takeaway 2"],
          "topics": ["topic tag 1", "topic tag 2"],
          "talk_points": [{"timestamp_ms": 12345, "text": "description of main point"}]
        }
        For talk_points, identify the 5-10 most significant moments and use the exact timestamp_ms from the transcript.

        Transcript:
        $transcript
    """.trimIndent()

    private suspend fun parseAndStoreAnalysis(episodeId: String, json: String) {
        val map = runCatching {
            gson.fromJson<Map<String, Any>>(json, object : TypeToken<Map<String, Any>>() {}.type)
        }.getOrNull() ?: return

        val summary = (map["summary"] as? String) ?: ""
        val keyTakeaways = gson.toJson(map["key_takeaways"])
        val topics = gson.toJson(map["topics"])
        val talkPoints = gson.toJson(map["talk_points"])

        transcriptDao.upsert(
            TranscriptEntity(
                episodeId = episodeId,
                status = STATUS_ANALYSED,
                summary = summary,
                keyTakeaways = keyTakeaways,
                topics = topics,
                talkPoints = talkPoints,
                analysedAt = System.currentTimeMillis()
            )
        )
    }

    companion object {
        const val STATUS_SEGMENTS_ONLY = "segments_only"
        const val STATUS_ANALYSED = "analysed"
        const val STATUS_ANALYSIS_FAILED = "analysis_failed"
    }
}
