package dk.lashout.podroid.domain.repository

interface LastPlayedRepository {
    suspend fun getLastEpisodeId(): String?
    suspend fun setLastEpisodeId(episodeId: String)
}
