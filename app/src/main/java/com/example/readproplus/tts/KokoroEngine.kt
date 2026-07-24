package com.example.readproplus.tts

import android.util.Log
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
import com.example.readproplus.model.tts.AudioSegment
import com.example.readproplus.model.tts.TtsConfig
import com.example.readproplus.model.tts.TtsVoice
import kotlin.math.roundToInt
import kotlin.math.roundToLong

sealed interface EngineState {
    data object Idle : EngineState
    data class Generating(val generatedMs: Long, val targetMs: Long) : EngineState
    data class Playing(val currentMs: Long, val totalMs: Long, val progress: Float) : EngineState
    data class Paused(val currentMs: Long, val totalMs: Long, val progress: Float) : EngineState
    data object Stopped : EngineState
    data class Error(val message: String) : EngineState
}

class KokoroEngine(
    private val inference: KokoroInference,
    private val tokenizer: KokoroTokenizer,
    private val voiceManager: VoiceManager,
    initialConfig: TtsConfig = TtsConfig(),
) {
    companion object {
        private const val TAG = "KokoroEngine"
    }

    private val audioPlayer = AudioPlayer()
    private var voiceEmbedding: FloatArray = FloatArray(0)
    private var currentJob: Job? = null
    private var config: TtsConfig = initialConfig

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    val playbackProgress: StateFlow<PlaybackProgress> = audioPlayer.playbackProgress
    suspend fun initialize(voice: TtsVoice = TtsVoice.NICOLE) {
        voiceEmbedding = withContext(Dispatchers.IO) {
            voiceManager.loadVoice(voice).also { embedding ->
                require(voiceManager.validateVoice(embedding)) {
                    "The ${voice.displayName} voice data is incomplete or invalid."
                }
            }
        }
        tokenizer.initialize()
        audioPlayer.setVolume(config.volume)
        audioPlayer.setSpeed(config.speed)
        Log.i(TAG, "Engine initialized with voice: ${voice.displayName}")
    }

    fun isReady(): Boolean = inference.isLoaded()

    fun loadModel(modelPath: String) {
        inference.loadModel(modelPath, voiceEmbedding)
    }

    suspend fun startSpeakingDuration(
        pages: List<String>,
        startPageIndex: Int,
        targetMinutes: Int,
    ) = coroutineScope {
        check(isReady()) { "Model not loaded. Call loadModel() first." }
        require(voiceEmbedding.isNotEmpty()) { "Voice not initialized" }

        currentJob?.cancel()
        currentJob = coroutineContext[Job]

        audioPlayer.stop()
        audioPlayer.resetBuffer()

        val targetMs = targetMinutes * 60 * 1000L
        val playbackSpeed = config.speed
        _engineState.value = EngineState.Generating(0L, targetMs)

        val startIndex = startPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        val sentencesToGenerate = mutableListOf<String>()

        for (i in startIndex until pages.size) {
            val pageText = pages[i]
            val sList = splitSentences(pageText)
            sentencesToGenerate.addAll(sList)
        }

        if (sentencesToGenerate.isEmpty()) {
            sentencesToGenerate.add("No text available to generate speech.")
        }

        var accumulatedSourceMs = 0L

        for (sentence in sentencesToGenerate) {
            ensureActive()

            try {
                val segment = generateAudioSegment(sentence)
                audioPlayer.appendSamples(segment.samples)
                accumulatedSourceMs += segment.durationMs
                val accumulatedPlaybackMs = (accumulatedSourceMs / playbackSpeed).roundToLong()

                _engineState.value = EngineState.Generating(accumulatedPlaybackMs, targetMs)

                if (accumulatedPlaybackMs >= targetMs) {
                    break
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Sentence audio generation failed for: '$sentence'", e)
                if (accumulatedSourceMs == 0L) {
                    _engineState.value = EngineState.Error("Audio generation failed: ${e.message}")
                    return@coroutineScope
                }
            }
        }

        ensureActive()

        _engineState.value = EngineState.Playing(0L, audioPlayer.getTotalDurationMs(), 0f)
        val playbackJob = launch {
            audioPlayer.play()
        }

        observePlaybackLoop(playbackJob)
    }

    suspend fun startSpeaking(text: String) = coroutineScope {
        startSpeakingDuration(listOf(text), 0, 1)
    }

    private suspend fun observePlaybackLoop(playbackJob: Job) = coroutineScope {
        while (isActive && !playbackJob.isCompleted) {
            val state = audioPlayer.playerState.value
            val prog = audioPlayer.playbackProgress.value

            when (state) {
                is PlayerState.Playing -> {
                    _engineState.value = EngineState.Playing(
                        currentMs = prog.currentMs,
                        totalMs = prog.totalMs,
                        progress = prog.progress,
                    )
                }
                is PlayerState.Paused -> {
                    _engineState.value = EngineState.Paused(
                        currentMs = prog.currentMs,
                        totalMs = prog.totalMs,
                        progress = prog.progress,
                    )
                }
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

    suspend fun pause() {
        audioPlayer.pause()
        val prog = audioPlayer.playbackProgress.value
        _engineState.value = EngineState.Paused(prog.currentMs, prog.totalMs, prog.progress)
    }

    suspend fun resume() {
        audioPlayer.play()
        val prog = audioPlayer.playbackProgress.value
        _engineState.value = EngineState.Playing(prog.currentMs, prog.totalMs, prog.progress)
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

    fun updateConfig(config: TtsConfig) {
        this.config = config
        audioPlayer.setVolume(config.volume)
        audioPlayer.setSpeed(config.speed)
    }

    fun release() {
        currentJob?.cancel()
        audioPlayer.release()
        inference.close()
        _engineState.value = EngineState.Idle
    }

    private suspend fun generateAudioSegment(sentence: String): AudioSegment = withContext(Dispatchers.Default) {
        val tokenIds = tokenizer.tokenize(sentence)
        val audioSamples = inference.infer(tokenIds)

        val pcmSamples = ShortArray(audioSamples.size) { i ->
            val sample = (audioSamples[i] * Short.MAX_VALUE).roundToInt()
            sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val durationMs = (pcmSamples.size * 1000L) / 24000

        AudioSegment(
            text = sentence,
            sampleRate = 24000,
            samples = pcmSamples,
            durationMs = durationMs,
        )
    }

    private fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val pattern = Regex("(?<=[.!?])\\s+(?=[A-Z\"''])")

        val raw = text.split(pattern)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val merged = mutableListOf<String>()
        var pending = ""
        for (sentence in raw) {
            pending = if (pending.isNotEmpty()) "$pending $sentence" else sentence
            if (pending.length >= 15) {
                merged.add(pending)
                pending = ""
            }
        }
        if (pending.isNotEmpty()) {
            if (merged.isNotEmpty()) {
                merged[merged.lastIndex] = "${merged.last()} $pending"
            } else {
                merged.add(pending)
            }
        }

        return merged.ifEmpty { listOf(text.trim()) }
    }
}
