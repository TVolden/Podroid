package dk.lashout.podroid.ui.screens.transcript

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dk.lashout.podroid.data.repository.TranscriptRepositoryImpl.Companion.STATUS_ANALYSED
import dk.lashout.podroid.data.repository.TranscriptRepositoryImpl.Companion.STATUS_ANALYSIS_FAILED
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptScreen(
    onBack: () -> Unit,
    viewModel: TranscriptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transcript") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is TranscriptUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is TranscriptUiState.NoTranscript -> {
                    Text(
                        text = "No transcript available for this episode",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                }
                is TranscriptUiState.Ready -> {
                    var selectedTab by remember { mutableIntStateOf(0) }
                    val tabs = buildList {
                        add("Transcript")
                        if (state.summary != null) add("Summary")
                        if (state.keyTakeaways.isNotEmpty()) add("Key Points")
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]))
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }

                        when (tabs.getOrNull(selectedTab)) {
                            "Transcript" -> TranscriptTab(
                                state = state,
                                positionMs = playerState.positionMs,
                                onSegmentClick = viewModel::seekTo,
                                onRequestAnalysis = viewModel::requestAnalysis
                            )
                            "Summary" -> SummaryTab(summary = state.summary ?: "")
                            "Key Points" -> KeyPointsTab(
                                keyTakeaways = state.keyTakeaways,
                                talkPoints = state.talkPoints,
                                onTalkPointClick = viewModel::seekTo
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptTab(
    state: TranscriptUiState.Ready,
    positionMs: Long,
    onSegmentClick: (Long) -> Unit,
    onRequestAnalysis: () -> Unit
) {
    val listState = rememberLazyListState()
    val currentIndex by remember(positionMs) {
        derivedStateOf {
            state.segments.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0)
        }
    }

    LaunchedEffect(currentIndex) {
        if (state.segments.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (state.analysisStatus == null && !state.isAnalysing) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = onRequestAnalysis) { Text("Analyse episode") }
                }
            }
        }
        if (state.isAnalysing) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analysing…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (state.analysisStatus == STATUS_ANALYSIS_FAILED) {
            item {
                Text(
                    "Analysis failed. Try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        itemsIndexed(state.segments, key = { idx, _ -> idx }) { idx, segment ->
            val isCurrent = idx == currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .clickable { onSegmentClick(segment.startMs) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = formatTimestamp(segment.startMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.width(52.dp)
                )
                Text(
                    text = segment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(thickness = 0.5.dp)
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun SummaryTab(summary: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun KeyPointsTab(
    keyTakeaways: List<String>,
    talkPoints: List<dk.lashout.podroid.domain.model.TalkPoint>,
    onTalkPointClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        if (keyTakeaways.isNotEmpty()) {
            item {
                Text("Key Takeaways", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(keyTakeaways.size) { idx ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium)
                    Text(keyTakeaways[idx], style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (talkPoints.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Talk Points", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(talkPoints.size) { idx ->
                val tp = talkPoints[idx]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTalkPointClick(tp.timestampMs) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SuggestionChip(
                        onClick = { onTalkPointClick(tp.timestampMs) },
                        label = { Text(formatTimestamp(tp.timestampMs), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        tp.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

private fun formatTimestamp(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
