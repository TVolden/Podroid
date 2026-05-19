package dk.lashout.podroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dk.lashout.podroid.data.local.entity.TranscriptEntity
import dk.lashout.podroid.data.local.entity.TranscriptSearchResult
import dk.lashout.podroid.data.local.entity.TranscriptSegmentEntity
import dk.lashout.podroid.data.local.entity.TranscriptSegmentFts
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Query("SELECT * FROM transcripts WHERE episodeId = :episodeId")
    fun observe(episodeId: String): Flow<TranscriptEntity?>

    @Query("SELECT * FROM transcripts WHERE episodeId = :episodeId")
    suspend fun get(episodeId: String): TranscriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transcript: TranscriptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSegments(segments: List<TranscriptSegmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFtsRows(rows: List<TranscriptSegmentFts>)

    @Query("SELECT * FROM transcript_segments WHERE episodeId = :episodeId ORDER BY startMs")
    suspend fun getSegments(episodeId: String): List<TranscriptSegmentEntity>

    @Query("""
        SELECT episodeId, startMs, text
        FROM transcript_segments_fts
        WHERE text MATCH :query
        ORDER BY episodeId, startMs
        LIMIT 200
    """)
    suspend fun search(query: String): List<TranscriptSearchResult>

    @Query("DELETE FROM transcript_segments WHERE episodeId = :episodeId")
    suspend fun deleteSegments(episodeId: String)

    @Query("DELETE FROM transcript_segments_fts WHERE episodeId = :episodeId")
    suspend fun deleteFtsRows(episodeId: String)
}
