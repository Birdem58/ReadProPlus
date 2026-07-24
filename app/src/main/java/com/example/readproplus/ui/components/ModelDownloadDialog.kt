package com.example.readproplus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF4FC3F7)

@Composable
fun ModelDownloadDialog(
    progress: Float,
    bytesDownloaded: Long,
    totalBytes: Long,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = "Downloading TTS Model",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = modifier.fillMaxWidth()) {
                Text(
                    text = "Downloading the Kokoro TTS model for offline text-to-speech.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Accent,
                    trackColor = Accent.copy(alpha = 0.2f),
                )
                Spacer(Modifier.height(8.dp))

                val downloadedMB = bytesDownloaded / (1024f * 1024f)
                val totalMB = totalBytes / (1024f * 1024f)
                Text(
                    text = "%.1f MB / %.1f MB (%.0f%%)".format(
                        downloadedMB, totalMB, progress * 100
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Required once (~300 MB). Works offline after download.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
    )
}
