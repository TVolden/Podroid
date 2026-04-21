package dk.lashout.podroid.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.domain.usecase.ImportPodcastUseCase
import dk.lashout.podroid.domain.usecase.SearchPodcastsUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

sealed interface ImportState {
    object Idle : ImportState
    object Loading : ImportState
    data class Success(val podcast: Podcast) : ImportState
    data class Error(val message: String) : ImportState
}

sealed interface ExploreUiState {
    object Idle : ExploreUiState
    object Loading : ExploreUiState
    data class Results(val podcasts: List<Podcast>) : ExploreUiState
    data class Error(val message: String) : ExploreUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val searchPodcasts: SearchPodcastsUseCase,
    private val importPodcast: ImportPodcastUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Idle)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    init {
        _query
            .debounce(300)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .onEach { q ->
                _uiState.value = ExploreUiState.Loading
                searchPodcasts(q).fold(
                    onSuccess = { _uiState.value = ExploreUiState.Results(it) },
                    onFailure = { _uiState.value = ExploreUiState.Error(it.message ?: "Search failed") }
                )
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(q: String) {
        _query.value = q
        if (q.isBlank()) _uiState.value = ExploreUiState.Idle
    }

    fun importFromUrl(url: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            importPodcast(url).fold(
                onSuccess = { _importState.value = ImportState.Success(it) },
                onFailure = { _importState.value = ImportState.Error(it.message ?: "Import failed") }
            )
        }
    }

    fun clearImportState() { _importState.value = ImportState.Idle }
}
