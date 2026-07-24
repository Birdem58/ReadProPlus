package com.example.readproplus.tts

import android.content.Context
import android.util.Log
import com.example.readproplus.model.tts.TtsVoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Loads bundled voice data and downloads optional Kokoro voice packs on demand. */
class VoiceManager(private val context: Context) {

    fun loadVoice(voice: TtsVoice): FloatArray {
        val bytes = when (val bundledAsset = voice.bundledAssetPath) {
            null -> voiceFile(voice).takeIf(File::exists)?.readBytes()
                ?: throw IOException("${voice.displayName} has not been downloaded yet.")

            else -> context.assets.open(bundledAsset).use { it.readBytes() }
        }

        require(bytes.size % Float.SIZE_BYTES == 0) {
            "Voice '${voice.displayName}' is not a float32 data file."
        }

        return FloatArray(bytes.size / Float.SIZE_BYTES).also { output ->
            ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
                .get(output)
        }
    }

    fun isVoiceAvailable(voice: TtsVoice): Boolean = runCatching {
        validateVoice(loadVoice(voice))
    }.getOrDefault(false)

    fun downloadVoice(voice: TtsVoice): Flow<VoiceDownloadProgress> = flow {
        if (isVoiceAvailable(voice)) {
            emit(VoiceDownloadProgress.Complete(voice.id))
            return@flow
        }

        val bundledAsset = voice.bundledAssetPath
        if (bundledAsset != null) {
            emit(VoiceDownloadProgress.Error(voice.id, "The bundled ${voice.displayName} voice is unavailable."))
            return@flow
        }

        val destination = voiceFile(voice)
        val temporary = temporaryVoiceFile(voice)
        temporary.delete()

        var connection: HttpURLConnection? = null
        try {
            connection = (URL("$VOICE_BASE_URL${voice.remoteFileName}").openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            val responseCode = connection.responseCode
            if (responseCode !in HttpURLConnection.HTTP_OK..HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("Voice download failed with HTTP $responseCode")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: UNKNOWN_FILE_SIZE
            var downloadedBytes = 0L
            emit(VoiceDownloadProgress.Downloading(voice.id, 0f, downloadedBytes, totalBytes))

            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0L) {
                            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        emit(VoiceDownloadProgress.Downloading(voice.id, progress, downloadedBytes, totalBytes))
                    }
                }
            }

            if (!isVoiceFileValid(temporary)) {
                throw IOException("Downloaded ${voice.displayName} voice data is invalid.")
            }

            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            emit(VoiceDownloadProgress.Complete(voice.id))
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Log.e(TAG, "Failed to download voice ${voice.id}", error)
            temporary.delete()
            emit(VoiceDownloadProgress.Error(voice.id, error.message ?: "Voice download failed."))
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    fun validateVoice(embedding: FloatArray): Boolean {
        if (embedding.size < STYLE_DIMENSION * 2 || embedding.size % STYLE_DIMENSION != 0) return false
        return embedding.all { it.isFinite() && kotlin.math.abs(it) <= MAX_ABSOLUTE_VALUE }
    }

    private fun isVoiceFileValid(file: File): Boolean = runCatching {
        file.isFile && file.length() >= MIN_VOICE_FILE_SIZE && validateVoice(readVoiceFile(file))
    }.getOrDefault(false)

    private fun readVoiceFile(file: File): FloatArray {
        val bytes = file.readBytes()
        require(bytes.size % Float.SIZE_BYTES == 0) { "Voice data has an invalid byte count." }
        return FloatArray(bytes.size / Float.SIZE_BYTES).also { output ->
            ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
                .get(output)
        }
    }

    private fun voiceFile(voice: TtsVoice): File = File(voiceDirectory, voice.remoteFileName)

    private fun temporaryVoiceFile(voice: TtsVoice): File = File(voiceDirectory, "${voice.remoteFileName}.tmp")

    private val voiceDirectory: File
        get() = File(context.filesDir, "kokoro_tts/voices").also { it.mkdirs() }

    private companion object {
        const val TAG = "VoiceManager"
        const val VOICE_BASE_URL = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/voices/"
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 60_000
        const val BUFFER_SIZE = 8_192
        const val UNKNOWN_FILE_SIZE = 522_240L
        const val STYLE_DIMENSION = 256
        const val MIN_VOICE_FILE_SIZE = STYLE_DIMENSION * Float.SIZE_BYTES * 2L
        const val MAX_ABSOLUTE_VALUE = 100f
    }
}

sealed interface VoiceDownloadProgress {
    data class Downloading(
        val voiceId: String,
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : VoiceDownloadProgress

    data class Complete(val voiceId: String) : VoiceDownloadProgress
    data class Error(val voiceId: String, val message: String) : VoiceDownloadProgress
}
