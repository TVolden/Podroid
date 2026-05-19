package dk.lashout.podroid.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(val parts: List<GeminiPart>)

data class GeminiPart(val text: String)

data class GeminiGenerationConfig(
    @SerializedName("responseMimeType") val responseMimeType: String = "application/json"
)

data class GeminiResponse(val candidates: List<GeminiCandidate>?)

data class GeminiCandidate(val content: GeminiContent?)
