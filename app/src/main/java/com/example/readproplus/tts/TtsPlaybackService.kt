package com.example.readproplus.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.readproplus.MainActivity
import com.example.readproplus.model.tts.TtsState

/** Keeps reader audio alive while the app is backgrounded or the screen is off. */
class TtsPlaybackService : Service() {

    private var title = "ReadProPlus speech"
    private var started = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        mediaSession = MediaSession(this, "ReadProPlus speech").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    commandHandler?.invoke(ACTION_RESUME)
                }

                override fun onPause() {
                    commandHandler?.invoke(ACTION_PAUSE)
                }

                override fun onStop() {
                    commandHandler?.invoke(ACTION_STOP)
                }
            })
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            isActive = true
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "ReadProPlus speech" }
                startForeground(NOTIFICATION_ID, buildNotification("Preparing audio", isPaused = false))
                acquireWakeLock()
                started = true
            }
            ACTION_UPDATE -> {
                if (started) {
                    val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "Reading" }
                    val paused = intent.getBooleanExtra(EXTRA_PAUSED, false)
                    updateNotification(label, paused)
                }
            }
            ACTION_PAUSE, ACTION_RESUME, ACTION_STOP -> commandHandler?.invoke(intent.action ?: return START_NOT_STICKY)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (instance === this) instance = null
        started = false
        wakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        wakeLock = null
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ReadProPlus:TtsPlayback")
            ?.apply { acquire() }
    }

    private fun updateNotification(label: String, paused: Boolean) {
        updateMediaSession(label, paused)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(label, paused))
    }

    private fun buildNotification(label: String, isPaused: Boolean): Notification {
        val launchIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pauseOrResume = PendingIntent.getService(
            this,
            REQUEST_PAUSE_RESUME,
            Intent(this, TtsPlaybackService::class.java).setAction(
                if (isPaused) ACTION_RESUME else ACTION_PAUSE,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, TtsPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(label)
            .setContentIntent(launchIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pauseOrResume,
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)

        return builder.build()
    }

    private fun updateMediaSession(label: String, paused: Boolean) {
        val state = when {
            paused -> PlaybackState.STATE_PAUSED
            label == "Playing" -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_BUFFERING
        }
        mediaSession?.setMetadata(
            android.media.MediaMetadata.Builder()
                .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, "ReadProPlus")
                .build(),
        )
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_STOP,
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Reader audio", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Controls for ReadProPlus background speech"
                },
            )
        }
    }

    companion object {
        const val ACTION_START = "com.example.readproplus.tts.START"
        const val ACTION_UPDATE = "com.example.readproplus.tts.UPDATE"
        const val ACTION_PAUSE = "com.example.readproplus.tts.PAUSE"
        const val ACTION_RESUME = "com.example.readproplus.tts.RESUME"
        const val ACTION_STOP = "com.example.readproplus.tts.STOP"

        private const val EXTRA_TITLE = "title"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_PAUSED = "paused"
        private const val CHANNEL_ID = "reader_audio"
        private const val NOTIFICATION_ID = 7001
        private const val REQUEST_OPEN = 7002
        private const val REQUEST_PAUSE_RESUME = 7003
        private const val REQUEST_STOP = 7004

        @Volatile
        var commandHandler: ((String) -> Unit)? = null

        @Volatile
        private var instance: TtsPlaybackService? = null

        fun start(context: Context, voiceName: String) {
            val intent = Intent(context, TtsPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, "ReadProPlus · $voiceName")
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun update(context: Context, state: EngineState) {
            val labelAndPaused = when (state) {
                EngineState.Idle, EngineState.Stopped, is EngineState.Error -> null
                is EngineState.Generating -> "Generating speech" to false
                is EngineState.Playing -> "Playing" to false
                is EngineState.Paused -> "Paused" to true
            }
            updateInternal(context, labelAndPaused)
        }

        fun update(context: Context, state: TtsState) {
            val labelAndPaused = when (state) {
                TtsState.Idle, TtsState.Stopped, is TtsState.Error -> null
                is TtsState.ModelDownloading -> "Preparing speech model" to false
                is TtsState.Generating -> "Generating speech" to false
                is TtsState.Playing -> "Playing" to false
                is TtsState.Paused -> "Paused" to true
            }
            updateInternal(context, labelAndPaused)
        }

        private fun updateInternal(context: Context, labelAndPaused: Pair<String, Boolean>?) {
            val service = instance ?: return
            if (labelAndPaused == null) {
                stop(context)
                return
            }
            service.updateNotification(labelAndPaused.first, labelAndPaused.second)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TtsPlaybackService::class.java))
        }
    }
}
