package com.example.readproplus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.readproplus.data.AudiobookRepository
import com.example.readproplus.model.audiobook.Audiobook
import com.example.readproplus.model.audiobook.AudiobookStatus
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.tts.AudiobookPlaybackState
import com.example.readproplus.tts.AudiobookPlayer
import com.example.readproplus.tts.AudiobookProcessingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AudiobookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudiobookRepository(application)
    private val player = AudiobookPlayer(application, repository)

    private val _audiobooks = MutableStateFlow<List<Audiobook>>(emptyList())
    val audiobooks: StateFlow<List<Audiobook>> = _audiobooks.asStateFlow()

    val playbackState: StateFlow<AudiobookPlaybackState> = player.playbackState

    init {
        refreshAudiobooks()

        AudiobookProcessingService.onCheckpointCallback = { updatedBook ->
            viewModelScope.launch(Dispatchers.Main) {
                refreshAudiobooks()
                player.notifyCheckpointUpdated(updatedBook)
            }
        }

        AudiobookProcessingService.onStatusChangedCallback = { _ ->
            viewModelScope.launch(Dispatchers.Main) {
                refreshAudiobooks()
            }
        }
    }

    fun refreshAudiobooks() {
        _audiobooks.value = repository.getAllAudiobooks()
    }

    fun getAudiobook(id: String): Audiobook? {
        return repository.getAudiobook(id)
    }

    fun getAudiobookByBookId(bookId: String): Audiobook? {
        return repository.getAudiobookByBookId(bookId)
    }

    fun createAndStartAudiobook(
        document: PdfDocument,
        voice: TtsVoice,
        startPage: Int,
        endPage: Int,
        mainTextOnly: Boolean = true,
    ): Audiobook {
        val id = UUID.randomUUID().toString()
        val isPiper = voice.piperModelFileName != null
        val totalPagesToProcess = (endPage - startPage + 1).coerceAtLeast(1)

        val newAudiobook = Audiobook(
            id = id,
            bookId = document.id,
            title = document.title,
            author = document.author,
            coverPath = document.sourceUri ?: document.filePath,
            sourceUri = document.sourceUri,
            filePath = document.filePath,
            mainTextOnly = mainTextOnly,
            voiceId = voice.id,
            voiceName = voice.displayName,
            engineType = if (isPiper) "PIPER" else "KOKORO",
            language = if (isPiper) "tr" else "en",
            startPage = startPage,
            endPage = endPage,
            totalPages = document.totalPages,
            processedPages = 0,
            checkpointProgress = 0f,
            checkpointDurationMs = 0L,
            totalEstimatedDurationMs = totalPagesToProcess * 45_000L, // initial rough estimate 45s/page
            currentPositionMs = 0L,
            currentPageIndex = startPage,
            status = AudiobookStatus.QUEUED,
            isPlayable = false,
            audioFileName = "audiobook_${id}.wav",
        )

        repository.saveAudiobook(newAudiobook)
        refreshAudiobooks()

        AudiobookProcessingService.startProcessing(getApplication(), id)
        return newAudiobook
    }

    fun resumeProcessing(audiobookId: String) {
        AudiobookProcessingService.startProcessing(getApplication(), audiobookId)
        val book = repository.getAudiobook(audiobookId)
        if (book != null) {
            repository.saveAudiobook(book.copy(status = AudiobookStatus.PROCESSING))
            refreshAudiobooks()
        }
    }

    fun pauseProcessing(audiobookId: String) {
        AudiobookProcessingService.pauseProcessing(getApplication(), audiobookId)
        val book = repository.getAudiobook(audiobookId)
        if (book != null) {
            repository.saveAudiobook(book.copy(status = AudiobookStatus.PAUSED))
            refreshAudiobooks()
        }
    }

    fun deleteAudiobook(audiobookId: String) {
        AudiobookProcessingService.cancelProcessing(getApplication(), audiobookId)
        if (playbackState.value.audiobook?.id == audiobookId) {
            player.pause()
        }
        repository.deleteAudiobook(audiobookId)
        refreshAudiobooks()
    }

    fun playAudiobook(audiobook: Audiobook) {
        player.loadAudiobook(audiobook, startPlaying = true)
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun togglePlayPause() = player.togglePlayPause()
    fun seekTo(posMs: Long) = player.seekTo(posMs)
    fun skipForward15() = player.skipForward15()
    fun skipBackward15() = player.skipBackward15()
    fun seekToPage(pageIndex: Int) = player.seekToPage(pageIndex)
    fun setSpeed(speed: Float) = player.setPlaybackSpeed(speed)
    fun setSleepTimer(minutes: Int?) = player.setSleepTimerMinutes(minutes)
    fun setSleepTimerEndOfPage() = player.setSleepTimerEndOfPage()

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
