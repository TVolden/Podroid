package dk.lashout.podroid.domain.model

enum class PlaylistEpisodeStatus {
    /** Not played in this playlist or globally. */
    UNPLAYED,
    /** Played globally (e.g. via another playlist or marked manually) but not yet played in this playlist. */
    PLAYED_GLOBALLY_ONLY,
    /** Played through this playlist — also marked played globally. */
    PLAYED_IN_PLAYLIST
}
