package dk.lashout.podroid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    @ColumnInfo(name = "episodeCount") val episodeCount: Int
)
