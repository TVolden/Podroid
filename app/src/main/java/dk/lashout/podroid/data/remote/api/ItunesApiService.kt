package dk.lashout.podroid.data.remote.api

import dk.lashout.podroid.data.remote.dto.ItunesSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApiService {

    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("limit") limit: Int = 25
    ): ItunesSearchResponse
}
