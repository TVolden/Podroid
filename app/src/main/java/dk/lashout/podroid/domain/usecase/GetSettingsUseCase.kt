package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Settings
import dk.lashout.podroid.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<Settings> = settingsRepository.getSettings()
}
