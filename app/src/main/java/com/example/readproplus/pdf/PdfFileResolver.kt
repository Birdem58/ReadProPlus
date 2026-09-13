package com.example.readproplus.pdf

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.FileNotFoundException

class PdfFileResolver(private val context: Context) {

    companion object {
        private const val MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024
    }

    fun resolve(uri: Uri): ParcelFileDescriptor {
        if (uri.scheme == null || uri.scheme == "file") {
            val path = uri.path ?: uri.toString()
            val file = java.io.File(path)
            if (file.exists() && file.isFile) {
                if (file.length() > MAX_FILE_SIZE_BYTES) {
                    throw PdfFileTooLargeException(MAX_FILE_SIZE_BYTES)
                }
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw FileNotFoundException("Could not open file: $uri")
        if (fd.statSize > MAX_FILE_SIZE_BYTES) {
            fd.close()
            throw PdfFileTooLargeException(MAX_FILE_SIZE_BYTES)
        }
        return fd
    }

    fun getDisplayName(uri: Uri): String? {
        val providerName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

        return providerName?.trim()?.takeIf { it.isNotBlank() }
            ?: uri.path
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() && it.contains('.') }
                ?.let { Uri.decode(it) }
    }
}

class PdfFileTooLargeException(val maxBytes: Long) :
    IllegalStateException("File exceeds maximum size of $maxBytes bytes")
