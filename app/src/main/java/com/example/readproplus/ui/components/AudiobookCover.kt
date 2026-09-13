package com.example.readproplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.ui.theme.ReadProPalette

private val coverAccents = listOf(
    Color(0xFF3F766D),
    Color(0xFF6D5C82),
    Color(0xFFB66D52),
    Color(0xFF476B85),
    Color(0xFF8A6B45),
)

/** A lightweight, deterministic cover treatment for generated audiobooks. */
@Composable
fun AudiobookCover(
    title: String,
    author: String? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val accent = remember(title) {
        coverAccents[kotlin.math.abs(title.hashCode()) % coverAccents.size]
    }
    val titleLines = remember(title) {
        title.trim().split(Regex("\\s+")).chunked(if (compact) 2 else 3).joinToString("\n") { it.joinToString(" ") }
    }

    Surface(
        modifier = modifier
            .shadow(if (compact) 3.dp else 12.dp, RoundedCornerShape(if (compact) 12.dp else 24.dp))
            .clip(RoundedCornerShape(if (compact) 12.dp else 24.dp)),
        shape = RoundedCornerShape(if (compact) 12.dp else 24.dp),
        color = accent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.78f),
                            accent,
                            Color(0xFF203D38),
                        ),
                    ),
                )
                .padding(if (compact) 10.dp else 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 28.dp, y = (-32).dp)
                    .size(if (compact) 56.dp else 112.dp)
                    .background(Color.White.copy(alpha = 0.10f), CircleShape),
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = if (compact) androidx.compose.foundation.layout.Arrangement.Center else androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "$title sesli kitap kapağı",
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(if (compact) 20.dp else 32.dp),
                )

                if (!compact) {
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = titleLines.ifBlank { "Sesli kitap" },
                    color = Color.White,
                    fontSize = if (compact) 10.sp else 20.sp,
                    lineHeight = if (compact) 12.sp else 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (compact) 3 else 5,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!compact) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = author ?: "ReadPro+ Audio",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(ReadProPalette.warmStrong),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "READPRO+",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }
    }
}

