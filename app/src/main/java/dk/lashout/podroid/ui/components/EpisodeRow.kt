package dk.lashout.podroid.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.PlaylistEpisodeStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
    onPodcastClick: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onMarkPlayedToggle: (() -> Unit)? = null,
    onCatchUpFromHere: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    playlistStatus: PlaylistEpisodeStatus? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .alpha(if (episode.isPlayed && !isSelectionMode) 0.55f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Leading: checkbox in selection mode, unread dot otherwise
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(24.dp)
                    .align(Alignment.CenterVertically)
            )
        } else {
            val dotColor = when (playlistStatus) {
                PlaylistEpisodeStatus.UNPLAYED         -> MaterialTheme.colorScheme.primary
                PlaylistEpisodeStatus.PLAYED_GLOBALLY_ONLY -> MaterialTheme.colorScheme.tertiary
                PlaylistEpisodeStatus.PLAYED_IN_PLAYLIST   -> MaterialTheme.colorScheme.surfaceVariant
                null -> if (!episode.isPlayed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, end = 6.dp)
                    .size(8.dp)
                    .background(color = dotColor, shape = CircleShape)
            )
        }

        // Podcast artwork — tappable to open the podcast detail (only outside selection mode)
        if (episode.podcastArtworkUrl.isNotBlank()) {
            AsyncImage(
                model = episode.podcastArtworkUrl,
                contentDescription = episode.podcastTitle.ifBlank { null },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (onPodcastClick != null && !isSelectionMode)
                            Modifier.combinedClickable(onClick = onPodcastClick, onLongClick = onLongClick)
                        else Modifier
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            if (episode.podcastTitle.isNotBlank()) {
                Text(
                    text = episode.podcastTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onPodcastClick != null && !isSelectionMode)
                        Modifier.combinedClickable(onClick = onPodcastClick, onLongClick = onLongClick)
                    else Modifier
                )
            }
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (!episode.isPlayed) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatDate(episode.publishedAt)}  \u2022  ${formatDuration(episode.durationSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isSelectionMode) {
            if (trailingContent != null) {
                trailingContent()
            } else if (onAddToPlaylist != null || onMarkPlayedToggle != null || onCatchUpFromHere != null) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Add to playlist") },
                            onClick = { menuExpanded = false; onAddToPlaylist() }
                        )
                    }
                    if (onCatchUpFromHere != null) {
                        DropdownMenuItem(
                            text = { Text("Catch up from here") },
                            onClick = { menuExpanded = false; onCatchUpFromHere() }
                        )
                    }
                    if (onMarkPlayedToggle != null) {
                        DropdownMenuItem(
                            text = { Text(if (episode.isPlayed) "Mark as unplayed" else "Mark as played") },
                            onClick = { menuExpanded = false; onMarkPlayedToggle() }
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMs))

private fun formatDuration(seconds: Long): String {
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
