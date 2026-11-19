package dk.lashout.podroid.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.lashout.podroid.BuildConfig
import dk.lashout.podroid.data.remote.PodcastIndexAuthInterceptor
import dk.lashout.podroid.data.remote.api.GeminiApiService
import dk.lashout.podroid.data.remote.api.ItunesApiService
import dk.lashout.podroid.data.remote.api.PodcastIndexApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://itunes.apple.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideItunesApiService(retrofit: Retrofit): ItunesApiService =
        retrofit.create(ItunesApiService::class.java)

    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideGeminiApiService(@Named("gemini") retrofit: Retrofit): GeminiApiService =
        retrofit.create(GeminiApiService::class.java)

    @Provides
    @Singleton
    @Named("podcastindex")
    fun providePodcastIndexOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(PodcastIndexAuthInterceptor(
            BuildConfig.PODCAST_INDEX_API_KEY,
            BuildConfig.PODCAST_INDEX_API_SECRET
        ))
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides
    @Singleton
    @Named("podcastindex")
    fun providePodcastIndexRetrofit(@Named("podcastindex") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.podcastindex.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun providePodcastIndexApiService(@Named("podcastindex") retrofit: Retrofit): PodcastIndexApiService =
        retrofit.create(PodcastIndexApiService::class.java)
}
