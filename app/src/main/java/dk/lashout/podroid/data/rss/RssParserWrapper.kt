package dk.lashout.podroid.data.rss

import android.util.Xml
import dk.lashout.podroid.domain.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class RssParserWrapper @Inject constructor() {

    private val dateFormats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    )

    suspend fun fetchPodcastInfo(feedUrl: String): PodcastChannelInfo =
        withContext(Dispatchers.IO) {
            val connection = URL(feedUrl).openConnection() as HttpURLConnection
            connection.apply {
                setRequestProperty("User-Agent", "Podroid/1.0")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                connection.inputStream.use { stream -> parsePodcastInfo(stream) }
            } finally {
                connection.disconnect()
            }
        }

    private fun parsePodcastInfo(stream: InputStream): PodcastChannelInfo {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(stream, null)
        }

        var title = ""
        var author = ""
        var description = ""
        var artworkUrl = ""
        var inItem = false
        var inChannelImage = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase() ?: ""
            val ns = parser.namespace ?: ""
            val isItunes = ns.contains("itunes")

            when (eventType) {
                XmlPullParser.START_TAG -> when {
                    tag == "item" -> inItem = true
                    inItem -> Unit
                    isItunes && tag == "author" -> if (author.isEmpty()) author = safeNextText(parser)
                    isItunes && tag == "image" -> {
                        val href = parser.getAttributeValue(null, "href") ?: ""
                        if (artworkUrl.isEmpty() && href.isNotBlank()) artworkUrl = href
                    }
                    tag == "title" -> if (title.isEmpty()) title = safeNextText(parser)
                    tag == "description" -> if (description.isEmpty()) description = safeNextText(parser)
                    tag == "author" -> if (author.isEmpty()) author = safeNextText(parser)
                    tag == "image" -> inChannelImage = true
                    inChannelImage && tag == "url" -> if (artworkUrl.isEmpty()) artworkUrl = safeNextText(parser)
                }
                XmlPullParser.END_TAG -> when {
                    tag == "item" -> inItem = false
                    tag == "image" && !inItem -> inChannelImage = false
                }
            }

            if (inItem) break
            eventType = parser.next()
        }

        return PodcastChannelInfo(
            title = title.ifBlank { "Unknown Podcast" },
            author = author,
            description = description,
            artworkUrl = artworkUrl
        )
    }

    open suspend fun fetchEpisodes(podcastId: String, feedUrl: String): List<Episode> =
        withContext(Dispatchers.IO) {
            val connection = URL(feedUrl).openConnection() as HttpURLConnection
            connection.apply {
                setRequestProperty("User-Agent", "Podroid/1.0")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                connection.inputStream.use { stream -> parseEpisodes(podcastId, stream) }
            } finally {
                connection.disconnect()
            }
        }

    private fun parseEpisodes(podcastId: String, stream: InputStream): List<Episode> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(stream, null)
        }

        val episodes = mutableListOf<Episode>()
        var inItem = false
        var title = ""
        var description = ""
        var audioUrl = ""
        var guid = ""
        var pubDate = ""
        var duration = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name?.lowercase() ?: ""
            val ns = parser.namespace ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tag == "item") {
                        inItem = true
                        title = ""; description = ""; audioUrl = ""; guid = ""; pubDate = ""; duration = ""
                    } else if (inItem) {
                        when (tag) {
                            "enclosure" ->
                                audioUrl = parser.getAttributeValue(null, "url") ?: ""
                            "guid" -> guid = safeNextText(parser)
                            "title" -> title = safeNextText(parser)
                            "description", "summary" -> description = safeNextText(parser)
                            "pubdate" -> pubDate = safeNextText(parser)
                            "duration" -> if (ns.contains("itunes") || ns.isEmpty()) {
                                duration = safeNextText(parser)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tag == "item" && inItem) {
                        if (audioUrl.isNotBlank()) {
                            val id = if (guid.isNotBlank()) "$podcastId:$guid" else "$podcastId:$title"
                            episodes.add(
                                Episode(
                                    id = id,
                                    podcastId = podcastId,
                                    title = title.ifBlank { "Untitled" },
                                    description = description,
                                    audioUrl = audioUrl,
                                    durationSeconds = DurationParser.parse(duration),
                                    publishedAt = parseDate(pubDate),
                                    isPlayed = false,
                                    playbackPositionMs = 0L
                                )
                            )
                        }
                        inItem = false
                    }
                }
            }
            eventType = parser.next()
        }
        return episodes
    }

    /** Reads the text content of the current tag, guarding against mixed-content / empty tags. */
    private fun safeNextText(parser: XmlPullParser): String = try {
        parser.nextText() ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun parseDate(pubDate: String?): Long {
        if (pubDate.isNullOrBlank()) return System.currentTimeMillis()
        for (format in dateFormats) {
            try {
                return format.parse(pubDate.trim())?.time ?: continue
            } catch (_: Exception) {
                // try next format
            }
        }
        return System.currentTimeMillis()
    }
}
