package dk.lashout.podroid.domain.repository

import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.model.PlaylistEntry
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: String): Playlist?
    suspend fun createPlaylist(name: String): Playlist
    suspend fun deletePlaylist(id: String)
    suspend fun makePlaylistPermanent(id: String, name: String): Playlist

    suspend fun getTemporaryPlaylist(): Playlist?
    suspend fun replaceTemporaryPlaylist(episodes: List<Episode>): Playlist

    fun getPlaylistEntries(playlistId: String): Flow<List<PlaylistEntry>>
    suspend fun getPlaylistEntriesSync(playlistId: String): List<PlaylistEntry>
    suspend fun addEpisodeToPlaylist(playlistId: String, episodeId: String)
    suspend fun removeEntry(entryId: String)
    suspend fun reorder(playlistId: String, fromPosition: Int, toPosition: Int)
    suspend fun markEntryPlayedInPlaylist(playlistId: String, episodeId: String)
}
