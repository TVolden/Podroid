package dk.lashout.podroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val durationSeconds: Long,
    val publishedAt: Long,
    val isPlayed: Boolean = false,
    val playbackPositionMs: Long = 0L,
    /** Epoch millis when this episode was last marked as played. 0 if never played. */
    val playedAt: Long = 0L
)
