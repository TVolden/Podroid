package dk.lashout.podroid.ui.screens.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.usecase.GetSubscriptionsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    getSubscriptions: GetSubscriptionsUseCase
) : ViewModel() {

    val subscriptions: StateFlow<List<Podcast>> = getSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
