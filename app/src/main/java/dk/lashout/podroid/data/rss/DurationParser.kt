package dk.lashout.podroid.data.rss

object DurationParser {

    /** Parses iTunes duration strings to total seconds.
     *  Accepted formats: "HH:MM:SS", "MM:SS", plain seconds as integer string. */
    fun parse(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val trimmed = raw.trim()
        return when {
            trimmed.contains(":") -> {
                val parts = trimmed.split(":").map { it.toLongOrNull() ?: 0L }
                when (parts.size) {
                    3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                    2 -> parts[0] * 60 + parts[1]
                    else -> 0L
                }
            }
            else -> trimmed.toLongOrNull() ?: 0L
        }
    }
}
