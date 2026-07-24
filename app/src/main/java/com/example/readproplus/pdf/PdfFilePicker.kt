package com.example.readproplus.pdf

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberPdfPickerLauncher(
    onPdfSelected: (android.net.Uri) -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
) { uri ->
    uri?.let { onPdfSelected(it) }
}

fun getPdfDisplayName(
    context: android.content.Context,
    uri: android.net.Uri,
): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
    }
    return name
}
