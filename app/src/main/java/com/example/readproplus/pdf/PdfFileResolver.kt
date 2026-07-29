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
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw FileNotFoundException("Could not open file: $uri")
        if (fd.statSize > MAX_FILE_SIZE_BYTES) {
            fd.close()
            throw PdfFileTooLargeException(MAX_FILE_SIZE_BYTES)
        }
        return fd
    }

    fun getDisplayName(uri: Uri): String? = runCatching {
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
}

class PdfFileTooLargeException(val maxBytes: Long) :
    IllegalStateException("File exceeds maximum size of $maxBytes bytes")
