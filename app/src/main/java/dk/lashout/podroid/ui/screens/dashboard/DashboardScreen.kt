package dk.lashout.podroid.ui.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.ui.components.AddToPlaylistDialog
import dk.lashout.podroid.ui.components.EpisodeRow
import dk.lashout.podroid.ui.components.MultiSelectActionBar

@Composable
fun DashboardScreen(
    onEpisodeClick: (Episode) -> Unit,
    onPodcastClick: (podcastId: String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val episodes by viewModel.episodes.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty()
    val episodeForDialog by viewModel.episodeForPlaylistDialog.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    episodeForDialog?.let {
        AddToPlaylistDialog(
            playlists = playlists,
            onSelectPlaylist = { playlist -> viewModel.addToPlaylist(playlist.id) },
            onCreatePlaylist = { name -> viewModel.createPlaylistAndAdd(name) },
            onDismiss = viewModel::dismissPlaylistDialog
        )
    }

    if (episodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Subscribe to podcasts to see new episodes here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp)
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
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
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(episodes, key = { it.id }) { episode ->
                    EpisodeRow(
                        episode = episode,
                        onClick = if (isSelectionMode) {
                            { viewModel.toggleSelection(episode.id) }
                        } else {
                            { onEpisodeClick(episode) }
                        },
                        onPodcastClick = if (isSelectionMode) null else { { onPodcastClick(episode.podcastId) } },
                        onAddToPlaylist = if (isSelectionMode) null else { { viewModel.showPlaylistDialog(episode) } },
                        onMarkPlayedToggle = if (isSelectionMode) null else { { viewModel.togglePlayed(episode) } },
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
