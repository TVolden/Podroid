package dk.lashout.podroid.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PodcastIndexSearchResponse(
    val status: String,
    val feeds: List<PodcastIndexFeed> = emptyList(),
    val count: Int = 0
)

data class PodcastIndexFeed(
    val id: Long,
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val image: String = "",
    val author: String = "",
    val episodeCount: Int = 0
)

data class PodcastIndexEpisodesResponse(
    val status: String,
    val items: List<PodcastIndexEpisode> = emptyList(),
    val count: Int = 0
)

data class PodcastIndexEpisode(
    val id: Long,
    val title: String = "",
    val description: String = "",
    val guid: String = "",
    val datePublished: Long = 0L,
    @SerializedName("enclosureUrl") val enclosureUrl: String = "",
    val duration: Int = 0,
    val image: String = "",
    val feedTitle: String = "",
    val feedImage: String = "",
    val feedId: Long = 0L
)
