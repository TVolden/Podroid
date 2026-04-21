package dk.lashout.podroid.ui.screens.podcastdetail

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.ui.components.AddToPlaylistDialog
import dk.lashout.podroid.ui.components.EpisodeRow
import dk.lashout.podroid.ui.components.MultiSelectActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    podcast: Podcast?,
    onBack: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onCatchUpFromHere: (Episode) -> Unit = {},
    viewModel: PodcastDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(podcast) {
        podcast?.let { viewModel.setPodcast(it) }
    }

    val currentPodcast by viewModel.podcast.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty()
    val episodeForDialog by viewModel.episodeForPlaylistDialog.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val displayPodcast = currentPodcast ?: podcast

    episodeForDialog?.let {
        AddToPlaylistDialog(
            playlists = playlists,
            onSelectPlaylist = { playlist -> viewModel.addToPlaylist(playlist.id) },
            onCreatePlaylist = { name -> viewModel.createPlaylistAndAdd(name) },
            onDismiss = viewModel::dismissPlaylistDialog
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayPodcast?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSelectionMode) {
                MultiSelectActionBar(
                    selectedCount = selectedIds.size,
                    totalCount = episodes.size,
                    onMarkAsPlayed = viewModel::markSelectedAsPlayed,
                    onMarkAsUnplayed = viewModel::markSelectedAsUnplayed,
                    onSelectAll = viewModel::selectAll,
                    onClearSelection = viewModel::clearSelection
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Podcast header
                displayPodcast?.let { p ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = p.artworkUrl,
                                contentDescription = p.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    p.author,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (p.isSubscribed) {
                                    OutlinedButton(onClick = viewModel::unsubscribe) {
                                        Text("Unsubscribe")
                                    }
                                } else {
                                    Button(onClick = viewModel::subscribe) {
                                        Text("Subscribe")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = p.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Loading episodes\u2026")
                        }
                    }
                }

                items(episodes, key = { it.id }) { episode ->
                    EpisodeRow(
                        episode = episode,
                        onClick = if (isSelectionMode) {
                            { viewModel.toggleSelection(episode.id) }
                        } else {
                            { onEpisodeClick(episode) }
                        },
                        onAddToPlaylist = if (isSelectionMode) null else { { viewModel.showPlaylistDialog(episode) } },
                        onMarkPlayedToggle = if (isSelectionMode) null else { { viewModel.togglePlayed(episode) } },
                        onCatchUpFromHere = if (isSelectionMode) null else { { onCatchUpFromHere(episode) } },
                        onLongClick = if (!isSelectionMode) { { viewModel.toggleSelection(episode.id) } } else null,
                        isSelected = episode.id in selectedIds,
                        isSelectionMode = isSelectionMode
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
