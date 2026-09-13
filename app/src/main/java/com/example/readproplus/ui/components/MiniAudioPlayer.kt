package com.example.readproplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.tts.AudiobookPlaybackState
import com.example.readproplus.ui.theme.ReadProPalette

@Composable
fun MiniAudioPlayer(
    playbackState: AudiobookPlaybackState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipForward15: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audiobook = playbackState.audiobook ?: return
    val totalMs = playbackState.totalDurationMs.coerceAtLeast(1L)
    val progress = (playbackState.currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(14.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = ReadProPalette.surface,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = ReadProPalette.warmStrong,
                trackColor = ReadProPalette.warm.copy(alpha = 0.85f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AudiobookCover(
                    title = audiobook.title,
                    compact = true,
                    modifier = Modifier.size(width = 42.dp, height = 50.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audiobook.title,
                        color = ReadProPalette.ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Sayfa ${playbackState.currentPageIndex + 1} / ${audiobook.totalPages}",
                        color = ReadProPalette.muted,
                        fontSize = 11.sp,
                    )
                }
                IconButton(onClick = onSkipForward15) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "15 saniye ileri sar",
                        tint = ReadProPalette.muted,
                        modifier = Modifier.size(23.dp),
                    )
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(40.dp)
                        .background(ReadProPalette.primary, CircleShape),
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Duraklat" else "Oynat",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

