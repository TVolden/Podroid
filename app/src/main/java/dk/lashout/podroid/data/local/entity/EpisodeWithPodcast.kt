package dk.lashout.podroid.data.local.entity

import androidx.room.ColumnInfo

/** Room query-result projection used when joining episodes with their parent podcast. */
data class EpisodeWithPodcast(
    val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val durationSeconds: Long,
    val publishedAt: Long,
    val isPlayed: Boolean,
    val playbackPositionMs: Long,
    val playedAt: Long,
    @ColumnInfo(name = "podcast_title") val podcastTitle: String,
    @ColumnInfo(name = "podcast_artwork_url") val podcastArtworkUrl: String
)
