package com.example.readproplus.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.readproplus.data.AudiobookRepository
import com.example.readproplus.model.audiobook.Audiobook
import com.example.readproplus.model.audiobook.AudiobookManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream

data class AudiobookPlaybackState(
    val audiobook: Audiobook? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val bufferedDurationMs: Long = 0L,
    val currentPageIndex: Int = 1,
    val speed: Float = 1.0f,
    val sleepTimerMinutesRemaining: Int? = null,
) {
    val currentPage: Int
        get() = currentPageIndex.coerceAtLeast(1)
}

/**
 * Storytel-style disk-streaming audio player backed by Android MediaPlayer.
 * Plays merged_audio.wav directly from disk with zero memory overhead.
 */
class AudiobookPlayer(
    private val context: Context,
    private val repository: AudiobookRepository,
) {
    companion object {
        private const val TAG = "AudiobookPlayer"
        private const val UPDATE_INTERVAL_MS = 250L
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentAudiobook: Audiobook? = null
    private var currentManifest: AudiobookManifest? = null
    private var currentSpeed: Float = 1.0f

    private val _playbackState = MutableStateFlow(AudiobookPlaybackState())
    val playbackState: StateFlow<AudiobookPlaybackState> = _playbackState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var sleepTimerTargetMs: Long = 0L
    private var sleepTimerEndOnPage: Int? = null

    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            if (mediaPlayer?.isPlaying == true) {
                mainHandler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    @Synchronized
    fun loadAudiobook(audiobook: Audiobook, startPlaying: Boolean = false) {
        if (currentAudiobook?.id == audiobook.id && mediaPlayer != null) {
            if (startPlaying && mediaPlayer?.isPlaying != true) {
                play()
            }
            return
        }

        releaseMediaPlayer()

        currentAudiobook = audiobook
        currentManifest = repository.getManifest(audiobook.bookId)
        val audioFile = repository.getAudioFile(audiobook)

        if (!audioFile.exists() || audioFile.length() < 44) {
            Log.w(TAG, "Audio file does not exist or has invalid size: ${audioFile.absolutePath}")
            _playbackState.value = AudiobookPlaybackState(
                audiobook = audiobook,
                currentPageIndex = audiobook.currentPage,
            )
            return
        }

        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            FileInputStream(audioFile).use { fis ->
                player.setDataSource(fis.fd)
            }

            player.setOnPreparedListener { mp ->
                setPlaybackSpeed(currentSpeed)
                val resumePos = audiobook.currentPositionMs.coerceIn(0L, mp.duration.toLong())
                if (resumePos > 0) {
                    mp.seekTo(resumePos.toInt())
                }
                updateProgress()
                if (startPlaying) {
                    mp.start()
                    mainHandler.post(progressRunnable)
                }
            }

            player.setOnCompletionListener { mp ->
                val latestBook = repository.getAudiobook(audiobook.id)
                val latestManifest = repository.getManifest(audiobook.bookId)
                val currentPos = mp.currentPosition.toLong()
                val latestDuration = latestManifest?.totalDurationMs ?: 0L
                if (latestDuration > currentPos + 1000L) {
                    // New checkpoint audio was appended to disk while listening
                    currentAudiobook = latestBook
                    currentManifest = latestManifest
                    val resumeAudiobook = (latestBook ?: audiobook).copy(currentPositionMs = currentPos)
                    loadAudiobook(resumeAudiobook, startPlaying = true)
                } else {
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                    saveProgress()
                }
            }

            player.prepareAsync()
            mediaPlayer = player

            _playbackState.value = AudiobookPlaybackState(
                audiobook = audiobook,
                isPlaying = false,
                currentPositionMs = audiobook.currentPositionMs,
                totalDurationMs = audiobook.totalEstimatedDurationMs.coerceAtLeast(audiobook.checkpointDurationMs),
                bufferedDurationMs = audiobook.checkpointDurationMs,
                currentPageIndex = audiobook.currentPage,
                speed = currentSpeed,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer", e)
        }
    }

    fun play() {
        val player = mediaPlayer ?: run {
            currentAudiobook?.let { loadAudiobook(it, startPlaying = true) }
            return
        }
        if (!player.isPlaying) {
            player.start()
            mainHandler.post(progressRunnable)
            updateProgress()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                mainHandler.removeCallbacks(progressRunnable)
                updateProgress()
                saveProgress()
            }
        }
    }

    fun togglePlayPause() {
        if (mediaPlayer?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val buffered = currentAudiobook?.checkpointDurationMs ?: player.duration.toLong()
        val target = positionMs.coerceIn(0L, buffered.coerceAtLeast(0L))
        player.seekTo(target.toInt())
        updateProgress()
        saveProgress()
    }

    fun skipBackward15() {
        val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
        seekTo((current - 15000L).coerceAtLeast(0L))
    }

    fun skipForward15() {
        val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
        seekTo(current + 15000L)
    }

    fun seekToPage(pageIndex: Int) {
        val manifest = currentManifest ?: return
        val marker = manifest.markers.firstOrNull { it.pageIndex == pageIndex }
        if (marker != null) {
            seekTo(marker.startMs)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.5f)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val params = player.playbackParams ?: PlaybackParams()
                    params.speed = currentSpeed
                    player.playbackParams = params
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set playback speed", e)
        }
        updateProgress()
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        if (minutes == null || minutes <= 0) {
            sleepTimerTargetMs = 0L
            sleepTimerEndOnPage = null
        } else {
            sleepTimerTargetMs = System.currentTimeMillis() + (minutes * 60_000L)
            sleepTimerEndOnPage = null
        }
        updateProgress()
    }

    fun setSleepTimerEndOfPage() {
        val current = _playbackState.value.currentPageIndex
        sleepTimerEndOnPage = current
        sleepTimerTargetMs = 0L
        updateProgress()
    }

    private fun updateProgress() {
        val player = mediaPlayer
        val pos = player?.currentPosition?.toLong() ?: _playbackState.value.currentPositionMs
        val isPlaying = player?.isPlaying ?: false

        // Check sleep timer
        if (sleepTimerTargetMs > 0L && System.currentTimeMillis() >= sleepTimerTargetMs) {
            sleepTimerTargetMs = 0L
            pause()
            return
        }

        // Determine active page (1-based)
        val manifest = currentManifest
        val activePage = manifest?.markers?.lastOrNull { it.startMs <= pos }?.pageIndex
            ?: currentAudiobook?.currentPage ?: 1

        // Check end of page sleep timer
        if (sleepTimerEndOnPage != null && activePage != sleepTimerEndOnPage) {
            sleepTimerEndOnPage = null
            pause()
            return
        }

        val remainingMinutes = if (sleepTimerTargetMs > 0L) {
            ((sleepTimerTargetMs - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L).toInt() + 1
        } else null

        val book = currentAudiobook
        val buffered = book?.checkpointDurationMs ?: (player?.duration?.toLong() ?: 0L)
        val total = book?.totalEstimatedDurationMs?.coerceAtLeast(buffered) ?: buffered

        _playbackState.value = _playbackState.value.copy(
            audiobook = book,
            isPlaying = isPlaying,
            currentPositionMs = pos,
            totalDurationMs = total,
            bufferedDurationMs = buffered,
            currentPageIndex = activePage,
            speed = currentSpeed,
            sleepTimerMinutesRemaining = remainingMinutes,
        )
    }

    private fun saveProgress() {
        val book = currentAudiobook ?: return
        val pos = mediaPlayer?.currentPosition?.toLong() ?: return
        val activePage = _playbackState.value.currentPageIndex
        repository.updateListeningProgress(book.id, pos, activePage)
    }

    fun notifyCheckpointUpdated(audiobook: Audiobook) {
        if (currentAudiobook?.id == audiobook.id) {
            currentAudiobook = audiobook
            currentManifest = repository.getManifest(audiobook.bookId)
            updateProgress()
        }
    }

    fun release() {
        saveProgress()
        mainHandler.removeCallbacks(progressRunnable)
        releaseMediaPlayer()
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
