package dk.lashout.podroid.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.lashout.podroid.domain.model.AutoplayOrder
import dk.lashout.podroid.domain.model.Settings
import dk.lashout.podroid.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
    private val AUTOPLAY_ORDER = stringPreferencesKey("autoplay_order")
    private val AUTOPLAY_THRESHOLD = intPreferencesKey("autoplay_threshold_seconds")
    private val CATCH_UP_INCLUDE_PLAYED = booleanPreferencesKey("catch_up_include_played")

    override fun getSettings(): Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            autoplayNext = prefs[AUTOPLAY_NEXT] ?: true,
            autoplayOrder = AutoplayOrder.valueOf(
                prefs[AUTOPLAY_ORDER] ?: AutoplayOrder.NEWER_FIRST.name
            ),
            autoplayThresholdSeconds = prefs[AUTOPLAY_THRESHOLD] ?: 120,
            catchUpIncludePlayed = prefs[CATCH_UP_INCLUDE_PLAYED] ?: false
        )
    }

    override suspend fun updateSettings(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[AUTOPLAY_NEXT] = settings.autoplayNext
            prefs[AUTOPLAY_ORDER] = settings.autoplayOrder.name
            prefs[AUTOPLAY_THRESHOLD] = settings.autoplayThresholdSeconds
            prefs[CATCH_UP_INCLUDE_PLAYED] = settings.catchUpIncludePlayed
        }
    }
}
