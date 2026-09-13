package com.example.readproplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.audiobook.Audiobook
import com.example.readproplus.model.audiobook.AudiobookStatus
import com.example.readproplus.ui.components.AudiobookCover
import com.example.readproplus.ui.theme.ReadProPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobooksShelfScreen(
    audiobooks: List<Audiobook>,
    onMenuClick: () -> Unit,
    onAddAudiobookClick: () -> Unit,
    onAudiobookClick: (Audiobook) -> Unit,
    onPlayClick: (Audiobook) -> Unit,
    onResumeProcessing: (Audiobook) -> Unit,
    onPauseProcessing: (Audiobook) -> Unit,
    onDeleteAudiobook: (Audiobook) -> Unit,
) {
    var audiobookPendingDelete by remember { mutableStateOf<Audiobook?>(null) }
    val continueListening = audiobooks.firstOrNull { it.isPlayable && it.currentPositionMs > 0L }
        ?: audiobooks.firstOrNull { it.isPlayable }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Audiobooks",
                            color = ReadProPalette.ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                        Text(
                            text = "Dinle, kaldığın yerden devam et",
                            color = ReadProPalette.muted,
                            fontSize = 12.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menüyü aç",
                            tint = ReadProPalette.ink,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddAudiobookClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Sesli kitap oluştur",
                            tint = ReadProPalette.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ReadProPalette.background),
            )
        },
        containerColor = ReadProPalette.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Sesli kitaplığın",
                        color = ReadProPalette.ink,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (audiobooks.isEmpty()) {
                            "İlk kitabını birkaç dokunuşla dinlemeye hazırla."
                        } else {
                            "Hazır olanı dinle, diğerlerini arka planda hazırla."
                        },
                        color = ReadProPalette.muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (continueListening != null) {
                item {
                    ContinueListeningCard(
                        audiobook = continueListening,
                        onPlay = { onPlayClick(continueListening) },
                    )
                }
            }

            if (audiobooks.isEmpty()) {
                item {
                    EmptyAudiobooksState(onAddAudiobookClick = onAddAudiobookClick)
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Tüm kitaplar",
                            color = ReadProPalette.ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = ReadProPalette.surfaceMuted,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = audiobooks.size.toString(),
                                color = ReadProPalette.muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                items(audiobooks, key = { it.id }) { audiobook ->
                    AudiobookCard(
                        audiobook = audiobook,
                        onClick = { onAudiobookClick(audiobook) },
                        onPlay = { onPlayClick(audiobook) },
                        onResume = { onResumeProcessing(audiobook) },
                        onPause = { onPauseProcessing(audiobook) },
                        onDelete = { audiobookPendingDelete = audiobook },
                    )
                }
            }
        }
    }

    audiobookPendingDelete?.let { audiobook ->
        AlertDialog(
            onDismissRequest = { audiobookPendingDelete = null },
            containerColor = ReadProPalette.surface,
            title = {
                Text(
                    text = "Sesli kitabı kaldır?",
                    color = ReadProPalette.ink,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "${audiobook.title} ve oluşturulmuş ses dosyası kaldırılacak.",
                    color = ReadProPalette.muted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        audiobookPendingDelete = null
                        onDeleteAudiobook(audiobook)
                    },
                ) {
                    Text("Kaldır", color = ReadProPalette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { audiobookPendingDelete = null }) {
                    Text("Vazgeç", color = ReadProPalette.primary)
                }
            },
        )
    }
}

