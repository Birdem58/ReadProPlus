package com.example.readproplus.pdf

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
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
}

class PdfFileTooLargeException(val maxBytes: Long) :
    IllegalStateException("File exceeds maximum size of $maxBytes bytes")
