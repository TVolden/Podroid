package dk.lashout.podroid.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.AutoplayOrder
import dk.lashout.podroid.domain.model.Settings
import dk.lashout.podroid.domain.usecase.GetSettingsUseCase
import dk.lashout.podroid.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<Settings> = getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setAutoplayNext(enabled: Boolean) {
        viewModelScope.launch { updateSettings(settings.value.copy(autoplayNext = enabled)) }
    }

    fun setAutoplayOrder(order: AutoplayOrder) {
        viewModelScope.launch { updateSettings(settings.value.copy(autoplayOrder = order)) }
    }

    fun setAutoplayThreshold(seconds: Int) {
        viewModelScope.launch { updateSettings(settings.value.copy(autoplayThresholdSeconds = seconds)) }
    }

    fun setCatchUpIncludePlayed(enabled: Boolean) {
        viewModelScope.launch { updateSettings(settings.value.copy(catchUpIncludePlayed = enabled)) }
    }
}
