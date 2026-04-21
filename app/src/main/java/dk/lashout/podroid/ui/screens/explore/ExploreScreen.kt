package dk.lashout.podroid.ui.screens.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dk.lashout.podroid.domain.model.Podcast
import dk.lashout.podroid.ui.components.PodcastCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPodcastClick: (Podcast) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val importState by viewModel.importState.collectAsState()
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
            placeholder = { Text("Search podcasts…") },
            trailingIcon = {
                if (!searchActive) {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = "Import by RSS URL")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {}

        when (val state = uiState) {
            is ExploreUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Search for your favourite podcasts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is ExploreUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ExploreUiState.Results -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.podcasts, key = { it.id }) { podcast ->
                        PodcastCard(podcast = podcast, onClick = { onPodcastClick(podcast) })
                    }
                }
            }
            is ExploreUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
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
                        supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.importFromUrl(importUrl) },
                    enabled = importUrl.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Import")
                    }
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
}
