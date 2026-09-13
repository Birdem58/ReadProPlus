package com.example.readproplus.tts

import android.content.Context
import android.util.Log
import com.example.readproplus.model.tts.AudioSegment
import com.example.readproplus.model.tts.TtsConfig
import com.example.readproplus.model.tts.TtsVoice
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Piper VITS runner backed by Sherpa-ONNX and the shared 24 kHz AudioPlayer. */
class PiperEngine(
    private val context: Context,
    private val modelManager: PiperModelManager,
    initialConfig: TtsConfig = TtsConfig(),
) {

    private val audioPlayer = AudioPlayer()
    private var tts: OfflineTts? = null
    private var currentJob: Job? = null
    private var config: TtsConfig = initialConfig

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private var activeVoiceId: String? = null

    suspend fun initialize(voice: TtsVoice) {
        require(voice.piperModelFileName != null) { "${voice.displayName} is not a Piper model." }
        val location = modelManager.resolveLocation(voice)
        val ttsConfig = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = location.modelPath,
                    tokens = location.tokensPath,
                    dataDir = location.dataDirectoryPath,
                ),
                numThreads = 2,
                debug = false,
                provider = "cpu",
            ),
            maxNumSentences = 1,
            silenceScale = 0.2f,
        )

        withContext(Dispatchers.IO) {
            tts?.release()
            tts = if (location.usesAssets) {
                OfflineTts(assetManager = context.assets, config = ttsConfig)
            } else {
                OfflineTts(config = ttsConfig)
            }
        }
        activeVoiceId = voice.id
        audioPlayer.setVolume(config.volume)
        audioPlayer.setSpeed(config.speed)
        Log.i(TAG, "Piper initialized with voice: ${voice.displayName}")
    }

    fun isReady(): Boolean = tts != null

    suspend fun startSpeakingRange(
        pages: List<String>,
        startPageIndex: Int,
        endPageIndex: Int,
        onAudioReady: suspend (chunks: List<ShortArray>, sampleCount: Long, durationMs: Long) -> Unit = { _, _, _ -> },
    ) = coroutineScope {
        check(isReady()) { "Piper model is not loaded. Call initialize() first." }
        currentJob?.cancel()
        currentJob = coroutineContext[Job]
        audioPlayer.stop()
        audioPlayer.resetBuffer()

        val sourcePages = pages.ifEmpty { listOf("Okunacak metin bulunamadı.") }
        val startIndex = startPageIndex.coerceIn(0, sourcePages.lastIndex)
        val endIndex = endPageIndex.coerceIn(startIndex, sourcePages.lastIndex)
        val targetPageTexts = sourcePages.subList(startIndex, endIndex + 1)
        val sentences = targetPageTexts.flatMap(::splitSentences).ifEmpty {
            listOf("Okunacak metin bulunamadı.")
        }
        val targetMs = targetPageTexts.sumOf { page ->
            page.split(Regex("\\s+")).count { it.isNotBlank() }.coerceAtLeast(1) * 60_000L / 165L
        }.coerceAtLeast(1L)
        _engineState.value = EngineState.Generating(0L, targetMs)

        var generatedDurationMs = 0L
        for (sentence in sentences) {
            ensureActive()
            try {
                val segment = generateAudioSegment(sentence)
                audioPlayer.appendSamples(segment.samples)
                generatedDurationMs += segment.durationMs
                _engineState.value = EngineState.Generating(audioPlayer.getTotalDurationMs(), targetMs)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.e(TAG, "Piper sentence generation failed", error)
                if (generatedDurationMs == 0L) {
                    _engineState.value = EngineState.Error("Piper audio generation failed: ${error.message}")
                    return@coroutineScope
                }
            }
        }

        ensureActive()
        runCatching {
            onAudioReady(
                audioPlayer.snapshotChunks(),
                audioPlayer.getTotalSampleCount(),
                audioPlayer.getTotalDurationMs(),
            )
        }.onFailure { error -> Log.w(TAG, "Could not save Piper audio", error) }

        val totalDuration = audioPlayer.getTotalDurationMs()
        _engineState.value = EngineState.Playing(0L, totalDuration, 0f)
        val playbackJob = launch { audioPlayer.play() }
        observePlaybackLoop(playbackJob)
    }

    suspend fun playSamples(samples: ShortArray) = coroutineScope {
        require(samples.isNotEmpty()) { "Cannot play empty audio." }
        currentJob?.cancel()
        currentJob = coroutineContext[Job]
        audioPlayer.stop()
        audioPlayer.resetBuffer()
        audioPlayer.appendSamples(samples)
        _engineState.value = EngineState.Playing(0L, audioPlayer.getTotalDurationMs(), 0f)
        val playbackJob = launch { audioPlayer.play() }
        observePlaybackLoop(playbackJob)
    }

    suspend fun pause() {
        audioPlayer.pause()
        val progress = audioPlayer.playbackProgress.value
        _engineState.value = EngineState.Paused(progress.currentMs, progress.totalMs, progress.progress)
    }

    suspend fun resume() {
        audioPlayer.play()
        val progress = audioPlayer.playbackProgress.value
        _engineState.value = EngineState.Playing(progress.currentMs, progress.totalMs, progress.progress)
    }

    suspend fun seekTo(progress: Float) {
        audioPlayer.seekTo(progress)
    }

    suspend fun stop() {
        currentJob?.cancel()
        currentJob = null
        audioPlayer.stop()
        _engineState.value = EngineState.Stopped
    }

    fun updateConfig(updatedConfig: TtsConfig) {
        config = updatedConfig
        audioPlayer.setVolume(updatedConfig.volume)
        audioPlayer.setSpeed(updatedConfig.speed)
    }

    fun release() {
        currentJob?.cancel()
        currentJob = null
        tts?.release()
        tts = null
        activeVoiceId = null
        audioPlayer.release()
        _engineState.value = EngineState.Idle
    }

    private suspend fun generateAudioSegment(sentence: String): AudioSegment = withContext(Dispatchers.Default) {
        val generated = requireNotNull(tts).generateWithConfig(
            text = sentence,
            config = GenerationConfig(sid = 0, speed = 1.0f, silenceScale = 0.2f),
        )
        val samplesAt24k = resampleTo24k(generated.samples, generated.sampleRate)
        val pcmSamples = ShortArray(samplesAt24k.size) { index ->
            (samplesAt24k[index] * Short.MAX_VALUE).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        AudioSegment(
            text = sentence,
            sampleRate = OUTPUT_SAMPLE_RATE,
            samples = pcmSamples,
            durationMs = pcmSamples.size * 1000L / OUTPUT_SAMPLE_RATE,
        )
    }

    private suspend fun observePlaybackLoop(playbackJob: Job) = coroutineScope {
        while (isActive && !playbackJob.isCompleted) {
            val state = audioPlayer.playerState.value
            val progress = audioPlayer.playbackProgress.value
            when (state) {
                is PlayerState.Playing -> _engineState.value = EngineState.Playing(
                    progress.currentMs,
                    progress.totalMs,
                    progress.progress,
                )
                is PlayerState.Paused -> _engineState.value = EngineState.Paused(
                    progress.currentMs,
                    progress.totalMs,
                    progress.progress,
                )
                is PlayerState.Idle, is PlayerState.Stopped -> {
                    if (playbackJob.isCompleted && _engineState.value !is EngineState.Generating) {
                        _engineState.value = EngineState.Stopped
                        break
                    }
                }
            }
            kotlinx.coroutines.delay(100)
        }
        if (playbackJob.isCompleted && _engineState.value !is EngineState.Generating) {
            _engineState.value = EngineState.Stopped
        }
    }

    private fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val raw = text.split(Regex("(?<=[.!?])\\s+(?=.)"))
            .map(String::trim)
            .filter(String::isNotBlank)
        val merged = mutableListOf<String>()
        var pending = ""
        for (sentence in raw) {
            pending = if (pending.isEmpty()) sentence else "$pending $sentence"
            if (pending.length >= MIN_SENTENCE_LENGTH) {
                merged += pending
                pending = ""
            }
        }
        if (pending.isNotEmpty()) {
            if (merged.isEmpty()) merged += pending else merged[merged.lastIndex] = "${merged.last()} $pending"
        }
        return merged.ifEmpty { listOf(text.trim()) }
    }

    private fun resampleTo24k(samples: FloatArray, sourceRate: Int): FloatArray {
        if (samples.isEmpty() || sourceRate <= 0 || sourceRate == OUTPUT_SAMPLE_RATE) return samples
        val outputSize = (samples.size.toLong() * OUTPUT_SAMPLE_RATE / sourceRate).toInt().coerceAtLeast(1)
        if (samples.size == 1) return FloatArray(outputSize) { samples[0] }
        return FloatArray(outputSize) { index ->
            val sourcePosition = index.toDouble() * (samples.size - 1) / (outputSize - 1).coerceAtLeast(1)
            val left = sourcePosition.toInt().coerceIn(0, samples.lastIndex)
            val right = (left + 1).coerceAtMost(samples.lastIndex)
            val fraction = sourcePosition - left
            (samples[left] * (1.0 - fraction) + samples[right] * fraction).toFloat()
        }
    }

    private companion object {
        const val TAG = "PiperEngine"
        const val OUTPUT_SAMPLE_RATE = 24_000
        const val MIN_SENTENCE_LENGTH = 15
    }
}
