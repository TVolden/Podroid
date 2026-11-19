package dk.lashout.podroid.ui.screens.explore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.EpisodeGroup
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.ui.components.AddToPlaylistDialog
import dk.lashout.podroid.ui.components.EpisodeRow
import dk.lashout.podroid.ui.components.PodcastCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPodcastClick: (Podcast) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val episodeState by viewModel.episodeState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val episodeForPlaylistDialog by viewModel.episodeForPlaylistDialog.collectAsState()

    var searchActive by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }

    LaunchedEffect(importState) {
        if (importState is ImportState.Success) {
            showImportDialog = false
            importUrl = ""
            onPodcastClick((importState as ImportState.Success).podcast)
            viewModel.clearImportState()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = { searchActive = false },
            active = searchActive,
            onActiveChange = { searchActive = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = {
                Text(if (searchMode == SearchMode.PODCASTS) "Search podcasts…" else "Search episodes…")
            },
            trailingIcon = {
                if (!searchActive && searchMode == SearchMode.PODCASTS) {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = "Import by RSS URL")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {}

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = searchMode == SearchMode.PODCASTS,
                onClick = { viewModel.setSearchMode(SearchMode.PODCASTS) },
                label = { Text("Podcasts") }
            )
            FilterChip(
                selected = searchMode == SearchMode.EPISODES,
                onClick = { viewModel.setSearchMode(SearchMode.EPISODES) },
                label = { Text("Episodes") }
            )
        }

        when (searchMode) {
            SearchMode.PODCASTS -> PodcastSearchContent(
                uiState = uiState,
                onPodcastClick = onPodcastClick
            )
            SearchMode.EPISODES -> EpisodeSearchContent(
                state = episodeState,
                onEpisodeClick = { episode, podcast ->
                    viewModel.prepareEpisode(episode, podcast)
                    onEpisodeClick(episode)
                },
                onAddToPlaylist = viewModel::showEpisodePlaylistDialog
            )
        }
    }

    if (showImportDialog) {
        val isLoading = importState is ImportState.Loading
        val errorMessage = (importState as? ImportState.Error)?.message

        AlertDialog(
            onDismissRequest = {
                if (!isLoading) {
                    showImportDialog = false
                    importUrl = ""
                    viewModel.clearImportState()
                }
            },
            title = { Text("Import podcast") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = {
                            importUrl = it
                            if (importState is ImportState.Error) viewModel.clearImportState()
                        },
                        label = { Text("RSS feed URL") },
                        placeholder = { Text("https://example.com/feed.xml") },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { msg ->
                            { Text(msg, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.importFromUrl(importUrl) },
                    enabled = importUrl.isNotBlank() && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        importUrl = ""
                        viewModel.clearImportState()
                    },
                    enabled = !isLoading
                ) { Text("Cancel") }
            }
        )
    }

    episodeForPlaylistDialog?.let { (_, _) ->
        AddToPlaylistDialog(
            playlists = playlists,
            onSelectPlaylist = { viewModel.addEpisodeToPlaylist(it.id) },
            onCreatePlaylist = viewModel::createPlaylistAndAddEpisode,
            onDismiss = viewModel::dismissEpisodePlaylistDialog
        )
    }
}

@Composable
private fun PodcastSearchContent(
    uiState: ExploreUiState,
    onPodcastClick: (Podcast) -> Unit
) {
    when (uiState) {
        is ExploreUiState.Idle -> CentredMessage("Search for your favourite podcasts")
        is ExploreUiState.Loading -> LoadingIndicator()
        is ExploreUiState.Results -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.podcasts, key = { it.id }) { podcast ->
                PodcastCard(podcast = podcast, onClick = { onPodcastClick(podcast) })
            }
        }
        is ExploreUiState.Error -> CentredMessage(uiState.message, isError = true)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeSearchContent(
    state: EpisodeSearchState,
    onEpisodeClick: (Episode, Podcast) -> Unit,
    onAddToPlaylist: (Episode, Podcast) -> Unit
) {
    when (state) {
        is EpisodeSearchState.Idle -> CentredMessage("Search for episodes across podcasts")
        is EpisodeSearchState.Loading -> LoadingIndicator()
        is EpisodeSearchState.Results -> {
            if (state.groups.isEmpty()) {
                CentredMessage("No episodes found")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    state.groups.forEach { group ->
                        stickyHeader(key = "header_${group.podcast.id}") {
                            PodcastGroupHeader(group.podcast)
                        }
                        items(group.episodes, key = { "${group.podcast.id}_${it.id}" }) { episode ->
                            EpisodeRow(
                                episode = episode,
                                onClick = { onEpisodeClick(episode, group.podcast) },
                                onAddToPlaylist = { onAddToPlaylist(episode, group.podcast) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        is EpisodeSearchState.Error -> CentredMessage(state.message, isError = true)
    }
}

@Composable
private fun PodcastGroupHeader(podcast: Podcast) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (podcast.artworkUrl.isNotBlank()) {
            AsyncImage(
                model = podcast.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            )
        }
        Text(
            text = podcast.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CentredMessage(text: String, isError: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
