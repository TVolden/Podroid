package dk.lashout.podroid.domain.model

data class Episode(
    val id: String,
    val podcastId: String,
    val podcastTitle: String = "",
    val podcastArtworkUrl: String = "",
    val title: String,
    val description: String,
    val audioUrl: String,
    val durationSeconds: Long,
    val publishedAt: Long,
    val isPlayed: Boolean = false,
    val playbackPositionMs: Long = 0L
)
