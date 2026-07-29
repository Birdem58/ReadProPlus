package com.example.readproplus.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.readproplus.model.tts.TtsState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Reliable reader fallback for devices where the bundled neural runtime cannot
 * start. It uses the device's installed text-to-speech engine and preserves the
 * same playback controls as the Kokoro reader.
 */
class SystemTtsReader(context: Context) {

    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = null
    private var initialization: CompletableDeferred<Result<Unit>>? = null
    private var utterances: List<Utterance> = emptyList()
    private var activeIndex = 0
    private var playbackSession = 0L
    private var speechRate = DEFAULT_SPEED
    private var volume = DEFAULT_VOLUME

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    suspend fun startReading(
        pages: List<String>,
        startPageIndex: Int,
        endPageIndex: Int,
    ): Result<Unit> {
        val initResult = ensureInitialized()
        if (initResult.isFailure) return initResult

        val sourcePages = pages.ifEmpty { listOf("No readable text is available.") }
        val startIndex = startPageIndex.coerceIn(0, sourcePages.lastIndex)
        val endIndex = endPageIndex.coerceIn(startIndex, sourcePages.lastIndex)
        val sourceSentences = sourcePages
            .subList(startIndex, endIndex + 1)
            .flatMap(::splitSentences)
            .ifEmpty { listOf("No readable text is available.") }

        val selected = sourceSentences.map { sentence ->
            Utterance(sentence, estimateDurationMs(sentence))
        }
        val estimatedDuration = selected.sumOf(Utterance::estimatedDurationMs)
        utterances = selected
        activeIndex = 0
        _state.value = TtsState.Generating(0L, estimatedDuration, 0f)
        return queueFrom(0)
    }

    fun pause() {
        if (utterances.isEmpty()) return
        playbackSession++
        textToSpeech?.stop()
        _state.value = pausedState(activeIndex)
    }

    fun resume(): Result<Unit> {
        if (utterances.isEmpty()) return Result.failure(IllegalStateException("Nothing is queued for speech."))
        return queueFrom(activeIndex)
    }

    fun seekTo(progress: Float): Result<Unit> {
        if (utterances.isEmpty()) return Result.failure(IllegalStateException("Nothing is queued for speech."))
        val targetDuration = (totalDurationMs() * progress.coerceIn(0f, 1f)).toLong()
        var elapsed = 0L
        activeIndex = utterances.indexOfFirst { utterance ->
            elapsed += utterance.estimatedDurationMs
            elapsed >= targetDuration
        }.takeIf { it >= 0 } ?: utterances.lastIndex
        return queueFrom(activeIndex)
    }

    fun stop() {
        playbackSession++
        textToSpeech?.stop()
        utterances = emptyList()
        activeIndex = 0
        _state.value = TtsState.Stopped
    }

