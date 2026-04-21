package dk.lashout.podroid.data.repository

import dk.lashout.podroid.data.local.dao.PlaylistDao
import dk.lashout.podroid.data.local.dao.PlaylistEpisodeDao
import dk.lashout.podroid.data.local.entity.PlaylistEntryProjection
import dk.lashout.podroid.data.local.entity.PlaylistEpisodeEntity
import dk.lashout.podroid.data.local.entity.PlaylistEntity
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.Playlist
import dk.lashout.podroid.domain.model.PlaylistEntry
import dk.lashout.podroid.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistEpisodeDao: PlaylistEpisodeDao
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllWithCount().map { list ->
            list.map { it.playlist.toDomain(it.episodeCount) }
        }

    override suspend fun getPlaylistById(id: String): Playlist? =
        playlistDao.getById(id)?.toDomain()

    override suspend fun createPlaylist(name: String): Playlist {
        val entity = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            isTemporary = false,
            createdAt = System.currentTimeMillis()
        )
        playlistDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun deletePlaylist(id: String) = playlistDao.delete(id)

    override suspend fun makePlaylistPermanent(id: String, name: String): Playlist {
        playlistDao.makePermanent(id, name)
        return playlistDao.getById(id)?.toDomain(0) ?: Playlist(id, name, false, System.currentTimeMillis())
    }

    override suspend fun getTemporaryPlaylist(): Playlist? =
        playlistDao.getTemporary()?.toDomain()

    override suspend fun replaceTemporaryPlaylist(episodes: List<Episode>): Playlist {
        playlistDao.deleteTemporary()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(id = id, name = "Now Playing", isTemporary = true, createdAt = now)
        playlistDao.insert(entity)
        val entries = episodes.mapIndexed { idx, ep ->
            PlaylistEpisodeEntity(
                id = UUID.randomUUID().toString(),
                playlistId = id,
                episodeId = ep.id,
                position = idx,
                addedAt = now,
                isPlayedInPlaylist = false
            )
        }
        if (entries.isNotEmpty()) playlistEpisodeDao.insertAll(entries)
        return entity.toDomain(episodes.size)
    }

    override fun getPlaylistEntries(playlistId: String): Flow<List<PlaylistEntry>> =
        playlistEpisodeDao.getEntriesForPlaylist(playlistId).map { list ->
            list.mapNotNull { it.toDomain() }
        }

    override suspend fun getPlaylistEntriesSync(playlistId: String): List<PlaylistEntry> =
        playlistEpisodeDao.getEntriesForPlaylistSync(playlistId).mapNotNull { it.toDomain() }

    override suspend fun addEpisodeToPlaylist(playlistId: String, episodeId: String) {
        val maxPos = playlistEpisodeDao.getMaxPosition(playlistId) ?: -1
        playlistEpisodeDao.insert(
            PlaylistEpisodeEntity(
                id = UUID.randomUUID().toString(),
                playlistId = playlistId,
                episodeId = episodeId,
                position = maxPos + 1,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeEntry(entryId: String) = playlistEpisodeDao.delete(entryId)

    override suspend fun reorder(playlistId: String, fromPosition: Int, toPosition: Int) {
        val items = playlistEpisodeDao.getEntriesForPlaylistSync(playlistId)
            .sortedBy { it.position }
            .toMutableList()
        if (fromPosition !in items.indices || toPosition !in items.indices) return
        val moved = items.removeAt(fromPosition)
        items.add(toPosition, moved)
        items.forEachIndexed { idx, item ->
            playlistEpisodeDao.updatePosition(item.id, idx)
        }
    }

    override suspend fun markEntryPlayedInPlaylist(playlistId: String, episodeId: String) =
        playlistEpisodeDao.markPlayedInPlaylist(playlistId, episodeId)

    // ── mapping helpers ───────────────────────────────────────────────────────

    private fun PlaylistEntity.toDomain(count: Int = 0) =
        Playlist(id = id, name = name, isTemporary = isTemporary, createdAt = createdAt, episodeCount = count)

    private fun PlaylistEntryProjection.toDomain(): PlaylistEntry? {
        // Drop entries whose episode was deleted
        val title = episodeTitle ?: return null
        val episode = Episode(
            id = episodeId,
            podcastId = podcastId ?: "",
            podcastTitle = podcastTitle ?: "",
            podcastArtworkUrl = podcastArtworkUrl ?: "",
            title = title,
            description = episodeDescription ?: "",
            audioUrl = audioUrl ?: "",
            durationSeconds = durationSeconds ?: 0L,
            publishedAt = publishedAt ?: 0L,
            isPlayed = isPlayed ?: false,
            playbackPositionMs = playbackPositionMs ?: 0L
        )
        return PlaylistEntry(
            id = id,
            playlistId = playlistId,
            episode = episode,
            position = position,
            addedAt = addedAt,
            isPlayedInPlaylist = isPlayedInPlaylist
        )
    }
}
