package dk.lashout.podroid.domain.model

data class Podcast(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val artworkUrl: String,
    val feedUrl: String,
    val isSubscribed: Boolean = false
)
