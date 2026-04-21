package dk.lashout.podroid.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.lashout.podroid.domain.model.AutoplayOrder
import dk.lashout.podroid.domain.model.Episode
import dk.lashout.podroid.domain.model.PlayerState
import dk.lashout.podroid.domain.repository.EpisodeRepository
import dk.lashout.podroid.domain.repository.PlaylistRepository
import dk.lashout.podroid.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeRepository: EpisodeRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val currentPlayback: CurrentPlaybackRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionPollingJob: Job? = null
    private var currentEpisode: Episode? = null

    fun connect() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
            startPositionPolling()
        }, Executors.newSingleThreadExecutor())
    }

    fun disconnect() {
        positionPollingJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    // ── playback commands ─────────────────────────────────────────────────────

    /** Play episode directly — creates a temporary playlist from its subscription. */
    fun playEpisodeFromSubscription(episode: Episode) {
        scope.launch(Dispatchers.IO) {
            val settings = settingsRepository.getSettings().first()
            val allEpisodes = episodeRepository.getEpisodesForPodcast(episode.podcastId).first()
            val sorted = when (settings.autoplayOrder) {
                AutoplayOrder.NEWER_FIRST -> allEpisodes.sortedByDescending { it.publishedAt }
                AutoplayOrder.OLDER_FIRST -> allEpisodes.sortedBy { it.publishedAt }
            }
            val playlist = playlistRepository.replaceTemporaryPlaylist(sorted)
            val idx = sorted.indexOfFirst { it.id == episode.id }.coerceAtLeast(0)
            currentPlayback.setActive(playlist.id, idx)
            launch(Dispatchers.Main) { playEpisode(episode) }
        }
    }

    /** Catch up from the given episode: builds a temp playlist of this episode + all newer ones. */
    fun catchUpFromHere(episode: Episode) {
        scope.launch(Dispatchers.IO) {
            val settings = settingsRepository.getSettings().first()
            val allEpisodes = episodeRepository.getEpisodesForPodcast(episode.podcastId).first()
            val catchUp = allEpisodes
                .filter { it.publishedAt >= episode.publishedAt }
                .filter { !it.isPlayed || settings.catchUpIncludePlayed }
                .sortedBy { it.publishedAt }
            val playlist = playlistRepository.replaceTemporaryPlaylist(catchUp)
            currentPlayback.setActive(playlist.id, 0)
            launch(Dispatchers.Main) { playEpisode(episode) }
        }
    }

    /** Play a named (or temporary) playlist starting at a specific episode. */
    fun playPlaylist(playlistId: String, episode: Episode, startIndex: Int) {
        currentPlayback.setActive(playlistId, startIndex)
        playEpisode(episode)
    }

    /** Persist the current temporary playlist under a new name. */
    fun saveCurrentPlaylist(name: String) {
        scope.launch {
            val id = currentPlayback.activePlaylistId.value ?: return@launch
            playlistRepository.makePlaylistPermanent(id, name)
        }
    }

    fun playEpisode(episode: Episode) {
        currentEpisode = episode
        val mediaItem = MediaItem.Builder()
            .setUri(episode.audioUrl)
            .setMediaId(episode.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(episode.podcastTitle)
                    .setArtworkUri(
                        if (episode.podcastArtworkUrl.isNotBlank())
                            android.net.Uri.parse(episode.podcastArtworkUrl)
                        else null
                    )
                    .build()
            )
            .build()
        controller?.setMediaItem(mediaItem, episode.playbackPositionMs)
        controller?.prepare()
        controller?.play()
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun skipBack(ms: Long = 15_000) { controller?.let { seekTo(maxOf(0, it.currentPosition - ms)) } }
    fun skipForward(ms: Long = 30_000) { controller?.let { seekTo(it.currentPosition + ms) } }
    fun setSpeed(speed: Float) { controller?.setPlaybackParameters(PlaybackParameters(speed)) }

    // ── player listener ───────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { updateState() }
        override fun onPlaybackStateChanged(playbackState: Int) { updateState() }
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) { updateState() }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId ?: return
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                scope.launch {
                    val episode = episodeRepository.getEpisodeWithPodcast(mediaId)
                    if (episode != null) { currentEpisode = episode; updateState() }
                }
            }
        }
    }

    private fun updateState() {
        val c = controller ?: return
        _playerState.value = PlayerState(
            currentEpisode = currentEpisode,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition,
            durationMs = c.duration.coerceAtLeast(0L),
            playbackSpeed = c.playbackParameters.speed,
            bufferedPositionMs = c.bufferedPosition
        )
    }

    private fun startPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = scope.launch {
            while (isActive) { updateState(); delay(500) }
        }
    }
}
