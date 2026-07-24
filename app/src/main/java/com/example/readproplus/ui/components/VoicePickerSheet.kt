package com.example.readproplus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.tts.VoiceDownloadProgress

private val VoiceAccent = Color(0xFF4FC3F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicePickerSheet(
    voices: List<TtsVoice>,
    selectedVoice: TtsVoice,
    voiceAvailability: Map<String, Boolean>,
    downloadProgress: VoiceDownloadProgress?,
    onVoiceSelected: (TtsVoice) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Kokoro voices",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                text = "Nicole is included. Tap any other voice to download it once (about 0.5 MB).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            LazyColumn(modifier = Modifier.heightIn(max = 560.dp)) {
                items(voices, key = TtsVoice::id) { voice ->
                    VoicePickerRow(
                        voice = voice,
                        isSelected = voice.id == selectedVoice.id,
                        isAvailable = voiceAvailability[voice.id] == true || voice.bundledAssetPath != null,
                        downloadProgress = downloadProgress,
                        onClick = { onVoiceSelected(voice) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VoicePickerRow(
    voice: TtsVoice,
    isSelected: Boolean,
    isAvailable: Boolean,
    downloadProgress: VoiceDownloadProgress?,
    onClick: () -> Unit,
) {
    val downloading = downloadProgress as? VoiceDownloadProgress.Downloading
    val error = downloadProgress as? VoiceDownloadProgress.Error
    val isDownloadingThisVoice = downloading?.voiceId == voice.id
    val errorForThisVoice = error?.takeIf { it.voiceId == voice.id }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDownloadingThisVoice, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = voice.displayName,
                color = if (isSelected) VoiceAccent else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp,
            )
            Text(
                text = voice.details,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )

            if (isDownloadingThisVoice) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { downloading.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = VoiceAccent,
                )
            } else if (errorForThisVoice != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Download failed — tap to retry",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                )
            }
        }

        when {
            isDownloadingThisVoice -> CircularProgressIndicator(
                modifier = Modifier.width(22.dp).height(22.dp),
                color = VoiceAccent,
                strokeWidth = 2.dp,
            )

            isSelected -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = VoiceAccent,
            )

            isAvailable -> Text(
                text = "Use",
                color = VoiceAccent,
                fontWeight = FontWeight.SemiBold,
            )

            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download ${voice.displayName}",
                    tint = VoiceAccent,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Download",
                    color = VoiceAccent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
