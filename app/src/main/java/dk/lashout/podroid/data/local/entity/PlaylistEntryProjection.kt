package dk.lashout.podroid.data.local.entity

import androidx.room.ColumnInfo

/** Flat projection returned by the playlist-entries JOIN query. */
data class PlaylistEntryProjection(
    // playlist_episodes columns
    val id: String,
    val playlistId: String,
    val episodeId: String,
    val position: Int,
    val addedAt: Long,
    val isPlayedInPlaylist: Boolean,
    // episodes columns (nullable — episode may have been deleted)
    @ColumnInfo(name = "ep_title")            val episodeTitle: String?,
    @ColumnInfo(name = "ep_podcastId")        val podcastId: String?,
    @ColumnInfo(name = "ep_description")      val episodeDescription: String?,
    @ColumnInfo(name = "ep_audioUrl")         val audioUrl: String?,
    @ColumnInfo(name = "ep_durationSeconds")  val durationSeconds: Long?,
    @ColumnInfo(name = "ep_publishedAt")      val publishedAt: Long?,
    @ColumnInfo(name = "ep_isPlayed")         val isPlayed: Boolean?,
    @ColumnInfo(name = "ep_playbackPositionMs") val playbackPositionMs: Long?,
    // podcasts columns
    @ColumnInfo(name = "pod_title")           val podcastTitle: String?,
    @ColumnInfo(name = "pod_artworkUrl")      val podcastArtworkUrl: String?
)
