package dk.lashout.podroid.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ItunesSearchResponse(
    @SerializedName("resultCount") val resultCount: Int,
    @SerializedName("results") val results: List<ItunesResult>
)

data class ItunesResult(
    @SerializedName("collectionId") val collectionId: Long,
    @SerializedName("collectionName") val collectionName: String?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("artworkUrl600") val artworkUrl600: String?,
    @SerializedName("artworkUrl100") val artworkUrl100: String?,
    @SerializedName("feedUrl") val feedUrl: String?,
    @SerializedName("trackCount") val trackCount: Int?
)

data class ItunesEpisodeSearchResponse(
    @SerializedName("resultCount") val resultCount: Int,
    @SerializedName("results") val results: List<ItunesEpisodeResult>
)

data class ItunesEpisodeResult(
    @SerializedName("trackId") val trackId: Long = 0L,
    @SerializedName("trackName") val trackName: String = "",
    @SerializedName("collectionId") val collectionId: Long = 0L,
    @SerializedName("collectionName") val collectionName: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("episodeUrl") val episodeUrl: String = "",
    @SerializedName("trackTimeMillis") val trackTimeMillis: Long = 0L,
    @SerializedName("episodeGuid") val episodeGuid: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("artworkUrl600") val artworkUrl600: String = "",
    @SerializedName("feedUrl") val feedUrl: String = ""
)
