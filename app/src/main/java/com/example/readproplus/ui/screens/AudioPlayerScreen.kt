@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.readproplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.audiobook.Audiobook
import com.example.readproplus.model.audiobook.AudiobookStatus
import com.example.readproplus.tts.AudiobookPlaybackState
import com.example.readproplus.ui.components.AudiobookCover
import com.example.readproplus.ui.theme.ReadProPalette

@Composable
fun AudioPlayerScreen(
    playbackState: AudiobookPlaybackState,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward15: () -> Unit,
    onSkipBackward15: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSleepTimerChange: (Int?) -> Unit,
    onSleepTimerEndOfPage: () -> Unit,
    onSeekToPage: (Int) -> Unit,
) {
    val audiobook = playbackState.audiobook ?: return
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showPagesSheet by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val totalMs = playbackState.totalDurationMs.coerceAtLeast(1L)
    val currentMs = playbackState.currentPositionMs.coerceIn(0L, totalMs)
    val sliderValue = if (isDraggingSlider) dragProgress else currentMs.toFloat() / totalMs.toFloat()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ReadProPalette.primarySoft, ReadProPalette.background, ReadProPalette.background),
                ),
            ),
    ) {
        val compact = maxHeight < 720.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (compact) 20.dp else 28.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerTopBar(
                onCollapse = onCollapse,
                onPagesClick = { showPagesSheet = true },
            )

            Spacer(Modifier.height(if (compact) 12.dp else 24.dp))

            AudiobookCover(
                title = audiobook.title,
                author = audiobook.author,
                modifier = Modifier.size(if (compact) 188.dp else 244.dp),
            )

            Spacer(Modifier.height(if (compact) 16.dp else 22.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = audiobook.title,
                    color = ReadProPalette.ink,
                    fontSize = if (compact) 21.sp else 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = audiobook.author ?: "${audiobook.voiceName} • ${languageLabel(audiobook.language)}",
                    color = ReadProPalette.muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = ReadProPalette.surface,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = ReadProPalette.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Sayfa ${playbackState.currentPage} / ${audiobook.totalPages}",
                            color = ReadProPalette.primaryDeep,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (audiobook.status == AudiobookStatus.ERROR) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFDEDEC),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = audiobook.errorMessage ?: "Seslendirme sırasında bir hata oluştu.",
                        color = Color(0xFFC0392B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            } else if (audiobook.status == AudiobookStatus.PROCESSING) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = ReadProPalette.warm.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = "İlk bölüm hazır • seslendirme arka planda devam ediyor",
                        color = ReadProPalette.warning,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(if (compact) 16.dp else 24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ReadProPalette.surface.copy(alpha = 0.82f),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LinearProgressIndicator(
                            progress = { (playbackState.bufferedDurationMs.toFloat() / totalMs).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50)),
                            color = ReadProPalette.primarySoft,
                            trackColor = ReadProPalette.surfaceMuted,
                        )
                        Slider(
                            value = sliderValue.coerceIn(0f, 1f),
                            onValueChange = {
                                isDraggingSlider = true
                                dragProgress = it
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                onSeek((dragProgress * totalMs).toLong())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = ReadProPalette.warmStrong,
                                activeTrackColor = ReadProPalette.warmStrong,
                                inactiveTrackColor = Color.Transparent,
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatTime(if (isDraggingSlider) (dragProgress * totalMs).toLong() else currentMs),
                            color = ReadProPalette.muted,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "-${formatTime((totalMs - currentMs).coerceAtLeast(0L))}",
                            color = ReadProPalette.muted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkipControl(
                    icon = Icons.Default.Replay10,
                    label = "15 sn geri",
                    onClick = onSkipBackward15,
                )
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(if (compact) 68.dp else 76.dp)
                        .background(ReadProPalette.primary, CircleShape),
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Duraklat" else "Oynat",
                        tint = Color.White,
                        modifier = Modifier.size(if (compact) 34.dp else 39.dp),
                    )
                }
                SkipControl(
                    icon = Icons.Default.Forward10,
                    label = "15 sn ileri",
                    onClick = onSkipForward15,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlayerOption(
                    icon = Icons.Default.Speed,
                    label = "Hız",
                    value = "${playbackState.speed}x",
                    modifier = Modifier.weight(1f),
                    onClick = { showSpeedSheet = true },
                )
                PlayerOption(
                    icon = Icons.Default.Bedtime,
                    label = "Uyku",
                    value = playbackState.sleepTimerMinutesRemaining?.let { "${it} dk" } ?: "Kapalı",
                    modifier = Modifier.weight(1f),
                    highlighted = playbackState.sleepTimerMinutesRemaining != null,
                    onClick = { showSleepSheet = true },
                )
            }
        }
    }

    if (showSpeedSheet) {
        SpeedSheet(
            currentSpeed = playbackState.speed,
            onSpeedSelected = {
                onSpeedChange(it)
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false },
        )
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            hasTimer = playbackState.sleepTimerMinutesRemaining != null,
            onMinutesSelected = {
                onSleepTimerChange(it)
                showSleepSheet = false
            },
            onEndOfPageSelected = {
                onSleepTimerEndOfPage()
                showSleepSheet = false
            },
            onDismiss = { showSleepSheet = false },
        )
    }

    if (showPagesSheet) {
        PagesSheet(
            audiobook = audiobook,
            currentPage = playbackState.currentPage,
            onPageSelected = {
                onSeekToPage(it)
                showPagesSheet = false
            },
            onDismiss = { showPagesSheet = false },
        )
    }
}

