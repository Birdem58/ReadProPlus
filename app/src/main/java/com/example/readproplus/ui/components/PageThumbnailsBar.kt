package com.example.readproplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.ui.theme.ReaderColorScheme

@Composable
fun PageThumbnailsBar(
    currentPage: Int,
    totalPages: Int,
    pages: List<String>,
    scheme: ReaderColorScheme,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentPage) {
        if (currentPage - 1 in pages.indices) {
            listState.animateScrollToItem((currentPage - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceColor)
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Page Thumbnails Preview",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.textColor,
            )
            Text(
                text = "$currentPage / $totalPages",
                fontSize = 11.sp,
                color = scheme.pageNumberColor,
            )
        }

        Spacer(Modifier.height(6.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(pages) { index, content ->
                val pageNum = index + 1
                val isSelected = pageNum == currentPage

                Card(
                    modifier = Modifier
                        .width(70.dp)
                        .height(95.dp)
                        .clickable { onPageSelected(pageNum) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) scheme.accentColor.copy(alpha = 0.2f) else scheme.background,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(scheme.surfaceVariant)
                                .padding(4.dp),
                        ) {
                            Text(
                                text = content.take(60).replace("\n", " "),
                                fontSize = 7.sp,
                                lineHeight = 8.sp,
                                color = scheme.textColor.copy(alpha = 0.8f),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$pageNum",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) scheme.accentColor else scheme.pageNumberColor,
                        )
                    }
                }
            }
        }
    }
}
