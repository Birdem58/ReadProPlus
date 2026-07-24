package com.example.readproplus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.tts.TtsState
import java.util.Locale
import kotlin.math.roundToInt

private val ControlBarBg = Color(0xFF1B1B2F)
private val ControlBarAccent = Color(0xFF4FC3F7)

@Composable
fun TtsControlBar(
    ttsState: TtsState,
    volume: Float = 1f,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit = {},
    onSpeedClick: () -> Unit,
    onVolumeChange: (Float) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVisible = ttsState !is TtsState.Idle && ttsState !is TtsState.Stopped

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(ControlBarBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            when (ttsState) {
                is TtsState.Generating -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Generating audio...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${(ttsState.progress * 100).toInt()}%",
                            color = ControlBarAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ttsState.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ControlBarAccent,
                        trackColor = ControlBarAccent.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(4.dp))
                }

                is TtsState.Playing, is TtsState.Paused -> {
                    val currentMs = when (ttsState) {
                        is TtsState.Playing -> ttsState.currentMs
                        is TtsState.Paused -> ttsState.currentMs
                        else -> 0L
                    }
                    val totalMs = when (ttsState) {
                        is TtsState.Playing -> ttsState.totalMs
                        is TtsState.Paused -> ttsState.totalMs
                        else -> 0L
                    }
                    val progress = when (ttsState) {
                        is TtsState.Playing -> ttsState.progress
                        is TtsState.Paused -> ttsState.progress
                        else -> 0f
                    }
                    val speed = when (ttsState) {
                        is TtsState.Playing -> ttsState.speed
                        is TtsState.Paused -> ttsState.speed
                        else -> 1.0f
                    }

                    var isDragging by remember { mutableStateOf(false) }
                    var sliderPos by remember { mutableFloatStateOf(progress) }
                    val activeProgress = if (isDragging) sliderPos else progress

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatTime(if (isDragging) (sliderPos * totalMs).toLong() else currentMs),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                        Slider(
                            value = activeProgress.coerceIn(0f, 1f),
                            onValueChange = {
                                isDragging = true
                                sliderPos = it
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                onSeek(sliderPos)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = ControlBarAccent,
                                activeTrackColor = ControlBarAccent,
                                inactiveTrackColor = ControlBarAccent.copy(alpha = 0.25f),
                            ),
                        )
                        Text(
                            text = formatTime(totalMs),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = when {
                                volume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
                                volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = "Volume",
                            tint = ControlBarAccent,
                        )
                        Text(
                            text = "Volume",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                        Slider(
                            value = volume.coerceIn(0f, 1f),
                            onValueChange = onVolumeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = ControlBarAccent,
                                activeTrackColor = ControlBarAccent,
                                inactiveTrackColor = ControlBarAccent.copy(alpha = 0.25f),
                            ),
                        )
                        Text(
                            text = "${(volume.coerceIn(0f, 1f) * 100).roundToInt()}%",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (ttsState is TtsState.Playing) "Playing" else "Paused",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "${formatSpeed(speed)} speed",
                                color = ControlBarAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        IconButton(onClick = onSpeedClick) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Speed,
                                    contentDescription = "Speed",
                                    tint = ControlBarAccent,
                                )
                                Text(
                                    text = formatSpeed(speed),
                                    color = ControlBarAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 2.dp),
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                when (ttsState) {
                                    is TtsState.Playing -> onPause()
                                    is TtsState.Paused -> onPlay()
                                    else -> {}
                                }
                            }
                        ) {
                            Icon(
                                imageVector = when (ttsState) {
                                    is TtsState.Playing -> Icons.Filled.Pause
                                    else -> Icons.Filled.PlayArrow
                                },
                                contentDescription = if (ttsState is TtsState.Playing) "Pause" else "Play",
                                tint = ControlBarAccent,
                            )
                        }

                        IconButton(onClick = onStop) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop",
                                tint = Color.White.copy(alpha = 0.8f),
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatSpeed(speed: Float): String {
    return if (speed % 1.0f == 0f) {
        "${speed.toInt()}x"
    } else {
        "${speed}x"
    }
}
