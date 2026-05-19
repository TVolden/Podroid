package dk.lashout.podroid.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "transcript_segments", indices = [Index("episodeId")])
data class TranscriptSegmentEntity(
    @PrimaryKey val id: String,
    val episodeId: String,
    val startMs: Long,
    val endMs: Long,
    val text: String
)
