package com.example.readproplus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.readproplus.model.tts.GeneratedAudio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DialogBackground = Color(0xFF1B1B2F)
private val CardBackground = Color(0xFF272744)
private val Accent = Color(0xFF4FC3F7)

@Composable
fun KokoroAudioDialog(
    totalPages: Int,
    currentPage: Int,
    mainTextOnly: Boolean,
    generatedAudios: List<GeneratedAudio>,
    onMainTextOnlyChange: (Boolean) -> Unit,
    onGenerate: (startPage: Int, endPage: Int, mainTextOnly: Boolean) -> Unit,
    onPlay: (GeneratedAudio) -> Unit,
    onDelete: (GeneratedAudio) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var startPageText by rememberSaveable(totalPages, currentPage) {
        mutableStateOf(currentPage.coerceIn(1, totalPages.coerceAtLeast(1)).toString())
    }
    var endPageText by rememberSaveable(totalPages, currentPage) {
        mutableStateOf(totalPages.coerceAtLeast(1).toString())
    }

    val startPage = startPageText.toIntOrNull()
    val endPage = endPageText.toIntOrNull()
    val validationMessage = when {
        totalPages <= 0 -> "This document has no readable pages."
        startPage == null || endPage == null -> "Enter both page numbers."
        startPage !in 1..totalPages -> "Start page must be between 1 and $totalPages."
        endPage !in 1..totalPages -> "End page must be between 1 and $totalPages."
        endPage < startPage -> "End page must be greater than or equal to the start page."
        else -> null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBackground),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Accent)
                    Text(
                        text = "Kokoro Audio",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DialogBackground,
                    contentColor = Accent,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Generate") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Generated audio (${generatedAudios.size})") },
                    )
                }

                Spacer(Modifier.height(16.dp))
                if (selectedTab == 0) {
                    Text(
                        text = "Choose the first and last page to convert into speech.",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = startPageText,
                            onValueChange = { startPageText = it.filter(Char::isDigit) },
                            label = { Text("Start page") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = endPageText,
                            onValueChange = { endPageText = it.filter(Char::isDigit) },
                            label = { Text("End page") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Available pages: 1-$totalPages",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                    validationMessage?.let { message ->
                        Spacer(Modifier.height(6.dp))
                        Text(text = message, color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Main text only", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Skip repeated headers, page numbers, captions, and links.",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                            )
                        }
                        Switch(checked = mainTextOnly, onCheckedChange = onMainTextOnlyChange)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Button(
                            enabled = validationMessage == null,
                            onClick = {
                                if (startPage != null && endPage != null) {
                                    onGenerate(startPage, endPage, mainTextOnly)
                                }
                            },
                        ) {
                            Text("Generate")
                        }
                    }
                } else {
                    if (generatedAudios.isEmpty()) {
                        Text(
                            text = "No generated audio for this book yet.",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(generatedAudios, key = GeneratedAudio::id) { audio ->
                                GeneratedAudioRow(
                                    audio = audio,
                                    onPlay = { onPlay(audio) },
                                    onDelete = { onDelete(audio) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = onDismiss) { Text("Close") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedAudioRow(
    audio: GeneratedAudio,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pages ${audio.startPage}-${audio.endPage}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatDuration(audio.durationMs)}  -  ${formatDate(audio.createdAt)}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play generated audio", tint = Accent)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete generated audio", tint = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(timestamp))
}
