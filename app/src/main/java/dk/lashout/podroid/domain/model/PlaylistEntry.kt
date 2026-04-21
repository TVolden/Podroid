package dk.lashout.podroid.domain.model

data class PlaylistEntry(
    val id: String,
    val playlistId: String,
    val episode: Episode,
    val position: Int,
    val addedAt: Long,
    val isPlayedInPlaylist: Boolean
) {
    val status: PlaylistEpisodeStatus get() = when {
        isPlayedInPlaylist  -> PlaylistEpisodeStatus.PLAYED_IN_PLAYLIST
        episode.isPlayed    -> PlaylistEpisodeStatus.PLAYED_GLOBALLY_ONLY
        else                -> PlaylistEpisodeStatus.UNPLAYED
    }
}
