package dk.lashout.podroid.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.net.Uri
import dk.lashout.podroid.domain.model.Episode

internal fun Episode.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(audioUrl)
    .setMediaId(id)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(podcastTitle)
            .setArtworkUri(if (podcastArtworkUrl.isNotBlank()) Uri.parse(podcastArtworkUrl) else null)
            .build()
    )
    .build()
