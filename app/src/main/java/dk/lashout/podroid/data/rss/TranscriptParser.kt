package dk.lashout.podroid.data.rss

import dk.lashout.podroid.domain.model.TranscriptSegment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptParser @Inject constructor() {

    fun parse(content: String, mimeType: String): List<TranscriptSegment> = when {
        mimeType.contains("vtt") -> parseVtt(content)
        mimeType.contains("srt") -> parseSrt(content)
        mimeType.contains("json") -> parseJson(content)
        else -> parsePlainText(content)
    }

    internal fun parseVtt(content: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val cue = parseCue(line, lines, i + 1)
                if (cue != null) { segments += cue.first; i = cue.second } else i++
            } else {
                i++
            }
        }
        return mergeShortSegments(segments)
    }

    private fun parseCue(timeLine: String, lines: List<String>, startIdx: Int): Pair<TranscriptSegment, Int>? {
        val times = timeLine.split("-->")
        val startMs = parseVttTimestamp(times[0].trim())
        val endMs = parseVttTimestamp(times[1].trim().substringBefore(" "))
        val textLines = mutableListOf<String>()
        var i = startIdx
        while (i < lines.size && lines[i].isNotBlank()) { textLines += lines[i++].trim() }
        val text = textLines.joinToString(" ").trim()
        if (text.isBlank() || startMs < 0) return null
        return Pair(TranscriptSegment(startMs, endMs, text), i)
    }

    internal fun parseSrt(content: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val blocks = content.trim().split(Regex("\\n\\s*\\n"))
        for (block in blocks) {
            val lines = block.trim().lines()
            val timeLine = lines.firstOrNull { it.contains("-->") } ?: continue
            val times = timeLine.split("-->")
            val startMs = parseSrtTimestamp(times[0].trim())
            val endMs = parseSrtTimestamp(times[1].trim())
            val text = lines.drop(lines.indexOf(timeLine) + 1).joinToString(" ").trim()
            if (text.isNotBlank() && startMs >= 0) {
                segments += TranscriptSegment(startMs, endMs, text)
            }
        }
        return mergeShortSegments(segments)
    }

    internal fun parseJson(content: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        // Podcast Index JSON: {"version":"1.0.0","segments":[{"startTime":0,"endTime":5,"body":"text"}]}
        val segmentPattern = Regex(""""startTime"\s*:\s*([\d.]+).*?"endTime"\s*:\s*([\d.]+).*?"body"\s*:\s*"([^"]*?)"""", RegexOption.DOT_MATCHES_ALL)
        for (match in segmentPattern.findAll(content)) {
            val startMs = (match.groupValues[1].toDoubleOrNull() ?: continue).toLong() * 1000
            val endMs = (match.groupValues[2].toDoubleOrNull() ?: continue).toLong() * 1000
            val text = match.groupValues[3].replace("\\n", " ").trim()
            if (text.isNotBlank()) segments += TranscriptSegment(startMs, endMs, text)
        }
        return if (segments.isNotEmpty()) segments else parsePlainText(content)
    }

    internal fun parsePlainText(content: String): List<TranscriptSegment> {
        val text = content.trim()
        return if (text.isBlank()) emptyList()
        else listOf(TranscriptSegment(0L, 0L, text))
    }

    // Merge segments shorter than 3 seconds into the next one to reduce noise
    private fun mergeShortSegments(segments: List<TranscriptSegment>): List<TranscriptSegment> {
        if (segments.isEmpty()) return segments
        val result = mutableListOf<TranscriptSegment>()
        var pending: TranscriptSegment? = null
        for (seg in segments) {
            pending = if (pending == null) {
                seg
            } else if (seg.startMs - pending.startMs < 3_000) {
                TranscriptSegment(pending.startMs, seg.endMs, "${pending.text} ${seg.text}")
            } else {
                result += pending
                seg
            }
        }
        if (pending != null) result += pending
        return result
    }

    // HH:MM:SS.mmm or MM:SS.mmm
    private fun parseVttTimestamp(ts: String): Long {
        val parts = ts.split(":")
        return try {
            when (parts.size) {
                3 -> {
                    val ms = parts[2].replace(",", ".").toDouble()
                    parts[0].toLong() * 3_600_000 + parts[1].toLong() * 60_000 + (ms * 1000).toLong()
                }
                2 -> {
                    val ms = parts[1].replace(",", ".").toDouble()
                    parts[0].toLong() * 60_000 + (ms * 1000).toLong()
                }
                else -> -1L
            }
        } catch (_: NumberFormatException) { -1L }
    }

    // HH:MM:SS,mmm
    private fun parseSrtTimestamp(ts: String): Long = parseVttTimestamp(ts.replace(",", "."))
}