    fun setSpeed(speed: Float) {
        speechRate = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(NORMAL_PITCH)
    }

    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
    }

    fun shutdown() {
        playbackSession++
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        initialization = null
        utterances = emptyList()
        _state.value = TtsState.Idle
    }

    private suspend fun ensureInitialized(): Result<Unit> {
        initialization?.let { return it.await() }

        val deferred = CompletableDeferred<Result<Unit>>()
        initialization = deferred
        textToSpeech = TextToSpeech(appContext) { status ->
            val engine = textToSpeech
            if (status != TextToSpeech.SUCCESS || engine == null) {
                deferred.complete(
                    Result.failure(IllegalStateException("Android text-to-speech is unavailable on this device.")),
                )
                return@TextToSpeech
            }

            val languageResult = engine.setLanguage(Locale.US)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                deferred.complete(
                    Result.failure(IllegalStateException("An English text-to-speech voice is not installed.")),
                )
                return@TextToSpeech
            }

            engine.setSpeechRate(speechRate)
            engine.setPitch(NORMAL_PITCH)
            engine.setOnUtteranceProgressListener(progressListener)
            deferred.complete(Result.success(Unit))
        }
        return deferred.await()
    }

    private fun queueFrom(startIndex: Int): Result<Unit> {
        val engine = textToSpeech
            ?: return Result.failure(IllegalStateException("Android text-to-speech is not initialized."))
        if (startIndex !in utterances.indices) {
            return Result.failure(IllegalArgumentException("Requested speech position is outside the queue."))
        }

        playbackSession++
        val session = playbackSession
        activeIndex = startIndex
        utterances.drop(startIndex).forEachIndexed { relativeIndex, utterance ->
            val queueMode = if (relativeIndex == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val result = engine.speak(
                utterance.text,
                queueMode,
                Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
                },
                utteranceId(session, startIndex + relativeIndex),
            )
            if (result != TextToSpeech.SUCCESS) {
                _state.value = TtsState.Error("Android text-to-speech could not queue the reader audio.", true)
                return Result.failure(IllegalStateException("Text-to-speech queueing failed."))
            }
        }
        return Result.success(Unit)
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            val index = indexForActiveSession(utteranceId) ?: return
            activeIndex = index
            _state.value = playingState(index)
        }

        override fun onDone(utteranceId: String) {
            val index = indexForActiveSession(utteranceId) ?: return
            if (index == utterances.lastIndex) {
                _state.value = TtsState.Stopped
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
            if (indexForActiveSession(utteranceId) != null) {
                _state.value = TtsState.Error("Android text-to-speech could not generate reader audio.", true)
            }
        }
    }

    private fun indexForActiveSession(utteranceId: String): Int? {
        val prefix = "$UTTERANCE_PREFIX$playbackSession:"
        if (!utteranceId.startsWith(prefix)) return null
        return utteranceId.removePrefix(prefix).toIntOrNull()?.takeIf { it in utterances.indices }
    }

    private fun utteranceId(session: Long, index: Int): String = "$UTTERANCE_PREFIX$session:$index"

    private fun playingState(index: Int): TtsState.Playing {
        val total = totalDurationMs()
        val current = elapsedDurationBefore(index)
        return TtsState.Playing(
            currentMs = current,
            totalMs = total,
            progress = progressFor(current, total),
            speed = speechRate,
        )
    }

    private fun pausedState(index: Int): TtsState.Paused {
        val total = totalDurationMs()
        val current = elapsedDurationBefore(index)
        return TtsState.Paused(
            currentMs = current,
            totalMs = total,
            progress = progressFor(current, total),
            speed = speechRate,
        )
    }

    private fun totalDurationMs(): Long = utterances.sumOf(Utterance::estimatedDurationMs)

    private fun elapsedDurationBefore(index: Int): Long = utterances.take(index).sumOf(Utterance::estimatedDurationMs)

    private fun progressFor(current: Long, total: Long): Float =
        if (total == 0L) 0f else (current.toFloat() / total).coerceIn(0f, 1f)

    private fun estimateDurationMs(text: String): Long {
        val words = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }.coerceAtLeast(1)
        return ((words * MILLIS_PER_MINUTE) / (WORDS_PER_MINUTE * speechRate)).toLong()
            .coerceAtLeast(MIN_UTTERANCE_DURATION_MS)
    }

    private fun splitSentences(text: String): List<String> = text
        .split(Regex("(?<=[.!?])\\s+"))
        .flatMap(::splitLongSentence)
        .map(String::trim)
        .filter(String::isNotBlank)

    private fun splitLongSentence(sentence: String): List<String> {
        if (sentence.length <= MAX_UTTERANCE_CHARACTERS) return listOf(sentence)
        val chunks = mutableListOf<String>()
        var remaining = sentence.trim()
        while (remaining.length > MAX_UTTERANCE_CHARACTERS) {
            val breakIndex = remaining.lastIndexOf(' ', MAX_UTTERANCE_CHARACTERS)
                .takeIf { it > 0 } ?: MAX_UTTERANCE_CHARACTERS
            chunks += remaining.substring(0, breakIndex).trim()
            remaining = remaining.substring(breakIndex).trim()
        }
        if (remaining.isNotBlank()) chunks += remaining
        return chunks
    }

    private data class Utterance(val text: String, val estimatedDurationMs: Long)

    private companion object {
        const val UTTERANCE_PREFIX = "readpro_system_tts_"
        const val MAX_UTTERANCE_CHARACTERS = 3_500
        const val WORDS_PER_MINUTE = 165f
        const val MILLIS_PER_MINUTE = 60_000L
        const val MIN_UTTERANCE_DURATION_MS = 250L
        const val DEFAULT_SPEED = 1f
        const val DEFAULT_VOLUME = 1f
        const val NORMAL_PITCH = 1f
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2f
    }
}
