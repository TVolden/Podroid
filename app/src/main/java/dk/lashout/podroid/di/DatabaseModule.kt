package dk.lashout.podroid.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dk.lashout.podroid.data.local.AppDatabase
import dk.lashout.podroid.data.local.dao.EpisodeDao
import dk.lashout.podroid.data.local.dao.PlaylistDao
import dk.lashout.podroid.data.local.dao.PlaylistEpisodeDao
import dk.lashout.podroid.data.local.dao.PodcastDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "podroid.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun providePodcastDao(db: AppDatabase): PodcastDao = db.podcastDao()

    @Provides
    fun provideEpisodeDao(db: AppDatabase): EpisodeDao = db.episodeDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun providePlaylistEpisodeDao(db: AppDatabase): PlaylistEpisodeDao = db.playlistEpisodeDao()
}
