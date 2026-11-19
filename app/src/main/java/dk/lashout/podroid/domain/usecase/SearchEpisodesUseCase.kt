package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.BuildConfig
import dk.lashout.podroid.data.remote.api.GeminiApiService
import dk.lashout.podroid.data.remote.api.ItunesApiService
import dk.lashout.podroid.data.remote.dto.GeminiContent
import dk.lashout.podroid.data.remote.dto.GeminiGenerationConfig
import dk.lashout.podroid.data.remote.dto.GeminiPart
import dk.lashout.podroid.data.remote.dto.GeminiRequest
import dk.lashout.podroid.data.remote.dto.ItunesEpisodeResult
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.EpisodeGroup
import dk.lashout.podroid.domain.model.Podcast
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class SearchEpisodesUseCase @Inject constructor(
    private val api: ItunesApiService,
    private val geminiApi: GeminiApiService
) {
    suspend operator fun invoke(query: String): Result<List<EpisodeGroup>> = runCatching {
        val rawResults = api.searchEpisodes(query).results
        if (rawResults.isEmpty()) return@runCatching emptyList()

        // iTunes can return the same episode under multiple collection entries
        val results = rawResults.distinctBy { it.episodeGuid.ifBlank { "itunes:${it.trackId}" } }

        val ranked = rerankWithGemini(query, results).ifEmpty { results }

        val groupOrder = ranked.map { it.collectionId }.distinct()
        val byCollection = ranked.groupBy { it.collectionId }
        groupOrder.mapNotNull { collectionId ->
            val items = byCollection[collectionId] ?: return@mapNotNull null
            val first = items.first()
            EpisodeGroup(
                podcast = first.toPodcast(),
                episodes = items.map { it.toEpisode() }
            )
        }
    }

    private suspend fun rerankWithGemini(
        query: String,
        results: List<ItunesEpisodeResult>
    ): List<ItunesEpisodeResult> {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return emptyList()
        return runCatching {
            val listing = results.mapIndexed { i, r ->
                val desc = r.description.take(120).replace('\n', ' ').trim()
                "[$i] ${r.trackName} — $desc"
            }.joinToString("\n")

            val prompt = """
                Search query: "$query"

                From the list of podcast episodes below, select those whose title or description
                indicates the episode is actually ABOUT the search topic (max $MAX_RESULTS, best first).
                Ignore what podcast it comes from — judge each episode purely on its own content.
                Return ONLY valid JSON: {"indices": [0, 3, 7]}

                $listing
            """.trimIndent()

            val response = geminiApi.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig(responseMimeType = "application/json")
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@runCatching emptyList<ItunesEpisodeResult>()

            Regex("\\d+").findAll(text)
                .mapNotNull { it.value.toIntOrNull() }
                .filter { it in results.indices }
                .map { results[it] }
                .toList()
        }.getOrElse { emptyList() }
    }

    private fun ItunesEpisodeResult.toPodcast() = Podcast(
        id = "itunes:$collectionId",
        title = collectionName,
        author = "",
        description = "",
        artworkUrl = artworkUrl600,
        feedUrl = feedUrl,
        isSubscribed = false
    )

    private fun ItunesEpisodeResult.toEpisode(): Episode {
        val publishedAtMs = runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(releaseDate)?.time ?: 0L
        }.getOrElse { 0L }
        return Episode(
            id = episodeGuid.ifBlank { "itunes:$trackId" },
            podcastId = "itunes:$collectionId",
            title = trackName,
            description = description,
            audioUrl = episodeUrl,
            durationSeconds = trackTimeMillis / 1000L,
            publishedAt = publishedAtMs,
            podcastTitle = collectionName,
            podcastArtworkUrl = artworkUrl600
        )
    }

    companion object {
        private const val MAX_RESULTS = 20
    }
}
