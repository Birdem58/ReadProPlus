package com.example.readproplus.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

sealed interface PlayerState {
    data object Idle : PlayerState
    data object Playing : PlayerState
    data object Paused : PlayerState
    data object Stopped : PlayerState
}

data class PlaybackProgress(
    val currentMs: Long = 0L,
    val totalMs: Long = 0L,
    val progress: Float = 0f,
    val speed: Float = 1.0f,
)

class AudioPlayer {

    companion object {
        private const val TAG = "AudioPlayer"
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FRAMES = 4096
        private const val BYTES_PER_FRAME = 2
        private const val DEFAULT_SPEED = 1f
        private const val MIN_SPEED = 0.5f
        private const val MAX_SPEED = 2f
        private const val NORMAL_PITCH = 1f
    }

    private var audioTrack: AudioTrack? = null
    private val masterBuffer = ArrayList<ShortArray>()
    private var totalSampleCount: Long = 0L

    private val playheadSampleIndex = AtomicInteger(0)
    private val isPlaying = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var currentVolume: Float = 1.0f
    private var currentSpeed: Float = 1.0f
    private var appliedPlaybackSpeed: Float = DEFAULT_SPEED

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

    @Synchronized
    fun resetBuffer() {
        masterBuffer.clear()
        totalSampleCount = 0L
        playheadSampleIndex.set(0)
        updateProgressFlow()
    }

    @Synchronized
    fun appendSamples(samples: ShortArray) {
        if (samples.isEmpty()) return
        masterBuffer.add(samples)
        totalSampleCount += samples.size
        updateProgressFlow()
    }

    @Synchronized
    fun snapshotSamples(): ShortArray {
        val result = ShortArray(totalSampleCount.toInt())
        var offset = 0
        masterBuffer.forEach { chunk ->
            chunk.copyInto(result, destinationOffset = offset)
            offset += chunk.size
        }
        return result
    }

    /**
     * Returns the generated PCM chunks without copying their sample data.
     * Callers must treat the returned arrays as read-only.
     */
    @Synchronized
    fun snapshotChunks(): List<ShortArray> = masterBuffer.toList()

    @Synchronized
    fun getTotalSampleCount(): Long = totalSampleCount

    fun getTotalDurationMs(): Long {
        return ((totalSampleCount * 1000.0) / SAMPLE_RATE / appliedPlaybackSpeed).toLong()
    }

    fun getCurrentPositionMs(): Long {
        return ((playheadSampleIndex.get().toLong() * 1000.0) / SAMPLE_RATE / appliedPlaybackSpeed).toLong()
    }

    private fun updateProgressFlow() {
        val totalMs = getTotalDurationMs()
        val currentMs = getCurrentPositionMs()
        val p = if (totalMs > 0) (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f
        _playbackProgress.value = PlaybackProgress(
            currentMs = currentMs,
            totalMs = totalMs,
            progress = p,
            speed = currentSpeed,
        )
    }

    private fun ensureAudioTrack() {
        if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
            )
            val speedSafeBufferSize = (minBufferSize * MAX_SPEED).roundToInt()
            val bufferSize = maxOf(
                speedSafeBufferSize,
                BUFFER_SIZE_FRAMES * BYTES_PER_FRAME * MAX_SPEED.roundToInt(),
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            appliedPlaybackSpeed = DEFAULT_SPEED
            applyPitchPreservingSpeed(track)
        } catch (e: Throwable) {
            Log.e(TAG, "AudioTrack initialization failed", e)
        }
    }

    suspend fun play() = withContext(Dispatchers.IO) {
        ensureAudioTrack()
        if (isPaused.get()) {
            isPaused.set(false)
            isPlaying.set(true)
            try {
                audioTrack?.play()
            } catch (e: Throwable) {
                Log.e(TAG, "AudioTrack play error", e)
            }
            _playerState.value = PlayerState.Playing
        } else if (!isPlaying.get()) {
            startPlayback()
        }
    }

    private suspend fun startPlayback() = withContext(Dispatchers.IO) {
        ensureAudioTrack()
        isPlaying.set(true)
        isPaused.set(false)
        try {
            audioTrack?.play()
        } catch (e: Throwable) {
            Log.e(TAG, "AudioTrack start error", e)
        }
        _playerState.value = PlayerState.Playing
        startPlaybackLoop()
    }

    suspend fun pause() {
        isPaused.set(true)
        try {
            audioTrack?.pause()
        } catch (e: Throwable) {
            Log.e(TAG, "AudioTrack pause error", e)
        }
        _playerState.value = PlayerState.Paused
        updateProgressFlow()
    }

