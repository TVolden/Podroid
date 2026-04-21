package dk.lashout.podroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dk.lashout.podroid.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(podcast: PodcastEntity): Long

    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY title ASC")
    fun getSubscriptions(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PodcastEntity?

    @Query("UPDATE podcasts SET isSubscribed = 1 WHERE id = :podcastId")
    suspend fun subscribe(podcastId: String): Int

    @Query("UPDATE podcasts SET isSubscribed = 0 WHERE id = :podcastId")
    suspend fun unsubscribe(podcastId: String): Int
}
