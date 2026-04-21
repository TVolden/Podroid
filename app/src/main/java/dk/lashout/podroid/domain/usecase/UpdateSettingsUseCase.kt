package dk.lashout.podroid.domain.usecase

import dk.lashout.podroid.domain.model.Settings
import dk.lashout.podroid.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: Settings) {
        settingsRepository.updateSettings(settings)
    }
}
