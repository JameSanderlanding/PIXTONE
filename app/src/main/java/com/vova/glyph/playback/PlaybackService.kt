package com.vova.glyph.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.vova.glyph.MainActivity
import com.vova.glyph.data.Prefs
import com.vova.glyph.playback.dsp.DspRenderersFactory
import com.vova.glyph.playback.dsp.ReverbAudioProcessor

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val dspQueryCommand = SessionCommand(CMD_QUERY_DSP, Bundle.EMPTY)
    private val dspChangedCommand = SessionCommand(CMD_DSP_CHANGED, Bundle.EMPTY)
    private lateinit var prefs: Prefs
    private lateinit var reverb: ReverbAudioProcessor
    private lateinit var effects: AudioEffects
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val analyticsListener = object : AnalyticsListener {
        override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
            effects.attach(audioSessionId)
            effects.applyAll(prefs.fxSpeed)
            mediaSession?.broadcastCustomCommand(
                dspChangedCommand,
                effects.snapshot(prefs.fxSpeed).toBundle(),
            )
        }
    }

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith(FX_PREFIX)) {
            effects.applyKey(key, prefs.fxSpeed)
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = SessionCommands.Builder()
                .addSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands)
                .add(dspQueryCommand)
                .add(dspChangedCommand)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CMD_QUERY_DSP) {
                val state = effects.snapshot(prefs.fxSpeed)
                return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_SUCCESS, state.toBundle()),
                )
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        reverb = ReverbAudioProcessor()
        effects = AudioEffects(prefs, reverb)
        effects.applyAll(prefs.fxSpeed)
        prefs.registerListener(preferenceListener)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val exoPlayer = ExoPlayer.Builder(this, DspRenderersFactory(this, reverb))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        exoPlayer.addAnalyticsListener(analyticsListener)
        effects.attach(exoPlayer.audioSessionId)
        effects.applyAll(prefs.fxSpeed)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .setCallback(sessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = player
        if (current == null || !current.playWhenReady || current.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (::prefs.isInitialized) prefs.unregisterListener(preferenceListener)
        if (::effects.isInitialized) effects.release()
        player?.removeAnalyticsListener(analyticsListener)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private companion object {
        const val FX_PREFIX = "fx_"
    }
}
