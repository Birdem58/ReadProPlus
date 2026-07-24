package com.example.readproplus.tts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ModelManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelManager"
        private const val MODEL_FILENAME = "kokoro-v0_19.onnx"
        private const val MODEL_URL = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/onnx/model.onnx"
        private const val EXPECTED_SHA256 = "8fbea51ea711f2af382e88c833d9e288c6dc82ce5e98421ea61c058ce21a34cb"
        private const val DOWNLOAD_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val BUFFER_SIZE = 8192
    }

    private val modelDir: File
        get() = File(context.filesDir, "kokoro_tts").also { it.mkdirs() }

    private val modelFile: File
        get() = File(modelDir, MODEL_FILENAME)

    private val tempFile: File
        get() = File(modelDir, "${MODEL_FILENAME}.tmp")

    suspend fun isModelReady(): Boolean = withContext(Dispatchers.IO) {
        if (modelFile.exists() && modelFile.length() > 10_000_000L && verifyChecksum(modelFile)) {
            return@withContext true
        }
        try {
            val assetList = context.assets.list("") ?: emptyArray()
            if (MODEL_FILENAME in assetList) {
                copyAssetToInternalStorage()
                return@withContext modelFile.exists() && modelFile.length() > 10_000_000L && verifyChecksum(modelFile)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking bundled asset model", e)
        }
        false
    }

    fun getModelPath(): String? {
        return if (modelFile.exists()) modelFile.absolutePath else null
    }

    fun downloadModel(): Flow<DownloadProgress> = flow {
        if (isModelReady()) {
            emit(DownloadProgress.Complete(modelFile.absolutePath))
            return@flow
        }

        try {
            val assetList = context.assets.list("") ?: emptyArray()
            if (MODEL_FILENAME in assetList) {
                emit(DownloadProgress.Downloading(0.5f, 0L, 300_000_000L))
                copyAssetToInternalStorage()
                if (modelFile.exists()) {
                    emit(DownloadProgress.Complete(modelFile.absolutePath))
                    return@flow
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bundled asset extraction failed, falling back to HTTP download", e)
        }

        if (modelFile.exists() && EXPECTED_SHA256 != "PLACEHOLDER_SHA256_HASH" && !verifyChecksum(modelFile)) {
            modelFile.delete()
        }

        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
        val totalBytes = getFileSize()

        emit(DownloadProgress.Downloading(0f, existingBytes, totalBytes))

        val url = URL(MODEL_URL)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = DOWNLOAD_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            if (existingBytes > 0) {
                setRequestProperty("Range", "bytes=$existingBytes-")
            }
        }

        try {
            val responseCode = connection.responseCode
            val supportsResume = responseCode == 206
            val actualTotal = if (supportsResume) {
                existingBytes + (connection.getHeaderField("Content-Range")
                    ?.substringAfter("/")?.toLongOrNull() ?: totalBytes)
            } else {
                connection.contentLength.toLong()
            }

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile, existingBytes > 0 && supportsResume)
            val buffer = ByteArray(BUFFER_SIZE)
            var downloadedBytes = if (supportsResume) existingBytes else 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                val progress = if (actualTotal > 0) downloadedBytes.toFloat() / actualTotal else -1f
                emit(DownloadProgress.Downloading(progress, downloadedBytes, actualTotal))
            }

            outputStream.close()
            inputStream.close()

            if (EXPECTED_SHA256 != "PLACEHOLDER_SHA256_HASH" && !verifyChecksum(tempFile)) {
                tempFile.delete()
                throw IOException("Downloaded model failed SHA256 verification")
            }

            tempFile.renameTo(modelFile)
            emit(DownloadProgress.Complete(modelFile.absolutePath))

        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            emit(DownloadProgress.Error(e.message ?: "Download failed"))
        } finally {
            connection.disconnect()
        }
    }

    suspend fun cancelDownload() {
        withContext(Dispatchers.IO) {
            tempFile.delete()
        }
    }

    suspend fun deleteModel() {
        withContext(Dispatchers.IO) {
            modelFile.delete()
            tempFile.delete()
        }
    }

    fun getModelSizeBytes(): Long {
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    private suspend fun getFileSize(): Long = withContext(Dispatchers.IO) {
        try {
            val url = URL(MODEL_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = DOWNLOAD_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            val size = conn.contentLength.toLong()
            conn.disconnect()
            size
        } catch (e: Exception) {
            300_000_000L
        }
    }

    private suspend fun verifyChecksum(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            hash == EXPECTED_SHA256
        } catch (e: Exception) {
            Log.e(TAG, "Checksum verification failed", e)
            false
        }
    }

    private fun copyAssetToInternalStorage() {
        try {
            context.assets.open(MODEL_FILENAME).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 10_000_000L) {
                tempFile.renameTo(modelFile)
                Log.i(TAG, "Successfully extracted bundled model asset to ${modelFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract bundled model asset", e)
            tempFile.delete()
        }
    }
}

sealed interface DownloadProgress {
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadProgress
    data class Complete(val modelPath: String) : DownloadProgress
    data class Error(val message: String) : DownloadProgress
}
