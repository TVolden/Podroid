package dk.lashout.podroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey val episodeId: String,
    /** "segments_only" | "analysed" | "analysis_failed" */
    val status: String,
    val summary: String? = null,
    val keyTakeaways: String? = null,
    val topics: String? = null,
    val talkPoints: String? = null,
    val analysedAt: Long? = null
)
