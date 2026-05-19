package dk.lashout.podroid.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4
@Entity(tableName = "transcript_segments_fts")
data class TranscriptSegmentFts(
    val episodeId: String,
    val startMs: Long,
    val text: String
)