    suspend fun seekTo(fraction: Float) = withContext(Dispatchers.IO) {
        val targetSample = (fraction.coerceIn(0f, 1f) * totalSampleCount).toInt()
        playheadSampleIndex.set(targetSample)
        try {
            audioTrack?.apply {
                pause()
                flush()
                if (isPlaying.get() && !isPaused.get()) {
                    play()
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "AudioTrack seek error", e)
        }
        updateProgressFlow()
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        isPlaying.set(false)
        isPaused.set(false)
        try {
            audioTrack?.apply {
                pause()
                flush()
                stop()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "AudioTrack stop error", e)
        }
        audioTrack = null
        appliedPlaybackSpeed = DEFAULT_SPEED
        resetBuffer()
        _playerState.value = PlayerState.Stopped
        updateProgressFlow()
    }

    private suspend fun startPlaybackLoop() = withContext(Dispatchers.IO) {
        try {
            while (isPlaying.get()) {
                while (isPaused.get() && isPlaying.get()) {
                    kotlinx.coroutines.delay(100)
                }
                if (!isPlaying.get()) break

                val sampleOffset = playheadSampleIndex.get()
                if (sampleOffset >= totalSampleCount) {
                    kotlinx.coroutines.delay(100)
                    if (playheadSampleIndex.get() >= totalSampleCount && isPlaying.get()) {
                        break
                    }
                    continue
                }

                val (chunk, offsetInChunk) = getChunkAtSampleIndex(sampleOffset) ?: run {
                    kotlinx.coroutines.delay(50)
                    continue
                }

                val remainingInChunk = chunk.size - offsetInChunk
                if (remainingInChunk <= 0) {
                    playheadSampleIndex.addAndGet(1)
                    continue
                }
                val sourceSampleCount = minOf(
                    remainingInChunk,
                    BUFFER_SIZE_FRAMES,
                )
                val renderedSamples = applyVolume(
                    source = chunk,
                    sourceOffset = offsetInChunk,
                    sourceSampleCount = sourceSampleCount,
                )

                val track = audioTrack ?: break
                val written = try {
                    track.write(renderedSamples, 0, renderedSamples.size)
                } catch (e: Throwable) {
                    Log.e(TAG, "AudioTrack write exception", e)
                    -1
                }

                if (written > 0) {
                    playheadSampleIndex.addAndGet(written.coerceAtMost(sourceSampleCount))
                    updateProgressFlow()
                } else if (written < 0) {
                    Log.e(TAG, "AudioTrack write error code: $written")
                    break
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Playback loop exception", e)
        } finally {
            val completedNaturally = totalSampleCount > 0L &&
                playheadSampleIndex.get().toLong() >= totalSampleCount
            isPlaying.set(false)
            if (!isPaused.get()) {
                // Keep the player replayable after a recording reaches EOF.
                if (completedNaturally) playheadSampleIndex.set(0)
                _playerState.value = PlayerState.Idle
            }
            try {
                audioTrack?.apply {
                    pause()
                    flush()
                }
            } catch (e: Throwable) {}
            updateProgressFlow()
        }
    }

    @Synchronized
    private fun getChunkAtSampleIndex(sampleIndex: Int): Pair<ShortArray, Int>? {
        var accumulated = 0
        val copy = ArrayList(masterBuffer)
        for (chunk in copy) {
            if (sampleIndex < accumulated + chunk.size) {
                val offset = sampleIndex - accumulated
                return Pair(chunk, offset)
            }
            accumulated += chunk.size
        }
        return null
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
            .takeIf { it.isFinite() }
            ?.coerceIn(MIN_SPEED, MAX_SPEED)
            ?: DEFAULT_SPEED
        audioTrack?.let(::applyPitchPreservingSpeed)
        updateProgressFlow()
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
    }

    fun release() {
        isPlaying.set(false)
        try {
            audioTrack?.release()
        } catch (e: Throwable) {}
        audioTrack = null
        appliedPlaybackSpeed = DEFAULT_SPEED
        resetBuffer()
    }

    /**
     * Speed and pitch are separate AudioTrack parameters. Keeping pitch at 1.0f
     * changes reading pace without turning the voice into a higher-pitched sample.
     */
    private fun applyPitchPreservingSpeed(track: AudioTrack) {
        try {
            track.setPlaybackParams(
                PlaybackParams()
                    .setSpeed(currentSpeed)
                    .setPitch(NORMAL_PITCH)
                    .setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_FAIL),
            )
            appliedPlaybackSpeed = currentSpeed
        } catch (error: Throwable) {
            Log.w(TAG, "Pitch-preserving playback speed is unavailable; keeping the last valid speed.", error)
        }
    }

    private fun applyVolume(
        source: ShortArray,
        sourceOffset: Int,
        sourceSampleCount: Int,
    ): ShortArray {
        val volume = currentVolume

        if (volume == 1f) {
            return source.copyOfRange(sourceOffset, sourceOffset + sourceSampleCount)
        }

        return ShortArray(sourceSampleCount) { index ->
            (source[sourceOffset + index] * volume)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

}
