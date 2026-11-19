package dk.lashout.podroid.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

class PodcastIndexAuthInterceptor(
    private val apiKey: String,
    private val apiSecret: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val timestamp = System.currentTimeMillis() / 1000L
        val request = chain.request().newBuilder()
            .header("X-Auth-Key", apiKey)
            .header("X-Auth-Date", timestamp.toString())
            .header("Authorization", sha1("$apiKey$apiSecret$timestamp"))
            .header("User-Agent", "Podroid/1.0")
            .build()
        return chain.proceed(request)
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
