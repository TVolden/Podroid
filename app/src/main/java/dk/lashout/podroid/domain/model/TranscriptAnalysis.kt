package dk.lashout.podroid.domain.model

data class TalkPoint(val timestampMs: Long, val text: String)

data class TranscriptAnalysis(
    val episodeId: String,
    val summary: String,
    val keyTakeaways: List<String>,
    val topics: List<String>,
    val talkPoints: List<TalkPoint>
)
