package dk.lashout.podroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dk.lashout.podroid.data.local.entity.PlaylistEntity
import dk.lashout.podroid.data.local.entity.PlaylistWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("""
        SELECT p.*, COUNT(pe.id) AS episodeCount
        FROM playlists p
        LEFT JOIN playlist_episodes pe ON p.id = pe.playlistId
        GROUP BY p.id
        ORDER BY p.isTemporary ASC, p.createdAt DESC
    """)
    fun getAllWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE isTemporary = 1 LIMIT 1")
    suspend fun getTemporary(): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE playlists SET name = :name, isTemporary = 0 WHERE id = :id")
    suspend fun makePermanent(id: String, name: String)

    @Query("DELETE FROM playlists WHERE isTemporary = 1")
    suspend fun deleteTemporary()
}