@Composable
private fun PlayerTopBar(onCollapse: () -> Unit, onPagesClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Oynatıcıyı küçült",
                tint = ReadProPalette.ink,
                modifier = Modifier.size(30.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NOW PLAYING",
                color = ReadProPalette.primaryDeep,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = "Sesli kitap",
                color = ReadProPalette.muted,
                fontSize = 11.sp,
            )
        }
        IconButton(onClick = onPagesClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "Sayfalara git",
                tint = ReadProPalette.ink,
            )
        }
    }
}

@Composable
private fun SkipControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width = 78.dp, height = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = ReadProPalette.ink, modifier = Modifier.size(30.dp))
        Text(label, color = ReadProPalette.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayerOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = if (highlighted) ReadProPalette.warm else ReadProPalette.surface.copy(alpha = 0.84f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) ReadProPalette.warmStrong else ReadProPalette.primary,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(label, color = ReadProPalette.muted, fontSize = 10.sp)
            Text(
                value,
                color = if (highlighted) ReadProPalette.warning else ReadProPalette.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SpeedSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ReadProPalette.background) {
        SheetHeader(title = "Oynatma hızı", subtitle = "Dinleme ritmini seç")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            speeds.chunked(3).forEach { rowSpeeds ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowSpeeds.forEach { speed ->
                        val selected = speed == currentSpeed
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSpeedSelected(speed) },
                            color = if (selected) ReadProPalette.primary else ReadProPalette.surface,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = "${speed}x",
                                color = if (selected) Color.White else ReadProPalette.ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 14.dp),
                            )
                        }
                    }
                    repeat(3 - rowSpeeds.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SleepTimerSheet(
    hasTimer: Boolean,
    onMinutesSelected: (Int?) -> Unit,
    onEndOfPageSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    val timers = listOf(15, 30, 45, 60)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ReadProPalette.background) {
        SheetHeader(title = "Uyku zamanlayıcısı", subtitle = "Dinleme ne zaman dursun?")
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            timers.forEach { minutes ->
                TimerChoice(text = "$minutes dakika", onClick = { onMinutesSelected(minutes) })
            }
            TimerChoice(text = "Bu sayfa bitince", onClick = onEndOfPageSelected)
            if (hasTimer) {
                TimerChoice(
                    text = "Zamanlayıcıyı kapat",
                    danger = true,
                    onClick = { onMinutesSelected(null) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TimerChoice(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (danger) ReadProPalette.danger.copy(alpha = 0.10f) else ReadProPalette.surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = text,
            color = if (danger) ReadProPalette.danger else ReadProPalette.ink,
            fontSize = 14.sp,
            fontWeight = if (danger) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
        )
    }
}

@Composable
private fun PagesSheet(
    audiobook: Audiobook,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ReadProPalette.background) {
        SheetHeader(
            title = "Sayfalar",
            subtitle = "${audiobook.totalPages} sayfalık sesli kitap",
        )
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(audiobook.totalPages) { index ->
                val page = index + 1
                val selected = page == currentPage
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPageSelected(page) },
                    color = if (selected) ReadProPalette.primarySoft else ReadProPalette.surface,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Sayfa $page",
                            color = if (selected) ReadProPalette.primaryDeep else ReadProPalette.ink,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = "Şu an çalıyor", tint = ReadProPalette.primary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, color = ReadProPalette.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = ReadProPalette.muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
    }
}

private fun languageLabel(language: String): String = if (language.equals("tr", ignoreCase = true)) "Türkçe" else "English"

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
