package dk.lashout.podroid.data.remote.api

import dk.lashout.podroid.data.remote.dto.PodcastIndexEpisodesResponse
import dk.lashout.podroid.data.remote.dto.PodcastIndexSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PodcastIndexApiService {

    @GET("api/1.0/search/byterm")
    suspend fun searchPodcasts(
        @Query("q") query: String,
        @Query("max") max: Int = 5
    ): PodcastIndexSearchResponse

    @GET("api/1.0/episodes/byfeedid")
    suspend fun getEpisodesByFeedId(
        @Query("id") feedId: Long,
        @Query("max") max: Int = 100
    ): PodcastIndexEpisodesResponse
}