@Composable
private fun ContinueListeningCard(
    audiobook: Audiobook,
    onPlay: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ReadProPalette.primaryDeep),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AudiobookCover(
                title = audiobook.title,
                author = audiobook.author,
                compact = true,
                modifier = Modifier.size(width = 70.dp, height = 88.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KALDIĞIN YERDEN",
                    color = ReadProPalette.warmStrong.copy(alpha = 0.95f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = audiobook.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Sayfa ${audiobook.currentPageIndex + 1} / ${audiobook.totalPages}",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                )
            }
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(48.dp)
                    .background(ReadProPalette.warmStrong, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Dinlemeye devam et",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyAudiobooksState(onAddAudiobookClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ReadProPalette.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = ReadProPalette.primarySoft,
                shape = CircleShape,
                modifier = Modifier.size(68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = ReadProPalette.primaryDeep,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                text = "Henüz sesli kitap yok",
                color = ReadProPalette.ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Bir PDF seç, seslendirmeni belirle ve ilk %5 hazır olduğunda dinlemeye başla.",
                color = ReadProPalette.muted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAddAudiobookClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReadProPalette.primary,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Sesli kitap oluştur", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AudiobookCard(
    audiobook: Audiobook,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val progress = audiobookProgress(audiobook)
    val isClickable = audiobook.isPlayable

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ReadProPalette.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AudiobookCover(
                    title = audiobook.title,
                    author = audiobook.author,
                    compact = true,
                    modifier = Modifier.size(width = 64.dp, height = 84.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audiobook.title,
                        color = ReadProPalette.ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = audiobook.author ?: "${audiobook.voiceName} • ${languageLabel(audiobook.language)}",
                        color = ReadProPalette.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    AudioStatusPill(audiobook)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Sesli kitap seçenekleri",
                                tint = ReadProPalette.muted,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            when (audiobook.status) {
                                AudiobookStatus.PROCESSING -> DropdownMenuItem(
                                    text = { Text("Duraklat") },
                                    leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onPause()
                                    },
                                )
                                AudiobookStatus.PAUSED, AudiobookStatus.ERROR -> DropdownMenuItem(
                                    text = { Text("Devam et") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onResume()
                                    },
                                )
                                else -> Unit
                            }
                            DropdownMenuItem(
                                text = { Text("Kaldır") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                            )
                        }
                    }

                    if (audiobook.isPlayable) {
                        IconButton(
                            onClick = onPlay,
                            modifier = Modifier
                                .size(42.dp)
                                .background(ReadProPalette.primary, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Dinle",
                                tint = Color.White,
                                modifier = Modifier.size(23.dp),
                            )
                        }
                    } else if (audiobook.status == AudiobookStatus.PROCESSING) {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier
                                .size(42.dp)
                                .background(ReadProPalette.surfaceMuted, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Seslendirmeyi duraklat",
                                tint = ReadProPalette.primaryDeep,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                }
            }

            if (audiobook.status == AudiobookStatus.PROCESSING || audiobook.status == AudiobookStatus.PAUSED) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (audiobook.isPlayable) ReadProPalette.primary else ReadProPalette.warmStrong,
                    trackColor = ReadProPalette.surfaceMuted,
                )
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (audiobook.isPlayable) "İlk bölüm hazır • arka planda devam ediyor" else "Ses hazırlanıyor",
                        color = ReadProPalette.muted,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = "%${(audiobook.checkpointProgress * 100).toInt().coerceIn(0, 100)}",
                        color = ReadProPalette.primaryDeep,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioStatusPill(audiobook: Audiobook) {
    val (label, color, background) = when (audiobook.status) {
        AudiobookStatus.COMPLETED -> Triple("Tamamlandı", ReadProPalette.success, ReadProPalette.success.copy(alpha = 0.12f))
        AudiobookStatus.PROCESSING -> if (audiobook.isPlayable) {
            Triple("Dinlemeye hazır", ReadProPalette.primaryDeep, ReadProPalette.primarySoft)
        } else {
            Triple("Seslendiriliyor", ReadProPalette.warning, ReadProPalette.warm)
        }
        AudiobookStatus.PAUSED -> Triple("Duraklatıldı", ReadProPalette.muted, ReadProPalette.surfaceMuted)
        AudiobookStatus.ERROR -> Triple("Tekrar denenecek", ReadProPalette.danger, ReadProPalette.danger.copy(alpha = 0.12f))
        AudiobookStatus.QUEUED -> Triple("Sırada", ReadProPalette.muted, ReadProPalette.surfaceMuted)
    }

    Surface(color = background, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

private fun audiobookProgress(audiobook: Audiobook): Float {
    val total = (audiobook.endPage - audiobook.startPage + 1).coerceAtLeast(1)
    return (audiobook.processedPages.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

private fun languageLabel(language: String): String = if (language.equals("tr", ignoreCase = true)) "Türkçe" else "English"

