package dk.lashout.podroid.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.lashout.podroid.domain.repository.LastPlayedRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lastPlayedDataStore by preferencesDataStore(name = "last_played")

@Singleton
class LastPlayedRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LastPlayedRepository {

    private val LAST_EPISODE_ID = stringPreferencesKey("last_episode_id")

    override suspend fun getLastEpisodeId(): String? =
        context.lastPlayedDataStore.data.first()[LAST_EPISODE_ID]

    override suspend fun setLastEpisodeId(episodeId: String) {
        context.lastPlayedDataStore.edit { it[LAST_EPISODE_ID] = episodeId }
    }
}
