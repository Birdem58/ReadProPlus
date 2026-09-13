package com.example.readproplus.data

import android.content.Context
import com.example.readproplus.model.tts.GeneratedAudio
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

class GeneratedAudioRepository(context: Context) {

    private val directory = File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }
    private val indexFile = File(directory, INDEX_FILE_NAME)

    @Synchronized
    fun listByBook(bookId: String): List<GeneratedAudio> = loadAll()
        .filter { it.bookId == bookId && audioFile(it).exists() }
        .sortedByDescending(GeneratedAudio::createdAt)

    @Synchronized
    fun save(
        bookId: String,
        bookTitle: String,
        startPage: Int,
        endPage: Int,
        durationMs: Long,
        samples: ShortArray,
    ): GeneratedAudio {
        require(samples.isNotEmpty()) { "Cannot save an empty audio recording." }

        return saveChunks(
            bookId = bookId,
            bookTitle = bookTitle,
            startPage = startPage,
            endPage = endPage,
            durationMs = durationMs,
            chunks = listOf(samples),
            sampleCount = samples.size.toLong(),
        )
    }

    @Synchronized
    fun saveChunks(
        bookId: String,
        bookTitle: String,
        startPage: Int,
        endPage: Int,
        durationMs: Long,
        chunks: List<ShortArray>,
        sampleCount: Long,
    ): GeneratedAudio {
        require(sampleCount > 0L && chunks.any { it.isNotEmpty() }) {
            "Cannot save an empty audio recording."
        }
        require(sampleCount <= Int.MAX_VALUE / BYTES_PER_SAMPLE) {
            "Generated audio is too large for a WAV file."
        }

        val id = UUID.randomUUID().toString()
        val fileName = "$id.wav"
        val item = GeneratedAudio(
            id = id,
            bookId = bookId,
            bookTitle = bookTitle,
            startPage = startPage,
            endPage = endPage,
            durationMs = durationMs,
            createdAt = System.currentTimeMillis(),
            fileName = fileName,
        )

        writeWav(audioFile(item), chunks, sampleCount)
        persist((loadAll() + item).filter { audioFile(it).exists() })
        return item
    }

    @Synchronized
    fun readSamples(item: GeneratedAudio): ShortArray {
        val file = audioFile(item)
        if (!file.exists()) throw IOException("Generated audio file is missing.")

        val bytes = file.readBytes()
        if (isWav(bytes)) {
            return readWavSamples(bytes)
        }

        // Older builds stored the generated 24 kHz mono samples directly in a
        // file named .wav. Keep those recordings playable and migrate them to
        // the canonical WAV container after they are read.
        return readRawPcmSamples(bytes).also { samples ->
            runCatching { writeWav(file, listOf(samples), samples.size.toLong()) }
        }
    }

    @Synchronized
    fun delete(item: GeneratedAudio) {
        audioFile(item).delete()
        persist(loadAll().filter { it.id != item.id && audioFile(it).exists() })
    }

    private fun audioFile(item: GeneratedAudio): File {
        // Metadata is app-generated, but keep the file lookup constrained to the
        // private directory even if an old/corrupt index contains a path separator.
        val safeName = File(item.fileName).name
        return File(directory, safeName)
    }

    private fun writeWav(file: File, chunks: List<ShortArray>, sampleCount: Long) {
        val dataSize = (sampleCount * BYTES_PER_SAMPLE).toInt()
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        try {
            DataOutputStream(BufferedOutputStream(temporaryFile.outputStream())).use { output ->
                output.writeBytes("RIFF")
                output.writeIntLE(WAV_HEADER_SIZE - 8 + dataSize)
                output.writeBytes("WAVE")
                output.writeBytes("fmt ")
                output.writeIntLE(16)
                output.writeShortLE(1)
                output.writeShortLE(CHANNEL_COUNT)
                output.writeIntLE(SAMPLE_RATE)
                output.writeIntLE(SAMPLE_RATE * CHANNEL_COUNT * BYTES_PER_SAMPLE)
                output.writeShortLE(CHANNEL_COUNT * BYTES_PER_SAMPLE)
                output.writeShortLE(BITS_PER_SAMPLE)
                output.writeBytes("data")
                output.writeIntLE(dataSize)
                chunks.forEach { chunk ->
                    chunk.forEach { sample -> output.writeShortLE(sample.toInt()) }
                }
            }
            check(temporaryFile.renameTo(file)) { "Could not finalize generated WAV file." }
        } finally {
            temporaryFile.delete()
        }
    }

    private fun isWav(bytes: ByteArray): Boolean =
        bytes.size >= 12 &&
            ascii(bytes, 0, 4) == "RIFF" &&
            ascii(bytes, 8, 4) == "WAVE"

    private fun readWavSamples(bytes: ByteArray): ShortArray {
        var offset = 12
        var audioFormat: Int? = null
        var channelCount: Int? = null
        var bitsPerSample: Int? = null
        var dataOffset = -1
        var dataSize = 0

        while (offset + WAV_CHUNK_HEADER_SIZE <= bytes.size) {
            val chunkId = ascii(bytes, offset, 4)
            val chunkSize = littleEndianUnsignedInt(bytes, offset + 4)
            val chunkDataOffset = offset + WAV_CHUNK_HEADER_SIZE
            val chunkEnd = chunkDataOffset.toLong() + chunkSize
            require(chunkEnd <= bytes.size) { "Generated WAV is truncated." }

            when (chunkId) {
                "fmt " -> {
                    require(chunkSize >= 16L) { "Generated WAV format chunk is invalid." }
                    audioFormat = littleEndianUnsignedShort(bytes, chunkDataOffset)
                    channelCount = littleEndianUnsignedShort(bytes, chunkDataOffset + 2)
                    bitsPerSample = littleEndianUnsignedShort(bytes, chunkDataOffset + 14)
                }

                "data" -> {
                    require(chunkSize > 0L && chunkSize % 2L == 0L) {
                        "Generated WAV has no PCM samples."
                    }
                    dataOffset = chunkDataOffset
                    dataSize = chunkSize.toInt()
                }
            }

            val nextOffset = chunkEnd.toInt() + (chunkSize.toInt() and 1)
            require(nextOffset <= bytes.size) { "Generated WAV is truncated." }
            offset = nextOffset
        }

        require(audioFormat == PCM_FORMAT) { "Generated WAV is not PCM audio." }
        require(channelCount == CHANNEL_COUNT && bitsPerSample == BITS_PER_SAMPLE) {
            "Generated WAV format is not supported."
        }
        require(dataOffset >= 0 && dataSize > 0) { "Generated WAV has no PCM samples." }

        return ShortArray(dataSize / BYTES_PER_SAMPLE) { index ->
            littleEndianShort(bytes, dataOffset + index * BYTES_PER_SAMPLE)
        }
    }

    private fun readRawPcmSamples(bytes: ByteArray): ShortArray {
        require(bytes.isNotEmpty() && bytes.size % BYTES_PER_SAMPLE == 0) {
            "Invalid WAV file."
        }
        return ShortArray(bytes.size / BYTES_PER_SAMPLE) { index ->
            littleEndianShort(bytes, index * BYTES_PER_SAMPLE)
        }
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes, offset, length, Charsets.US_ASCII)

    private fun littleEndianUnsignedInt(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)

    private fun littleEndianUnsignedShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun littleEndianShort(bytes: ByteArray, offset: Int): Short =
        littleEndianUnsignedShort(bytes, offset).toShort()

    private fun loadAll(): List<GeneratedAudio> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(indexFile.readText(Charsets.UTF_8))
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                GeneratedAudio(
                    id = item.getString("id"),
                    bookId = item.getString("bookId"),
                    bookTitle = item.optString("bookTitle", "Untitled"),
                    startPage = item.optInt("startPage", 1),
                    endPage = item.optInt("endPage", 1),
                    durationMs = item.optLong("durationMs", 0L),
                    createdAt = item.optLong("createdAt", 0L),
                    fileName = item.getString("fileName"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(items: List<GeneratedAudio>) {
        JSONArray().apply {
            items.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("bookId", item.bookId)
                    put("bookTitle", item.bookTitle)
                    put("startPage", item.startPage)
                    put("endPage", item.endPage)
                    put("durationMs", item.durationMs)
                    put("createdAt", item.createdAt)
                    put("fileName", item.fileName)
                })
            }
        }.also { indexFile.writeText(it.toString(), Charsets.UTF_8) }
    }

    private fun DataOutputStream.writeIntLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte((value ushr 8) and 0xFF)
        writeByte((value ushr 16) and 0xFF)
        writeByte((value ushr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte((value ushr 8) and 0xFF)
    }

    private companion object {
        const val DIRECTORY_NAME = "generated_audio"
        const val INDEX_FILE_NAME = "index.json"
        const val SAMPLE_RATE = 24_000
        const val CHANNEL_COUNT = 1
        const val BITS_PER_SAMPLE = 16
        const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
        const val WAV_HEADER_SIZE = 44
        const val WAV_CHUNK_HEADER_SIZE = 8
        const val PCM_FORMAT = 1
    }
}
