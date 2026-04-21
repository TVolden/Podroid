package dk.lashout.podroid.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import dk.lashout.podroid.domain.repository.EpisodeRepository
import dk.lashout.podroid.domain.repository.PlaylistRepository
import dk.lashout.podroid.domain.repository.SettingsRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var episodeRepository: EpisodeRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var currentPlayback: CurrentPlaybackRepository

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private val scope = MainScope()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {}

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val episodeId = player.currentMediaItem?.mediaId ?: return
                scope.launch { handleEpisodeEnded(episodeId) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying && player.playbackState != Player.STATE_ENDED) {
                val episodeId = player.currentMediaItem?.mediaId ?: return
                scope.launch { checkThresholdAndMark(episodeId) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(playerListener)
        mediaSession = MediaLibrarySession.Builder(this, player, object : MediaLibrarySession.Callback {}).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = mediaSession

    override fun onDestroy() {
        player.removeListener(playerListener)
        mediaSession.release()
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    // ── playback event handlers ───────────────────────────────────────────────

    private suspend fun handleEpisodeEnded(episodeId: String) {
        episodeRepository.markAsPlayed(episodeId)

        val playlistId = currentPlayback.activePlaylistId.value
        if (playlistId != null) {
            playlistRepository.markEntryPlayedInPlaylist(playlistId, episodeId)
        }

        val settings = settingsRepository.getSettings().first()
        if (!settings.autoplayNext) return

        val next = if (playlistId != null) {
            val entries = playlistRepository.getPlaylistEntriesSync(playlistId)
            val nextIndex = currentPlayback.activeEpisodeIndex.value + 1
            entries.getOrNull(nextIndex)?.episode?.also { currentPlayback.advanceIndex() }
        } else {
            val episode = episodeRepository.getEpisodeById(episodeId) ?: return
            episodeRepository.getNextEpisodeInPodcast(episode.podcastId, episode.publishedAt, settings.autoplayOrder)
        } ?: return

        val mediaItem = MediaItem.Builder()
            .setUri(next.audioUrl)
            .setMediaId(next.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(next.title)
                    .setArtist(next.podcastTitle)
                    .setArtworkUri(
                        if (next.podcastArtworkUrl.isNotBlank())
                            android.net.Uri.parse(next.podcastArtworkUrl)
                        else null
                    )
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem, next.playbackPositionMs)
        player.prepare()
        player.play()
    }

    private suspend fun checkThresholdAndMark(episodeId: String) {
        val duration = player.duration
        val position = player.currentPosition
        if (duration <= 0) return
        val remainingMs = duration - position
        val settings = settingsRepository.getSettings().first()
        if (settings.autoplayThresholdSeconds > 0 &&
            remainingMs >= 0 &&
            remainingMs <= settings.autoplayThresholdSeconds * 1000L
        ) {
            episodeRepository.markAsPlayed(episodeId)
            val playlistId = currentPlayback.activePlaylistId.value
            if (playlistId != null) {
                playlistRepository.markEntryPlayedInPlaylist(playlistId, episodeId)
            }
        }
    }
}
