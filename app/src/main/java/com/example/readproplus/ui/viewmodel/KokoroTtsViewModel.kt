package com.example.readproplus.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.readproplus.data.GeneratedAudioRepository
import com.example.readproplus.data.TtsPreferences
import com.example.readproplus.model.tts.GeneratedAudio
import com.example.readproplus.model.tts.TtsConfig
import com.example.readproplus.model.tts.TtsState
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.pdf.PdfSpeechTextFilter
import com.example.readproplus.tts.DownloadProgress
import com.example.readproplus.tts.EngineState
import com.example.readproplus.tts.KokoroEngine
import com.example.readproplus.tts.KokoroInference
import com.example.readproplus.tts.KokoroTokenizer
import com.example.readproplus.tts.ModelManager
import com.example.readproplus.tts.SystemTtsReader
import com.example.readproplus.tts.VoiceManager
import com.example.readproplus.tts.VoiceDownloadProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KokoroTtsViewModel(application: Application) : AndroidViewModel(application) {

    private val modelManager = ModelManager(application)
    private val voiceManager = VoiceManager(application)
    private val tokenizer = KokoroTokenizer(application)
    private val inference = KokoroInference()
    private val preferences = TtsPreferences(application)
    private val generatedAudioRepository = GeneratedAudioRepository(application)
    private val currentConfig = MutableStateFlow(TtsConfig())
    private val engine = KokoroEngine(inference, tokenizer, voiceManager, currentConfig.value)
    private val systemTts = SystemTtsReader(application)

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private val _generatedAudios = MutableStateFlow<List<GeneratedAudio>>(emptyList())
    val generatedAudios: StateFlow<List<GeneratedAudio>> = _generatedAudios.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val _volume = MutableStateFlow(currentConfig.value.volume)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _selectedVoice = MutableStateFlow(TtsVoice.NICOLE)
    val selectedVoice: StateFlow<TtsVoice> = _selectedVoice.asStateFlow()

    private val _voiceAvailability = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val voiceAvailability: StateFlow<Map<String, Boolean>> = _voiceAvailability.asStateFlow()

    private val _voiceDownloadProgress = MutableStateFlow<VoiceDownloadProgress?>(null)
    val voiceDownloadProgress: StateFlow<VoiceDownloadProgress?> = _voiceDownloadProgress.asStateFlow()

    private var speakingJob: Job? = null
    private var voiceDownloadJob: Job? = null
    private var volumeSaveJob: Job? = null
    private var isEngineInitialized = false
    private var generatedAudioBookId: String? = null

    @Volatile
    private var activeBackend = ReaderBackend.NONE

    init {
        observeEngineState()
        viewModelScope.launch {
            systemTts.state.collect { state ->
                if (activeBackend == ReaderBackend.SYSTEM) {
                    _ttsState.value = state
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                val storedVoice = TtsVoice.fromId(preferences.voiceId.first())
                val restoredVoice = withContext(Dispatchers.IO) {
                    storedVoice.takeIf(voiceManager::isVoiceAvailable) ?: TtsVoice.NICOLE
                }
                val config = TtsConfig(
                    speed = preferences.speed.first(),
                    volume = preferences.volume.first(),
                    voiceId = restoredVoice.id,
                )
                currentConfig.value = config
                _volume.value = config.volume
                _selectedVoice.value = restoredVoice
                engine.updateConfig(config)
                systemTts.setSpeed(config.speed)
                systemTts.setVolume(config.volume)
                if (storedVoice.id != restoredVoice.id) {
                    preferences.setVoiceId(restoredVoice.id)
                }
                refreshVoiceAvailability()
            }.onFailure { error ->
                Log.w(TAG, "Could not restore TTS preferences", error)
                refreshVoiceAvailability()
            }
        }
    }

    fun checkAndDownloadModel() {
        viewModelScope.launch {
            if (!hasUsableKokoroAssets() || !ensureNeuralEngine()) {
                _ttsState.value = TtsState.Error(
                    "Kokoro could not start. Use the reader button to continue with the device voice.",
                    recoverable = true,
                )
            }
        }
    }

    fun loadGeneratedAudios(bookId: String?) {
        generatedAudioBookId = bookId
        if (bookId == null) {
            _generatedAudios.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val items = generatedAudioRepository.listByBook(bookId)
            if (generatedAudioBookId == bookId) {
                _generatedAudios.value = items
            }
        }
    }

    fun startPageRangeReading(
        bookId: String?,
        bookTitle: String,
        pages: List<String>,
        startPage: Int,
        endPage: Int,
        mainTextOnly: Boolean = false,
    ) {
        if (pages.isEmpty()) {
            _ttsState.value = TtsState.Error("There are no pages available to read.", recoverable = true)
            return
        }

        val safeStartPage = startPage.coerceIn(1, pages.size)
        val safeEndPage = endPage.coerceIn(safeStartPage, pages.size)
        speakingJob?.cancel()
        speakingJob = viewModelScope.launch {
            val pagesForSpeech = if (mainTextOnly) {
                withContext(Dispatchers.Default) { PdfSpeechTextFilter.mainTextPages(pages) }
            } else {
                pages
            }
            val canUseKokoro = hasUsableKokoroAssets() && ensureNeuralEngine()
            if (!canUseKokoro) {
                startSystemReading(pagesForSpeech, safeStartPage - 1, safeEndPage - 1)
                return@launch
            }

            activeBackend = ReaderBackend.KOKORO
            try {
                engine.startSpeakingRange(
                    pages = pagesForSpeech,
                    startPageIndex = safeStartPage - 1,
                    endPageIndex = safeEndPage - 1,
                ) { samples, durationMs ->
                    if (bookId != null) {
                        val updatedItems = withContext(Dispatchers.IO) {
                            generatedAudioRepository.save(
                                bookId = bookId,
                                bookTitle = bookTitle,
                                startPage = safeStartPage,
                                endPage = safeEndPage,
                                durationMs = durationMs,
                                samples = samples,
                            )
                            generatedAudioRepository.listByBook(bookId)
                        }
                        if (generatedAudioBookId == bookId) {
                            _generatedAudios.value = updatedItems
                        }
                    }
                }
                if (engine.engineState.value is EngineState.Error) {
                    startSystemReading(pagesForSpeech, safeStartPage - 1, safeEndPage - 1)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "Kokoro playback failed; using the device voice instead", error)
                startSystemReading(pagesForSpeech, safeStartPage - 1, safeEndPage - 1)
            }
        }
    }

    fun startReadingOrInitialize(text: String) {
        startPageRangeReading(
            bookId = null,
            bookTitle = "Quick read",
            pages = listOf(text),
            startPage = 1,
            endPage = 1,
        )
    }

    fun playGeneratedAudio(audio: GeneratedAudio) {
        speakingJob?.cancel()
        speakingJob = viewModelScope.launch {
            activeBackend = ReaderBackend.KOKORO
            try {
                val samples = withContext(Dispatchers.IO) {
                    generatedAudioRepository.readSamples(audio)
                }
                engine.playSamples(samples)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                activeBackend = ReaderBackend.NONE
                _ttsState.value = TtsState.Error(
                    error.message ?: "Could not play the generated audio.",
                    recoverable = true,
                )
            }
        }
    }

    fun deleteGeneratedAudio(audio: GeneratedAudio) {
        if (_ttsState.value is TtsState.Playing || _ttsState.value is TtsState.Paused) {
            stopReading()
        }
        viewModelScope.launch(Dispatchers.IO) {
            generatedAudioRepository.delete(audio)
            val bookId = generatedAudioBookId ?: return@launch
            _generatedAudios.value = generatedAudioRepository.listByBook(bookId)
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            modelManager.cancelDownload()
            _downloadProgress.value = null
            _ttsState.value = TtsState.Idle
        }
    }

    fun retryDownload() = checkAndDownloadModel()

    fun pauseReading() {
        when (activeBackend) {
            ReaderBackend.KOKORO -> viewModelScope.launch { engine.pause() }
            ReaderBackend.SYSTEM -> systemTts.pause()
            ReaderBackend.NONE -> Unit
        }
    }

    fun resumeReading() {
        when (activeBackend) {
            ReaderBackend.KOKORO -> viewModelScope.launch { engine.resume() }
            ReaderBackend.SYSTEM -> systemTts.resume().exceptionOrNull()?.let { error ->
                _ttsState.value = TtsState.Error(error.message ?: "Could not resume device text-to-speech.", true)
            }
            ReaderBackend.NONE -> Unit
        }
    }

    fun seekTo(progress: Float) {
        when (activeBackend) {
            ReaderBackend.KOKORO -> viewModelScope.launch { engine.seekTo(progress) }
            ReaderBackend.SYSTEM -> systemTts.seekTo(progress).exceptionOrNull()?.let { error ->
                _ttsState.value = TtsState.Error(error.message ?: "Could not seek device text-to-speech.", true)
            }
            ReaderBackend.NONE -> Unit
        }
    }

    fun stopReading() {
        speakingJob?.cancel()
        when (activeBackend) {
            ReaderBackend.KOKORO -> viewModelScope.launch { engine.stop() }
            ReaderBackend.SYSTEM -> systemTts.stop()
            ReaderBackend.NONE -> Unit
        }
        activeBackend = ReaderBackend.NONE
        _ttsState.value = TtsState.Stopped
    }

    fun setSpeed(speed: Float) {
        val safeSpeed = speed.takeIf { it.isFinite() }?.coerceIn(MIN_SPEED, MAX_SPEED) ?: DEFAULT_SPEED
        val updated = currentConfig.value.copy(speed = safeSpeed)
        currentConfig.value = updated
        engine.updateConfig(updated)
        systemTts.setSpeed(updated.speed)
        viewModelScope.launch { preferences.setSpeed(updated.speed) }
    }

    fun setVolume(volume: Float) {
        val safeVolume = volume.takeIf { it.isFinite() }?.coerceIn(MIN_VOLUME, MAX_VOLUME) ?: DEFAULT_VOLUME
        val updated = currentConfig.value.copy(volume = safeVolume)
        currentConfig.value = updated
        _volume.value = updated.volume
        engine.updateConfig(updated)
        systemTts.setVolume(updated.volume)
        volumeSaveJob?.cancel()
        volumeSaveJob = viewModelScope.launch {
            delay(VOLUME_SAVE_DEBOUNCE_MS)
            preferences.setVolume(updated.volume)
        }
    }

    fun selectVoice(voice: TtsVoice) {
        if (_voiceAvailability.value[voice.id] == true || voice.bundledAssetPath != null) {
            applyVoice(voice)
        } else {
            downloadAndSelectVoice(voice)
        }
    }

    fun dismissError() {
        if (_ttsState.value is TtsState.Error) {
            systemTts.stop()
            activeBackend = ReaderBackend.NONE
            _ttsState.value = TtsState.Idle
        }
    }

    override fun onCleared() {
        speakingJob?.cancel()
        voiceDownloadJob?.cancel()
        volumeSaveJob?.cancel()
        systemTts.shutdown()
        engine.release()
        super.onCleared()
    }

    private fun observeEngineState() {
        viewModelScope.launch {
            engine.engineState.collect { state ->
                if (activeBackend == ReaderBackend.KOKORO) {
                    _ttsState.value = mapEngineStateToTtsState(state)
                }
            }
        }
    }

    private suspend fun hasUsableKokoroAssets(): Boolean = withContext(Dispatchers.IO) {
        voiceManager.isVoiceAvailable(_selectedVoice.value) &&
            tokenizer.hasCompatibleVocabulary()
    }

    private suspend fun ensureNeuralEngine(): Boolean {
        if (isEngineInitialized) return true

        return try {
            if (!modelManager.isModelReady()) {
                modelManager.downloadModel().collect { progress ->
                    when (progress) {
                        is DownloadProgress.Downloading -> {
                            _downloadProgress.value = progress
                            _ttsState.value = TtsState.ModelDownloading(
                                progress = progress.progress,
                                bytesDownloaded = progress.bytesDownloaded,
                                totalBytes = progress.totalBytes,
                            )
                        }

                        is DownloadProgress.Complete -> {
                            _downloadProgress.value = progress
                            preferences.setModelDownloaded(true)
                        }

                        is DownloadProgress.Error -> throw IllegalStateException(progress.message)
                    }
                }
            }

            val modelPath = modelManager.getModelPath()
                ?: throw IllegalStateException("Kokoro model file is not available after setup.")
            val voice = _selectedVoice.value
            require(withContext(Dispatchers.IO) { voiceManager.isVoiceAvailable(voice) }) {
                "The ${voice.displayName} voice is not available. Download it before reading."
            }
            engine.initialize(voice)
            withContext(Dispatchers.Default) {
                engine.loadModel(modelPath)
            }
            isEngineInitialized = true
            true
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Log.w(TAG, "Kokoro initialization failed", error)
            engine.release()
            isEngineInitialized = false
            false
        }
    }

    private suspend fun startSystemReading(
        pages: List<String>,
        startPageIndex: Int,
        endPageIndex: Int,
    ) {
        activeBackend = ReaderBackend.SYSTEM
        systemTts.startReading(pages, startPageIndex, endPageIndex)
            .exceptionOrNull()
            ?.let { error ->
                activeBackend = ReaderBackend.NONE
                _ttsState.value = TtsState.Error(
                    "No text-to-speech engine could start: ${error.message}",
                    recoverable = true,
                )
            }
    }

    private fun mapEngineStateToTtsState(engineState: EngineState): TtsState {
        val speed = currentConfig.value.speed
        return when (engineState) {
            EngineState.Idle -> TtsState.Idle
            is EngineState.Generating -> TtsState.Generating(
                generatedMs = engineState.generatedMs,
                targetMs = engineState.targetMs,
                progress = if (engineState.targetMs > 0L) {
                    (engineState.generatedMs.toFloat() / engineState.targetMs).coerceIn(0f, 1f)
                } else {
                    0f
                },
            )

            is EngineState.Playing -> TtsState.Playing(
                currentMs = engineState.currentMs,
                totalMs = engineState.totalMs,
                progress = engineState.progress,
                speed = speed,
            )

            is EngineState.Paused -> TtsState.Paused(
                currentMs = engineState.currentMs,
                totalMs = engineState.totalMs,
                progress = engineState.progress,
                speed = speed,
            )

            EngineState.Stopped -> TtsState.Stopped
            is EngineState.Error -> TtsState.Error(engineState.message, recoverable = true)
        }
    }

    private fun downloadAndSelectVoice(voice: TtsVoice) {
        voiceDownloadJob?.cancel()
        voiceDownloadJob = viewModelScope.launch {
            voiceManager.downloadVoice(voice).collect { progress ->
                _voiceDownloadProgress.value = progress
                when (progress) {
                    is VoiceDownloadProgress.Complete -> {
                        refreshVoiceAvailability()
                        applyVoice(voice)
                    }

                    is VoiceDownloadProgress.Downloading,
                    is VoiceDownloadProgress.Error -> Unit
                }
            }
        }
    }

    private fun applyVoice(voice: TtsVoice) {
        if (_selectedVoice.value == voice && isEngineInitialized) return

        speakingJob?.cancel()
        systemTts.stop()
        activeBackend = ReaderBackend.NONE
        engine.release()
        isEngineInitialized = false

        _selectedVoice.value = voice
        currentConfig.value = currentConfig.value.copy(voiceId = voice.id)
        _ttsState.value = TtsState.Stopped
        viewModelScope.launch { preferences.setVoiceId(voice.id) }
    }

    private fun refreshVoiceAvailability() {
        viewModelScope.launch(Dispatchers.IO) {
            _voiceAvailability.value = TtsVoice.ALL.associate { voice ->
                voice.id to voiceManager.isVoiceAvailable(voice)
            }
        }
    }

    private enum class ReaderBackend {
        NONE,
        KOKORO,
        SYSTEM,
    }

    private companion object {
        const val TAG = "KokoroTtsViewModel"
        const val DEFAULT_SPEED = 1f
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2f
        const val DEFAULT_VOLUME = 1f
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
        const val VOLUME_SAVE_DEBOUNCE_MS = 250L
    }
}
