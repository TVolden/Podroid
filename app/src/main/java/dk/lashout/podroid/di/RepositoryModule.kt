package dk.lashout.podroid.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.lashout.podroid.data.repository.EpisodeRepositoryImpl
import dk.lashout.podroid.data.repository.PlaylistRepositoryImpl
import dk.lashout.podroid.data.repository.PodcastRepositoryImpl
import dk.lashout.podroid.data.repository.SettingsRepositoryImpl
import dk.lashout.podroid.domain.repository.EpisodeRepository
import dk.lashout.podroid.domain.repository.PlaylistRepository
import dk.lashout.podroid.domain.repository.PodcastRepository
import dk.lashout.podroid.domain.repository.SettingsRepository
import javax.inject.Singleton

// Note: @Provides is used instead of @Binds intentionally.
// Dagger's BindsMethodValidator crashes with a "KSTypeArgument.type STAR null" error when
// processing @Binds methods under KSP2 (Hilt ≤ 2.51). @Provides is the correct workaround
// until that bug is resolved upstream. Do not convert these to @Binds.
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePodcastRepository(impl: PodcastRepositoryImpl): PodcastRepository = impl

    @Provides
    @Singleton
    fun provideEpisodeRepository(impl: EpisodeRepositoryImpl): EpisodeRepository = impl

    @Provides
    @Singleton
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun providePlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl
}
