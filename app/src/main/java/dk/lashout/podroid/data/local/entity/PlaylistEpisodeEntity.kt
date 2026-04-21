package dk.lashout.podroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_episodes",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("episodeId")]
)
data class PlaylistEpisodeEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val episodeId: String,
    val position: Int,
    val addedAt: Long,
    val isPlayedInPlaylist: Boolean = false
)
