package com.example.readproplus.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.readproplus.MainActivity
import com.example.readproplus.R
import com.example.readproplus.data.AudiobookRepository
import com.example.readproplus.data.BookCache
import com.example.readproplus.data.PdfRepository
import com.example.readproplus.model.audiobook.Audiobook
import com.example.readproplus.model.audiobook.AudiobookManifest
import com.example.readproplus.model.audiobook.AudiobookStatus
import com.example.readproplus.model.tts.TtsConfig
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.pdf.PdfExtractor
import com.example.readproplus.pdf.PdfFileResolver
import com.example.readproplus.pdf.PdfSpeechTextFilter
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Foreground Service for background Audiobook TTS generation with 5% Checkpoints.
 * - Flushes WAV header & updates manifest every 5% of pages.
 * - Makes audiobook playable immediately after the first checkpoint.
 * - Supports resume from the last completed checkpoint if interrupted.
 */
class AudiobookProcessingService : Service() {

    companion object {
        private const val TAG = "AudiobookProcService"
        const val CHANNEL_ID = "audiobook_processing_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.readproplus.tts.ACTION_START"
        const val ACTION_PAUSE = "com.example.readproplus.tts.ACTION_PAUSE"
        const val ACTION_CANCEL = "com.example.readproplus.tts.ACTION_CANCEL"

        const val EXTRA_AUDIOBOOK_ID = "extra_audiobook_id"

        var onCheckpointCallback: ((Audiobook) -> Unit)? = null
        var onStatusChangedCallback: ((Audiobook) -> Unit)? = null

        fun startProcessing(context: Context, audiobookId: String) {
            val intent = Intent(context, AudiobookProcessingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_AUDIOBOOK_ID, audiobookId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseProcessing(context: Context, audiobookId: String) {
            val intent = Intent(context, AudiobookProcessingService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_AUDIOBOOK_ID, audiobookId)
            }
            context.startService(intent)
        }

        fun cancelProcessing(context: Context, audiobookId: String) {
            val intent = Intent(context, AudiobookProcessingService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_AUDIOBOOK_ID, audiobookId)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var processingJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var repository: AudiobookRepository
    private lateinit var pdfRepository: PdfRepository
    private lateinit var piperEngine: PiperEngine
    private lateinit var kokoroEngine: KokoroEngine
    private lateinit var kokoroModelManager: ModelManager
    private var isKokoroInited = false

    private var activeAudiobookId: String? = null

    override fun onCreate() {
        super.onCreate()
        runCatching { PDFBoxResourceLoader.init(applicationContext) }
        repository = AudiobookRepository(applicationContext)
        val resolver = PdfFileResolver(applicationContext)
        val extractor = PdfExtractor(resolver)
        val cache = BookCache(applicationContext).apply { runCatching { load() } }
        pdfRepository = PdfRepository(applicationContext, extractor, cache)

        val piperModelManager = PiperModelManager(applicationContext)
        piperEngine = PiperEngine(applicationContext, piperModelManager, TtsConfig())

        kokoroModelManager = ModelManager(applicationContext)
        val voiceManager = VoiceManager(applicationContext)
        val tokenizer = KokoroTokenizer(applicationContext)
        val inference = KokoroInference()
        kokoroEngine = KokoroEngine(inference, tokenizer, voiceManager, TtsConfig())

        createNotificationChannel()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ReadProPlus:AudiobookWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val audiobookId = intent.getStringExtra(EXTRA_AUDIOBOOK_ID) ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification("Hazırlanıyor...", 0, 100))
                startProcessingJob(audiobookId)
            }
            ACTION_PAUSE -> {
                pauseProcessingJob(audiobookId)
            }
            ACTION_CANCEL -> {
                cancelProcessingJob(audiobookId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startProcessingJob(audiobookId: String) {
        if (activeAudiobookId == audiobookId && processingJob?.isActive == true) {
            return
        }

        processingJob?.cancel()
        activeAudiobookId = audiobookId

        processingJob = serviceScope.launch {
            wakeLock?.acquire(4 * 60 * 60 * 1000L) // up to 4 hours wake lock
            try {
                processAudiobook(audiobookId)
            } catch (e: CancellationException) {
                Log.i(TAG, "Audiobook processing cancelled: $audiobookId")
            } catch (e: Exception) {
                Log.e(TAG, "Audiobook processing error", e)
                val book = repository.getAudiobook(audiobookId)
                if (book != null) {
                    val updated = book.copy(
                        status = AudiobookStatus.ERROR,
                        errorMessage = e.message ?: "Unknown error",
                    )
                    repository.saveAudiobook(updated)
                    onStatusChangedCallback?.invoke(updated)
                }
            } finally {
                wakeLock?.let { if (it.isHeld) it.release() }
                if (processingJob?.isActive != true) {
                    stopForeground(false)
                }
            }
        }
    }

    private suspend fun processAudiobook(audiobookId: String) = withContext(Dispatchers.IO) {
        var audiobook = repository.getAudiobook(audiobookId) ?: return@withContext
        runCatching { PDFBoxResourceLoader.init(applicationContext) }

        var bookDoc = pdfRepository.getBook(audiobook.bookId)
        if (bookDoc == null) {
            val cache = BookCache(applicationContext).apply { runCatching { load() } }
            pdfRepository = PdfRepository(applicationContext, PdfExtractor(PdfFileResolver(applicationContext)), cache)
            bookDoc = pdfRepository.getBook(audiobook.bookId)
        }

        // Find selected voice
        val voice = TtsVoice.fromId(audiobook.voiceId)
        val isPiper = voice.backend == com.example.readproplus.model.tts.TtsBackend.PIPER

        // Initialize engine
        if (isPiper) {
            if (!piperEngine.isReady()) {
                piperEngine.initialize(voice)
            }
        } else {
            if (!isKokoroInited) {
                check(kokoroModelManager.isModelReady()) {
                    "The bundled Kokoro model is not available."
                }
                val modelPath = kokoroModelManager.getModelPath()
                    ?: error("The Kokoro model path is not available after setup.")
                kokoroEngine.initialize(voice)
                kokoroEngine.loadModel(modelPath)
                isKokoroInited = true
            } else {
                kokoroEngine.initialize(voice)
            }
        }

        // Prepare manifest & resume position
        var manifest = repository.getManifest(audiobook.bookId) ?: AudiobookManifest(
            bookId = audiobook.bookId,
            lastProcessedPage = 0,
            totalDurationMs = 0L,
            totalSampleCount = 0L,
        )

        val totalPagesToProcess = (audiobook.endPage - audiobook.startPage + 1).coerceAtLeast(1)
        val resumePage = (manifest.lastProcessedPage + 1).coerceAtLeast(audiobook.startPage)

        if (resumePage > audiobook.endPage) {
            val completed = audiobook.copy(
                status = AudiobookStatus.COMPLETED,
                isPlayable = true,
                checkpointProgress = 1.0f,
                processedPages = totalPagesToProcess,
                checkpointDurationMs = manifest.totalDurationMs,
            )
            repository.saveAudiobook(completed)
            onStatusChangedCallback?.invoke(completed)
            updateNotification("Seslendirme tamamlandı", 100, 100)
            return@withContext
        }

        audiobook = audiobook.copy(status = AudiobookStatus.PROCESSING)
        repository.saveAudiobook(audiobook)
        onStatusChangedCallback?.invoke(audiobook)

        // Pre-extract page texts for the entire range in a single pass
        val pageTextsMap: Map<Int, String> = if (bookDoc != null) {
            pdfRepository.extractPageRangeText(
                document = bookDoc,
                startPageIndex = audiobook.startPage - 1,
                endPageIndex = audiobook.endPage - 1,
            )
        } else {
            val uriStr = audiobook.sourceUri ?: audiobook.filePath ?: audiobook.bookId
            val uri = runCatching { Uri.parse(uriStr) }.getOrNull()
            if (uri != null) {
                val extractor = PdfExtractor(PdfFileResolver(applicationContext))
                extractor.extractPageRangeText(
                    uri = uri,
                    startPageIndex = audiobook.startPage - 1,
                    endPageIndex = audiobook.endPage - 1,
                )
            } else {
                emptyMap()
            }
        }

        val hasAnyText = pageTextsMap.values.any { it.isNotBlank() }
        if (!hasAnyText) {
            Log.e(TAG, "No selectable text in page range ${audiobook.startPage}..${audiobook.endPage} for book ${audiobook.title}")
            val errorBook = audiobook.copy(
                status = AudiobookStatus.ERROR,
                errorMessage = "Belgede seslendirilebilecek metin bulunamadı. Lütfen belgenin taranmış/görsel olmadığını kontrol edin.",
            )
            repository.saveAudiobook(errorBook)
            onStatusChangedCallback?.invoke(errorBook)
            updateNotification("Seslendirme başarısız: Metin bulunamadı", 0, 0)
            return@withContext
        }

        // Checkpoint interval: every 5% of pages, or at least every 1 page for short books
        val checkpointStep = (totalPagesToProcess * 0.05f).roundToInt().coerceAtLeast(1)

        for (page in resumePage..audiobook.endPage) {
            if (!isActive) break

            val currentProcessedCount = page - audiobook.startPage + 1
            val progressPercent = (currentProcessedCount * 100 / totalPagesToProcess).coerceIn(0, 100)

            val rawText = pageTextsMap[page - 1] ?: ""
            val cleanText = if (audiobook.mainTextOnly) {
                PdfSpeechTextFilter.mainTextPages(listOf(rawText)).firstOrNull()?.takeIf { it.isNotBlank() }
                    ?: rawText.trim()
            } else {
                rawText.trim()
            }

            Log.i(TAG, "Synthesizing page $page/${audiobook.endPage}, raw text chars: ${rawText.length}, clean chars: ${cleanText.length}")

            // Synthesize PCM samples
            val samples = if (cleanText.isNotBlank()) {
                if (isPiper) {
                    piperEngine.synthesizeTextToSamples(cleanText)
                } else {
                    kokoroEngine.synthesizeTextToSamples(cleanText)
                }
            } else {
                Log.w(TAG, "Page $page has no text, generating 0.1s silence")
                ShortArray(2400) { 0 }
            }

            // Append to WAV file and commit marker
            manifest = repository.appendPageAudioAndCommit(audiobook, page, samples, manifest)

            // Checkpoint milestone logic
            val isCheckpointMilestone = (currentProcessedCount % checkpointStep == 0) || (page == audiobook.endPage)
            val checkpointProgress = (currentProcessedCount.toFloat() / totalPagesToProcess).coerceIn(0f, 1f)
            val isPlayable = checkpointProgress >= 0.05f || page == audiobook.endPage || audiobook.isPlayable

            if (isCheckpointMilestone) {
                val isDone = page == audiobook.endPage
                audiobook = audiobook.copy(
                    processedPages = currentProcessedCount,
                    checkpointProgress = checkpointProgress,
                    checkpointDurationMs = manifest.totalDurationMs,
                    totalEstimatedDurationMs = ((manifest.totalDurationMs.toDouble() / currentProcessedCount) * totalPagesToProcess).toLong(),
                    isPlayable = isPlayable,
                    status = if (isDone) AudiobookStatus.COMPLETED else AudiobookStatus.PROCESSING,
                )
                repository.saveAudiobook(audiobook)
                onCheckpointCallback?.invoke(audiobook)
                onStatusChangedCallback?.invoke(audiobook)

                val checkpointPercent = (checkpointProgress * 100).toInt()
                val notifText = "Sayfa $page/${audiobook.endPage} (%$progressPercent) • %$checkpointPercent Checkpoint Hazır"
                updateNotification(notifText, progressPercent, 100)
            } else {
                val notifText = "Sayfa $page/${audiobook.endPage} (%$progressPercent)"
                updateNotification(notifText, progressPercent, 100)
            }
        }

        if (audiobook.processedPages >= totalPagesToProcess) {
            val finalBook = audiobook.copy(
                status = AudiobookStatus.COMPLETED,
                isPlayable = true,
                checkpointProgress = 1.0f,
            )
            repository.saveAudiobook(finalBook)
            onStatusChangedCallback?.invoke(finalBook)
            updateNotification("Seslendirme tamamlandı: ${finalBook.title}", 100, 100)
        }
    }

    private fun pauseProcessingJob(audiobookId: String) {
        if (activeAudiobookId == audiobookId) {
            processingJob?.cancel()
            processingJob = null
            val book = repository.getAudiobook(audiobookId)
            if (book != null) {
                val paused = book.copy(status = AudiobookStatus.PAUSED)
                repository.saveAudiobook(paused)
                onStatusChangedCallback?.invoke(paused)
            }
            updateNotification("Seslendirme duraklatıldı", 0, 0)
            stopForeground(false)
        }
    }

    private fun cancelProcessingJob(audiobookId: String) {
        if (activeAudiobookId == audiobookId) {
            processingJob?.cancel()
            processingJob = null
            val book = repository.getAudiobook(audiobookId)
            if (book != null) {
                val paused = book.copy(status = AudiobookStatus.PAUSED)
                repository.saveAudiobook(paused)
                onStatusChangedCallback?.invoke(paused)
            }
            stopForeground(true)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sesli Kitap Seslendirme",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Arka planda sesli kitap oluşturma ilerlemesi"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, progress: Int, max: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingLaunch = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sesli Kitap Üretiliyor")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setProgress(max, progress, max == 0)
            .setContentIntent(pendingLaunch)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String, progress: Int, max: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text, progress, max))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
