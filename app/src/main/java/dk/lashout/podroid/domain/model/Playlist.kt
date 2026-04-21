package dk.lashout.podroid.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val isTemporary: Boolean,
    val createdAt: Long,
    val episodeCount: Int = 0
)
