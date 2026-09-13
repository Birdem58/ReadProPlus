package com.example.readproplus.data

import android.content.Context
import com.example.readproplus.model.audiobook.Audiobook
import com.example.readproplus.model.audiobook.AudiobookManifest
import com.example.readproplus.model.audiobook.AudiobookPageMarker
import com.example.readproplus.model.audiobook.AudiobookStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Repository managing audiobook metadata, 24kHz Mono WAV files,
 * page manifests, and 5% checkpoint flushes.
 */
class AudiobookRepository(private val context: Context) {

    companion object {
        private const val DIRECTORY_NAME = "audiobooks"
        private const val INDEX_FILE_NAME = "audiobooks.json"
        private const val SAMPLE_RATE = 24000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val BYTES_PER_SAMPLE = 2
        private const val HEADER_SIZE = 44
    }

    private val baseDir = File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }
    private val audioDir = File(baseDir, "audio").apply { mkdirs() }
    private val indexFile = File(baseDir, INDEX_FILE_NAME)

    @Synchronized
    fun getAllAudiobooks(): List<Audiobook> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val json = indexFile.readText()
            val array = JSONArray(json)
            val list = mutableListOf<Audiobook>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(deserializeAudiobook(obj))
            }
            list.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getAudiobook(id: String): Audiobook? {
        return getAllAudiobooks().firstOrNull { it.id == id }
    }

    @Synchronized
    fun getAudiobookByBookId(bookId: String): Audiobook? {
        return getAllAudiobooks().firstOrNull { it.bookId == bookId }
    }

    @Synchronized
    fun saveAudiobook(audiobook: Audiobook) {
        val current = getAllAudiobooks().toMutableList()
        val index = current.indexOfFirst { it.id == audiobook.id }
        if (index >= 0) {
            current[index] = audiobook
        } else {
            current.add(0, audiobook)
        }
        persistAll(current)
    }

    @Synchronized
    fun deleteAudiobook(id: String) {
        val audiobook = getAudiobook(id) ?: return
        val current = getAllAudiobooks().filterNot { it.id == id }
        persistAll(current)

        // Delete audio and manifest
        getAudioFile(audiobook).delete()
        getManifestFile(audiobook.bookId).delete()
    }

    fun getAudioFile(audiobook: Audiobook): File {
        return File(audioDir, audiobook.audioFileName)
    }

    private fun getManifestFile(bookId: String): File {
        val safeName = bookId.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        return File(baseDir, "manifest_${safeName}.json")
    }

    @Synchronized
    fun getManifest(bookId: String): AudiobookManifest? {
        val file = getManifestFile(bookId)
        if (!file.exists()) return null
        return try {
            val obj = JSONObject(file.readText())
            val lastProcessedPage = obj.optInt("lastProcessedPage", 0)
            val totalDurationMs = obj.optLong("totalDurationMs", 0L)
            val totalSampleCount = obj.optLong("totalSampleCount", 0L)
            val markersArray = obj.optJSONArray("markers") ?: JSONArray()
            val markers = mutableListOf<AudiobookPageMarker>()
            for (i in 0 until markersArray.length()) {
                val m = markersArray.getJSONObject(i)
                markers.add(
                    AudiobookPageMarker(
                        pageIndex = m.getInt("pageIndex"),
                        startMs = m.getLong("startMs"),
                        endMs = m.getLong("endMs"),
                        sampleOffset = m.getLong("sampleOffset"),
                        sampleCount = m.getLong("sampleCount"),
                    )
                )
            }
            AudiobookManifest(
                bookId = bookId,
                lastProcessedPage = lastProcessedPage,
                totalDurationMs = totalDurationMs,
                totalSampleCount = totalSampleCount,
                markers = markers,
            )
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun saveManifest(manifest: AudiobookManifest) {
        val file = getManifestFile(manifest.bookId)
        val obj = JSONObject().apply {
            put("bookId", manifest.bookId)
            put("lastProcessedPage", manifest.lastProcessedPage)
            put("totalDurationMs", manifest.totalDurationMs)
            put("totalSampleCount", manifest.totalSampleCount)
            val markersArray = JSONArray()
            manifest.markers.forEach { m ->
                markersArray.put(
                    JSONObject().apply {
                        put("pageIndex", m.pageIndex)
                        put("startMs", m.startMs)
                        put("endMs", m.endMs)
                        put("sampleOffset", m.sampleOffset)
                        put("sampleCount", m.sampleCount)
                    }
                )
            }
            put("markers", markersArray)
        }
        file.writeText(obj.toString(2))
    }

    /**
     * Initializes the WAV container if it doesn't already exist.
     */
    @Synchronized
    fun prepareAudioFile(audiobook: Audiobook): File {
        val file = getAudioFile(audiobook)
        if (!file.exists() || file.length() < HEADER_SIZE) {
            file.parentFile?.mkdirs()
            file.createNewFile()
            RandomAccessFile(file, "rw").use { raf ->
                writeWavHeader(raf, 0L)
            }
        }
        return file
    }

    /**
     * Appends raw PCM samples to the audio file and commits the WAV header.
     */
    @Synchronized
    fun appendPageAudioAndCommit(
        audiobook: Audiobook,
        pageIndex: Int,
        samples: ShortArray,
        manifest: AudiobookManifest,
    ): AudiobookManifest {
        val file = prepareAudioFile(audiobook)
        val sampleBytes = ByteArray(samples.size * BYTES_PER_SAMPLE)
        val buffer = ByteBuffer.wrap(sampleBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            buffer.putShort(sample)
        }

        var newTotalSampleCount: Long
        var startMs: Long
        var endMs: Long

        RandomAccessFile(file, "rw").use { raf ->
            val prevDataSize = (raf.length() - HEADER_SIZE).coerceAtLeast(0)
            val prevSampleCount = prevDataSize / BYTES_PER_SAMPLE
            startMs = (prevSampleCount * 1000L) / SAMPLE_RATE

            // Seek to end and write PCM
            raf.seek(raf.length())
            raf.write(sampleBytes)

            val newDataSize = (raf.length() - HEADER_SIZE).coerceAtLeast(0)
            newTotalSampleCount = newDataSize / BYTES_PER_SAMPLE
            endMs = (newTotalSampleCount * 1000L) / SAMPLE_RATE

            // Commit WAV header with updated sizes
            writeWavHeader(raf, newDataSize)
        }

        val newMarker = AudiobookPageMarker(
            pageIndex = pageIndex,
            startMs = startMs,
            endMs = endMs,
            sampleOffset = manifest.totalSampleCount,
            sampleCount = samples.size.toLong(),
        )

        val updatedMarkers = manifest.markers.filterNot { it.pageIndex == pageIndex } + newMarker
        val updatedManifest = manifest.copy(
            lastProcessedPage = pageIndex,
            totalDurationMs = endMs,
            totalSampleCount = newTotalSampleCount,
            markers = updatedMarkers.sortedBy { it.pageIndex },
        )
        saveManifest(updatedManifest)

        return updatedManifest
    }

    /**
     * Commits WAV header for a given data size.
     */
    private fun writeWavHeader(raf: RandomAccessFile, dataSize: Long) {
        val totalFileSize = dataSize + 36
        val byteRate = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE
        val blockAlign = CHANNELS * BYTES_PER_SAMPLE

        val header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalFileSize.toInt())
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size (16 for PCM)
        header.putShort(1) // AudioFormat (1 for PCM)
        header.putShort(CHANNELS.toShort())
        header.putInt(SAMPLE_RATE)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(BITS_PER_SAMPLE.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize.toInt())

        raf.seek(0)
        raf.write(header.array())
    }

    @Synchronized
    fun updateListeningProgress(id: String, positionMs: Long, pageIndex: Int) {
        val book = getAudiobook(id) ?: return
        saveAudiobook(
            book.copy(
                currentPositionMs = positionMs,
                currentPageIndex = pageIndex,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun persistAll(list: List<Audiobook>) {
        val array = JSONArray()
        list.forEach { array.put(serializeAudiobook(it)) }
        indexFile.writeText(array.toString(2))
    }

    private fun serializeAudiobook(a: Audiobook): JSONObject {
        return JSONObject().apply {
            put("id", a.id)
            put("bookId", a.bookId)
            put("title", a.title)
            put("author", a.author)
            put("coverPath", a.coverPath)
            put("sourceUri", a.sourceUri)
            put("filePath", a.filePath)
            put("mainTextOnly", a.mainTextOnly)
            put("voiceId", a.voiceId)
            put("voiceName", a.voiceName)
            put("engineType", a.engineType)
            put("language", a.language)
            put("startPage", a.startPage)
            put("endPage", a.endPage)
            put("totalPages", a.totalPages)
            put("processedPages", a.processedPages)
            put("checkpointProgress", a.checkpointProgress.toDouble())
            put("checkpointDurationMs", a.checkpointDurationMs)
            put("totalEstimatedDurationMs", a.totalEstimatedDurationMs)
            put("currentPositionMs", a.currentPositionMs)
            put("currentPageIndex", a.currentPageIndex)
            put("status", a.status.name)
            put("errorMessage", a.errorMessage)
            put("isPlayable", a.isPlayable)
            put("audioFileName", a.audioFileName)
            put("createdAt", a.createdAt)
            put("updatedAt", a.updatedAt)
        }
    }

    private fun deserializeAudiobook(o: JSONObject): Audiobook {
        return Audiobook(
            id = o.getString("id"),
            bookId = o.getString("bookId"),
            title = o.getString("title"),
            author = if (o.isNull("author")) null else o.getString("author"),
            coverPath = if (o.isNull("coverPath")) null else o.getString("coverPath"),
            sourceUri = if (o.isNull("sourceUri")) null else o.getString("sourceUri"),
            filePath = if (o.isNull("filePath")) null else o.getString("filePath"),
            mainTextOnly = o.optBoolean("mainTextOnly", true),
            voiceId = o.getString("voiceId"),
            voiceName = o.getString("voiceName"),
            engineType = o.optString("engineType", "PIPER"),
            language = o.optString("language", "tr"),
            startPage = o.optInt("startPage", 1),
            endPage = o.optInt("endPage", 1),
            totalPages = o.optInt("totalPages", 1),
            processedPages = o.optInt("processedPages", 0),
            checkpointProgress = o.optDouble("checkpointProgress", 0.0).toFloat(),
            checkpointDurationMs = o.optLong("checkpointDurationMs", 0L),
            totalEstimatedDurationMs = o.optLong("totalEstimatedDurationMs", 0L),
            currentPositionMs = o.optLong("currentPositionMs", 0L),
            currentPageIndex = o.optInt("currentPageIndex", 1),
            status = runCatching { AudiobookStatus.valueOf(o.getString("status")) }.getOrDefault(AudiobookStatus.QUEUED),
            errorMessage = if (o.isNull("errorMessage")) null else o.getString("errorMessage"),
            isPlayable = o.optBoolean("isPlayable", false),
            audioFileName = o.optString("audioFileName", "audiobook_${o.getString("id")}.wav"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
        )
    }
}
