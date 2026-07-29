package com.example.readproplus.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.readproplus.model.pdf.FormatType

class BatchImporter(private val context: Context) {

    fun scanFolderTree(treeUri: Uri): List<Uri> {
        persistReadPermission(treeUri)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val foundUris = mutableListOf<Uri>()
        traverseDocumentFile(rootDoc, foundUris)
        return foundUris
    }

    fun persistReadPermission(uri: Uri) {
        if (uri.scheme != "content") return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun traverseDocumentFile(dir: DocumentFile, result: MutableList<Uri>) {
        if (!dir.isDirectory) return
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                traverseDocumentFile(file, result)
            } else if (file.isFile) {
                val name = file.name ?: ""
                val ext = name.substringAfterLast('.', "").lowercase()
                if (FormatType.ALL_EXTENSIONS.contains(ext)) {
                    result.add(file.uri)
                }
            }
        }
    }
}
