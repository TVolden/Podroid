package dk.lashout.podroid.domain.model

data class Settings(
    val autoplayNext: Boolean = true,
    val autoplayOrder: AutoplayOrder = AutoplayOrder.NEWER_FIRST,
    /** Mark episode as played when this many seconds or fewer remain. 0 = only on full completion. */
    val autoplayThresholdSeconds: Int = 120,
    /** Include already-played episodes when building a catch-up playlist. */
    val catchUpIncludePlayed: Boolean = false
)

enum class AutoplayOrder {
    NEWER_FIRST,
    OLDER_FIRST
}
