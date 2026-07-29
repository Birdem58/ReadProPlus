package com.example.readproplus.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.readproplus.model.pdf.FormatType
import java.io.File

class StorageScanner(private val context: Context) {

    fun scanForBooks(): List<Uri> {
        val foundUris = mutableListOf<Uri>()
        val mediaStoreUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
        )

        // DISPLAY_NAME is portable across modern MediaStore providers. DATA is
        // unavailable or restricted on newer Android releases.
        runCatching {
            context.contentResolver.query(
                mediaStoreUri,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = if (nameColumn >= 0) cursor.getString(nameColumn).orEmpty() else ""
                    if (FormatType.ALL_EXTENSIONS.contains(name.substringAfterLast('.', "").lowercase())) {
                        foundUris += Uri.withAppendedPath(mediaStoreUri, cursor.getLong(idColumn).toString())
                    }
                }
            }
        }

        // Direct filesystem access is a legacy fallback only. On Android 11+
        // it is normally blocked by scoped storage, so SAF/MediaStore remains
        // the supported path.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val directoriesToScan = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                File(Environment.getExternalStorageDirectory(), "Books"),
                File(Environment.getExternalStorageDirectory(), "Documents"),
            )
            directoriesToScan.forEach { dir ->
                if (dir.exists() && dir.isDirectory) scanDirRecursively(dir, foundUris)
            }
        }

        return foundUris.distinctBy { it.toString() }
    }

    private fun scanDirRecursively(dir: File, result: MutableList<Uri>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirRecursively(file, result)
            } else if (file.isFile && FormatType.ALL_EXTENSIONS.contains(file.extension.lowercase())) {
                result += Uri.fromFile(file)
            }
        }
    }
}
