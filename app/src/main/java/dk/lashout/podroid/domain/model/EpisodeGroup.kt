package dk.lashout.podroid.domain.model

data class EpisodeGroup(
    val podcast: Podcast,
    val episodes: List<Episode>
)
