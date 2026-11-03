package com.vova.glyph.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.vova.glyph.data.Prefs
import com.vova.glyph.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlayerUiState(
    val connected: Boolean = false,
    val trackId: Long? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val error: String? = null,

    val completedTrackId: Long? = null,

    val completionCount: Long = 0,
)

class PlayerConnection(private val context: Context) {
    private val appContext = context.applicationContext
    private val executor = ContextCompat.getMainExecutor(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs = Prefs(appContext)

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private var originalItems: List<MediaItem> = emptyList()

    private var shuffleEnabled: Boolean = prefs.shuffle

    private val _state = MutableStateFlow(PlayerUiState(shuffle = prefs.shuffle))
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.update {
                it.copy(error = error.localizedMessage ?: "Ошибка воспроизведения")
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _state.update { old ->
                    old.copy(
                        completedTrackId = old.trackId,
                        completionCount = old.completionCount + 1,
                    )
                }
            }
        }
    }

    init {
        scope.launch {
            while (isActive) {
                val player = controller
                if (
                    player != null &&
                    (player.isPlaying || player.playbackState == Player.STATE_BUFFERING)
                ) {
                    updateState(player)
                }
                delay(500)
            }
        }
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { connected ->
                        controller = connected

                        connected.shuffleModeEnabled = false
                        connected.addListener(listener)
                        updateState(connected)
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                connected = false,
                                error = error.localizedMessage ?: "Не удалось подключить плеер",
                            )
                        }
                    }
            },
            executor,
        )
    }

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        val player = controller ?: return
        if (tracks.isEmpty()) return
        val items = tracks.map { track -> track.toMediaItem() }
        originalItems = items
        val start = items[startIndex.coerceIn(0, items.lastIndex)]

        val ordered = if (shuffleEnabled) shuffleKeepingFirst(items, start.mediaId) else items
        val index = ordered.indexOfFirst { it.mediaId == start.mediaId }.coerceAtLeast(0)
        player.setMediaItems(ordered, index, 0L)
        player.prepare()
        player.play()
        updateState(player)
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
        updateState(player)
    }

    fun next() {
        val player = controller ?: return
        when {
            player.hasNextMediaItem() -> player.seekToNext()

            shuffleEnabled && originalItems.size > 1 -> {
                startRandomRound(player)
                return
            }
            else -> player.seekTo(0, 0L)
        }
        updateState(player)
    }

    fun previous() {
        val player = controller ?: return
        if (player.hasPreviousMediaItem()) player.seekToPrevious()
        else player.seekTo(0, 0L)
        updateState(player)
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        player.seekTo(positionMs.coerceAtLeast(0))
        updateState(player)
    }

    fun toggleShuffle() {
        val player = controller ?: return
        shuffleEnabled = !shuffleEnabled
        prefs.shuffle = shuffleEnabled

        if (originalItems.isEmpty()) {
            originalItems = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        }
        if (originalItems.isEmpty()) {
            updateState(player)
            return
        }

        val currentId = player.currentMediaItem?.mediaId
        val ordered = if (shuffleEnabled) {
            shuffleKeepingFirst(originalItems, currentId)
        } else {
            originalItems
        }
        applyOrder(player, ordered, currentId)
        updateState(player)
    }

    fun cycleRepeat() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        updateState(player)
    }

    fun stop() {
        controller?.stop()
        updateState(controller)
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun shuffleKeepingFirst(items: List<MediaItem>, firstId: String?): List<MediaItem> {
        if (items.size < 2) return items
        val first = items.firstOrNull { it.mediaId == firstId }
            ?: return items.shuffled()
        return listOf(first) + items.filter { it.mediaId != firstId }.shuffled()
    }

    private fun applyOrder(
        player: Player,
        ordered: List<MediaItem>,
        keepId: String?,
        startPositionMs: Long? = null,
    ) {
        val currentId = keepId ?: player.currentMediaItem?.mediaId
        val position = startPositionMs ?: player.currentPosition.coerceAtLeast(0L)
        val wasPlaying = player.playWhenReady
        val index = ordered.indexOfFirst { it.mediaId == currentId }.coerceAtLeast(0)
        player.setMediaItems(ordered, index, position)
        player.prepare()
        if (wasPlaying) player.play()
    }

    private fun startRandomRound(player: Player) {
        val lastId = player.currentMediaItem?.mediaId
        var ordered = originalItems.shuffled()

        if (ordered.size > 1 && ordered.first().mediaId == lastId) {
            ordered = ordered.drop(1) + ordered.first()
        }
        applyOrder(player, ordered, ordered.firstOrNull()?.mediaId, startPositionMs = 0L)
    }

    private fun updateState(player: Player?) {
        if (player == null) return
        val duration = player.duration
        val position = player.currentPosition.coerceAtLeast(0)
        _state.update { old ->

            val looped = old.durationMs > 0 &&
                old.positionMs >= old.durationMs - LOOP_DETECT_MARGIN_MS &&
                position < old.positionMs - LOOP_DETECT_BACKSTEP_MS
            old.copy(
                connected = true,
                trackId = player.currentMediaItem?.mediaId?.toLongOrNull(),
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = position,
                durationMs = if (duration > 0 && duration != C.TIME_UNSET) duration else 0,
                shuffle = shuffleEnabled,
                repeatMode = player.repeatMode,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
                completedTrackId = if (looped) old.trackId else old.completedTrackId,
                completionCount = old.completionCount + if (looped) 1 else 0,
            )
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val remoteArt = artwork?.takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        }?.let(Uri::parse)
        val localArt = artwork?.takeIf {
            !it.startsWith("http://") && !it.startsWith("https://")
        }?.let(::localArtUri)
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album.ifBlank { null })
            .apply {
                if (localArt != null) {
                    setArtworkUri(localArt)
                } else if (remoteArt != null) {
                    setArtworkUri(remoteArt)
                }
            }
            .build()
        val mediaUri = if (uri.contains("://")) Uri.parse(uri) else Uri.fromFile(File(uri))
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun localArtUri(path: String): Uri? = runCatching {
        val file = File(path)
        if (!file.exists() || file.length() <= 0) return null
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.artprovider", file)
    }.getOrNull()
}

private const val LOOP_DETECT_MARGIN_MS = 1_500L

private const val LOOP_DETECT_BACKSTEP_MS = 1_000L
