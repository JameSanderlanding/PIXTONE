package com.vova.glyph.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.vova.glyph.data.Prefs
import com.vova.glyph.data.Track
import com.vova.glyph.playback.dsp.EqCurve
import com.vova.glyph.playback.dsp.EqPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

data class PlayerUiState(
    val connected: Boolean = false,
    val trackId: Long? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val error: String? = null,
    val errorTrackId: Long? = null,

    val completedTrackId: Long? = null,

    val completionCount: Long = 0,
)

data class PlayerProgress(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
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

    private val _progress = MutableStateFlow(PlayerProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    private val _dsp = MutableStateFlow(
        DspUiState(
            eqEnabled = prefs.fxEqEnabled,
            eqPreset = EqPreset.fromName(prefs.fxEqPreset),
            bassEnabled = prefs.fxBassEnabled,
            bassStrength = prefs.fxBassStrength,
            loudnessEnabled = prefs.fxLoudnessEnabled,
            loudnessGainMb = prefs.fxLoudnessGain,
            speed = prefs.fxSpeed,
            reverbEnabled = prefs.fxReverbEnabled,
            reverbRoom = prefs.fxReverbRoom,
            reverbDamp = prefs.fxReverbDamp,
            reverbWet = prefs.fxReverbWet,
        )
    )
    val dsp: StateFlow<DspUiState> = _dsp.asStateFlow()

    private var progressUpdates: Job? = null

    private var lastPositionMs: Long = 0L

    private var lastDurationMs: Long = 0L

    private var pendingSeekMs: Long? = null

    private val controllerListener = object : MediaController.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (command.customAction == CMD_DSP_CHANGED) {
                val remote = dspFromBundle(args)
                if (remote.queried) _dsp.value = remote
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val failedId = controller?.currentMediaItem?.mediaId?.toLongOrNull()
                ?: _state.value.trackId
            _state.update {
                it.copy(
                    error = error.localizedMessage ?: "Ошибка воспроизведения",
                    errorTrackId = failedId,
                )
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
            _progress.subscriptionCount.collect { listeners ->
                if (listeners > 0) startProgressUpdates() else stopProgressUpdates()
            }
        }
    }

    private fun startProgressUpdates() {
        if (progressUpdates?.isActive == true) return
        progressUpdates = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null) refreshProgress(player)
                delay(if (player?.isPlaying == true) PROGRESS_ACTIVE_MS else PROGRESS_IDLE_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdates?.cancel()
        progressUpdates = null
        lastPositionMs = 0L
        lastDurationMs = 0L
    }

    private fun refreshProgress(player: Player, detectLoop: Boolean = true) {
        val reported = player.currentPosition.coerceAtLeast(0)
        val seekTarget = pendingSeekMs
        val position = if (seekTarget != null && abs(reported - seekTarget) > SEEK_ACK_TOLERANCE_MS) {
            seekTarget
        } else {
            pendingSeekMs = null
            reported
        }
        val rawDuration = player.duration
        val duration = if (rawDuration > 0 && rawDuration != C.TIME_UNSET) rawDuration else 0L

        val looped = detectLoop && lastDurationMs > 0 &&
            lastPositionMs >= lastDurationMs - LOOP_DETECT_MARGIN_MS &&
            position < lastPositionMs - LOOP_DETECT_BACKSTEP_MS

        lastPositionMs = position
        lastDurationMs = duration

        _progress.value = PlayerProgress(positionMs = position, durationMs = duration)

        if (looped) {
            _state.update { old ->
                old.copy(
                    completedTrackId = old.trackId,
                    completionCount = old.completionCount + 1,
                )
            }
        }
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token)
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { connected ->
                        controller = connected

                        connected.shuffleModeEnabled = false
                        connected.addListener(listener)
                        applySpeed(connected)
                        updateState(connected)
                        queryDsp(connected)
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
        lastPositionMs = 0L
        lastDurationMs = 0L
        pendingSeekMs = null
        updateState(player)
        refreshProgress(player, detectLoop = false)
    }

    fun addToQueue(track: Track) {
        val player = controller ?: return
        if (player.mediaItemCount == 0) {
            playQueue(listOf(track), 0)
            return
        }
        if (originalItems.isEmpty()) {
            originalItems = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        }
        val item = track.toMediaItem()
        player.addMediaItem(item)
        originalItems = originalItems + item
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
        refreshProgress(player, detectLoop = false)
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
        pendingSeekMs = null
        updateState(player)
        refreshProgress(player, detectLoop = false)
    }

    fun previous() {
        val player = controller ?: return
        if (player.hasPreviousMediaItem()) player.seekToPrevious()
        else player.seekTo(0, 0L)
        pendingSeekMs = null
        updateState(player)
        refreshProgress(player, detectLoop = false)
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        val target = positionMs.coerceAtLeast(0)
        pendingSeekMs = target
        player.seekTo(target)
        lastPositionMs = target
        _progress.value = PlayerProgress(
            positionMs = target,
            durationMs = _progress.value.durationMs,
        )
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

    fun updateTrackMetadata(track: Track) {
        val player = controller ?: return
        val item = track.toMediaItem()
        val index = (0 until player.mediaItemCount).firstOrNull {
            player.getMediaItemAt(it).mediaId == item.mediaId
        } ?: return
        player.replaceMediaItem(index, item)
        originalItems = originalItems.map { if (it.mediaId == item.mediaId) item else it }
    }

    fun stop() {
        pendingSeekMs = null
        controller?.stop()
        updateState(controller)
    }

    fun setSpeed(value: Float) {
        val speed = value.coerceIn(MIN_SPEED, MAX_SPEED)
        prefs.fxSpeed = speed
        controller?.setPlaybackParameters(PlaybackParameters(speed, speed))
        _dsp.update { current ->
            current.copy(
                speed = speed,
                bands = bandsForPreset(current, current.eqPreset, speed),
            )
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        prefs.fxEqEnabled = enabled
        _dsp.update { it.copy(eqEnabled = enabled) }
    }

    fun setEqPreset(preset: EqPreset) {
        val current = _dsp.value
        val bands = if (preset == EqPreset.CUSTOM) {
            customBands(current)
        } else {
            bandsForPreset(current, preset, current.speed)
        }
        if (preset == EqPreset.CUSTOM) {
            prefs.setCustomEq(encodeBands(bands.map { it.levelMb }))
        } else {
            prefs.selectEqPreset(preset.name)
        }
        _dsp.value = current.copy(
            eqPreset = preset,
            eqEnabled = true,
            bands = bands,
        )
    }

    fun setBandLevel(index: Int, levelMb: Int) {
        val current = _dsp.value
        if (index !in current.bands.indices) return
        val level = levelMb.coerceIn(current.levelMinMb, current.levelMaxMb)
        val bands = current.bands.mapIndexed { position, band ->
            if (position == index) band.copy(levelMb = level) else band
        }
        prefs.setCustomEq(encodeBands(bands.map { it.levelMb }))
        _dsp.value = current.copy(
            bands = bands,
            eqPreset = EqPreset.CUSTOM,
            eqEnabled = true,
        )
    }

    fun setBassEnabled(enabled: Boolean) {
        prefs.fxBassEnabled = enabled
        val strength = if (enabled && prefs.fxBassStrength == 0) DEFAULT_BASS_STRENGTH else prefs.fxBassStrength
        if (strength != prefs.fxBassStrength) prefs.fxBassStrength = strength
        _dsp.update { it.copy(bassEnabled = enabled, bassStrength = strength) }
    }

    fun setBassStrength(strength: Int) {
        val value = strength.coerceIn(0, 1000)
        prefs.fxBassStrength = value
        prefs.fxBassEnabled = true
        _dsp.update { it.copy(bassStrength = value, bassEnabled = true) }
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        prefs.fxLoudnessEnabled = enabled
        val gain = if (enabled && prefs.fxLoudnessGain == 0) DEFAULT_LOUDNESS_MB else prefs.fxLoudnessGain
        if (gain != prefs.fxLoudnessGain) prefs.fxLoudnessGain = gain
        _dsp.update { it.copy(loudnessEnabled = enabled, loudnessGainMb = gain) }
    }

    fun setLoudnessGain(gainMb: Int) {
        val value = gainMb.coerceIn(0, MAX_LOUDNESS_MB)
        prefs.fxLoudnessGain = value
        prefs.fxLoudnessEnabled = true
        _dsp.update { it.copy(loudnessGainMb = value, loudnessEnabled = true) }
    }

    fun setReverbEnabled(enabled: Boolean) {
        prefs.fxReverbEnabled = enabled
        _dsp.update { it.copy(reverbEnabled = enabled) }
    }

    fun setReverbRoom(value: Int) {
        val room = value.coerceIn(0, 100)
        prefs.fxReverbRoom = room
        prefs.fxReverbEnabled = true
        _dsp.update { it.copy(reverbRoom = room, reverbEnabled = true) }
    }

    fun setReverbDamp(value: Int) {
        val damp = value.coerceIn(0, 100)
        prefs.fxReverbDamp = damp
        prefs.fxReverbEnabled = true
        _dsp.update { it.copy(reverbDamp = damp, reverbEnabled = true) }
    }

    fun setReverbWet(value: Int) {
        val wet = value.coerceIn(0, 100)
        prefs.fxReverbWet = wet
        prefs.fxReverbEnabled = true
        _dsp.update { it.copy(reverbWet = wet, reverbEnabled = true) }
    }

    fun resetDsp() {
        prefs.resetFx()
        controller?.setPlaybackParameters(PlaybackParameters(1f, 1f))
        _dsp.update { current ->
            current.copy(
                bands = current.bands.map { band -> band.copy(levelMb = 0) },
                eqEnabled = false,
                eqPreset = EqPreset.FLAT,
                bassEnabled = false,
                bassStrength = 0,
                loudnessEnabled = false,
                loudnessGainMb = 0,
                speed = 1f,
                reverbEnabled = false,
                reverbRoom = 70,
                reverbDamp = 40,
                reverbWet = 30,
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null, errorTrackId = null) }
    }

    private fun applySpeed(player: Player) {
        val speed = prefs.fxSpeed
        runCatching { player.setPlaybackParameters(PlaybackParameters(speed, speed)) }
    }

    private fun queryDsp(player: MediaController) {
        val future = runCatching {
            player.sendCustomCommand(
                SessionCommand(CMD_QUERY_DSP, Bundle.EMPTY),
                Bundle.EMPTY,
            )
        }.getOrNull() ?: return
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { result ->
                        if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                            val remote = dspFromBundle(result.extras)
                            if (remote.queried) _dsp.value = remote
                        } else {
                            _dsp.update { it.copy(queried = true) }
                        }
                    }
                    .onFailure { _dsp.update { it.copy(queried = true) } }
            },
            executor,
        )
    }

    private fun customBands(state: DspUiState): List<DspBand> {
        val stored = decodeBands(prefs.fxEqBands)
        if (stored.isEmpty()) return state.bands
        return state.bands.mapIndexed { index, band ->
            band.copy(
                levelMb = stored.getOrElse(index) { band.levelMb }
                    .coerceIn(state.levelMinMb, state.levelMaxMb),
            )
        }
    }

    private fun bandsForPreset(
        state: DspUiState,
        preset: EqPreset,
        speed: Float,
    ): List<DspBand> {
        if (preset == EqPreset.CUSTOM || state.bands.isEmpty()) return state.bands
        val centers = state.bands.map { it.centerHz }.toIntArray()
        val gains = EqCurve.gains(preset, centers, speed, state.levelMinMb, state.levelMaxMb)
        return state.bands.mapIndexed { index, band -> band.copy(levelMb = gains[index]) }
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
        _state.update { old ->
            old.copy(
                connected = true,
                trackId = player.currentMediaItem?.mediaId?.toLongOrNull(),
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                shuffle = shuffleEnabled,
                repeatMode = player.repeatMode,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
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
            .setTitle(effectiveTitle)
            .setArtist(effectiveArtist)
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
        val builder = MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
        if (isOnline) builder.setMimeType("audio/mp4")
        return builder.build()
    }

    private fun localArtUri(path: String): Uri? = runCatching {
        val file = File(path)
        if (!file.exists() || file.length() <= 0) return null
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.artprovider", file)
    }.getOrNull()
}

private const val PROGRESS_ACTIVE_MS = 500L

private const val PROGRESS_IDLE_MS = 1_000L

private const val SEEK_ACK_TOLERANCE_MS = 700L

private const val LOOP_DETECT_MARGIN_MS = 1_500L

private const val LOOP_DETECT_BACKSTEP_MS = 1_000L

private const val MIN_SPEED = 0.5f

private const val MAX_SPEED = 1.5f

private const val MAX_LOUDNESS_MB = 1_500

private const val DEFAULT_BASS_STRENGTH = 500

private const val DEFAULT_LOUDNESS_MB = 500
