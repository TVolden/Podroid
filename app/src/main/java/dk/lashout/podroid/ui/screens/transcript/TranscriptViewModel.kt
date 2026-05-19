package dk.lashout.podroid.ui.screens.transcript

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.lashout.podroid.data.local.entity.TranscriptEntity
import dk.lashout.podroid.data.repository.TranscriptRepositoryImpl.Companion.STATUS_ANALYSED
import dk.lashout.podroid.data.repository.TranscriptRepositoryImpl.Companion.STATUS_ANALYSIS_FAILED
import dk.lashout.podroid.domain.model.PlayerState
import dk.lashout.podroid.domain.model.TalkPoint
import dk.lashout.podroid.domain.model.TranscriptSegment
import dk.lashout.podroid.domain.repository.EpisodeRepository
import dk.lashout.podroid.domain.usecase.AnalyseTranscriptUseCase
import dk.lashout.podroid.domain.usecase.GetTranscriptUseCase
import dk.lashout.podroid.service.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TranscriptUiState {
    object Loading : TranscriptUiState
    object NoTranscript : TranscriptUiState
    data class Ready(
        val segments: List<TranscriptSegment>,
        val summary: String?,
        val keyTakeaways: List<String>,
        val topics: List<String>,
        val talkPoints: List<TalkPoint>,
        val isAnalysing: Boolean,
        val analysisStatus: String?
    ) : TranscriptUiState
}

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTranscript: GetTranscriptUseCase,
    private val analyseTranscript: AnalyseTranscriptUseCase,
    private val episodeRepository: EpisodeRepository,
    private val mediaController: PlaybackController
) : ViewModel() {

    val episodeId: String = android.net.Uri.decode(checkNotNull(savedStateHandle["episodeId"]))

    val playerState: StateFlow<PlayerState> = mediaController.playerState

    private val _uiState = MutableStateFlow<TranscriptUiState>(TranscriptUiState.Loading)
    val uiState: StateFlow<TranscriptUiState> = _uiState.asStateFlow()

    private val _isAnalysing = MutableStateFlow(false)

    private val gson = Gson()

    init {
        viewModelScope.launch { loadTranscript() }
        getTranscript.observe(episodeId).onEach { entity ->
            refreshState(entity)
        }.launchIn(viewModelScope)
    }

    private suspend fun loadTranscript() {
        val segments = getTranscript.segments(episodeId)
        if (segments.isEmpty()) {
            val episode = episodeRepository.getEpisodeById(episodeId)
            if (episode?.transcriptUrl != null) {
                getTranscript.fetchAndStore(episode)
            } else {
                _uiState.value = TranscriptUiState.NoTranscript
            }
        }
    }

    private fun refreshState(entity: TranscriptEntity?) {
        viewModelScope.launch {
            val segments = getTranscript.segments(episodeId)
            if (segments.isEmpty() && entity == null) {
                _uiState.value = TranscriptUiState.NoTranscript
                return@launch
            }
            _uiState.value = TranscriptUiState.Ready(
                segments = segments,
                summary = entity?.summary,
                keyTakeaways = parseJsonArray(entity?.keyTakeaways),
                topics = parseJsonArray(entity?.topics),
                talkPoints = parseTalkPoints(entity?.talkPoints),
                isAnalysing = _isAnalysing.value,
                analysisStatus = entity?.status
            )
        }
    }

    fun requestAnalysis() {
        if (_isAnalysing.value) return
        _isAnalysing.value = true
        updateIsAnalysing(true)
        viewModelScope.launch {
            analyseTranscript(episodeId)
            _isAnalysing.value = false
            updateIsAnalysing(false)
        }
    }

    fun seekTo(ms: Long) = mediaController.seekTo(ms)

    private fun updateIsAnalysing(value: Boolean) {
        val current = _uiState.value
        if (current is TranscriptUiState.Ready) {
            _uiState.value = current.copy(isAnalysing = value)
        }
    }

    private fun parseJsonArray(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
        }.getOrDefault(emptyList())
    }

    private fun parseTalkPoints(json: String?): List<TalkPoint> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val raw = gson.fromJson<List<Map<String, Any>>>(
                json, object : TypeToken<List<Map<String, Any>>>() {}.type
            )
            raw.mapNotNull { m ->
                val ms = (m["timestamp_ms"] as? Double)?.toLong() ?: return@mapNotNull null
                val text = m["text"] as? String ?: return@mapNotNull null
                TalkPoint(ms, text)
            }
        }.getOrDefault(emptyList())
    }
}
