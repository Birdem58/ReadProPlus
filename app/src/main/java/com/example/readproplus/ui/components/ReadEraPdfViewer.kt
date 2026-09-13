package com.example.readproplus.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.readproplus.model.ReadingMode
import com.example.readproplus.model.ScrollMode
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.rendering.PdfPageRendererManager
import com.example.readproplus.rendering.ReaderPageColorFilter
import com.example.readproplus.ui.theme.ReaderColorScheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * High-performance ReadEra-style PDF & Document Viewer.
 * Features:
 * - Continuous Vertical Scroll (LazyColumn) or Horizontal Paged (HorizontalPager)
 * - Pinch-to-zoom and double-tap zoom (1.0x <-> 2.0x)
 * - Zero-layout-shift aspect ratio placeholders
 * - ReadEra color filters (Night, Sepia, Day)
 * - Real-time page synchronization
 */
@Composable
fun ReadEraPdfViewer(
    document: PdfDocument?,
    currentPage: Int,
    scrollMode: ScrollMode,
    readingMode: ReadingMode,
    scheme: ReaderColorScheme,
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit = {},
    onSingleTap: () -> Unit = {},
) {
    if (document == null || document.totalPages <= 0) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(scheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = scheme.accentColor)
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun toggleDoubleTapZoom() {
        if (scale > 1.1f) {
            resetZoom()
        } else {
            scale = 2.0f
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { toggleDoubleTapZoom() },
                )
            }
            .pointerInput(scale) {
                if (scale > 1.05f) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4.5f)
                        if (scale <= 1.05f) {
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            },
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.roundToPx() }

        when (scrollMode) {
            ScrollMode.VERTICAL -> {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = (currentPage - 1).coerceAtLeast(0),
                )

                // Report page change on scroll
                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemIndex }
                        .distinctUntilChanged()
                        .collect { pageIdx ->
                            onPageChanged(pageIdx + 1)
                        }
                }

                // Smooth scroll when currentPage changes externally (e.g. slider or audiobook sync)
                LaunchedEffect(currentPage) {
                    val targetIndex = (currentPage - 1).coerceIn(0, document.totalPages - 1)
                    if (listState.firstVisibleItemIndex != targetIndex) {
                        val distance = kotlin.math.abs(listState.firstVisibleItemIndex - targetIndex)
                        if (distance > 4) {
                            listState.scrollToItem(targetIndex)
                        } else {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(
                        count = document.totalPages,
                        key = { it },
                    ) { pageIndex ->
                        ReadEraPageItem(
                            document = document,
                            pageIndex = pageIndex,
                            readingMode = readingMode,
                            scheme = scheme,
                            screenWidthPx = screenWidthPx,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        )
                    }
                }
            }
            ScrollMode.PAGED -> {
                val pagerState = rememberPagerState(
                    initialPage = (currentPage - 1).coerceAtLeast(0),
                    pageCount = { document.totalPages },
                )

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .distinctUntilChanged()
                        .collect { pageIdx ->
                            onPageChanged(pageIdx + 1)
                        }
                }

                LaunchedEffect(currentPage) {
                    val targetPage = (currentPage - 1).coerceIn(0, document.totalPages - 1)
                    if (pagerState.currentPage != targetPage) {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) { pageIndex ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ReadEraPageItem(
                            document = document,
                            pageIndex = pageIndex,
                            readingMode = readingMode,
                            scheme = scheme,
                            screenWidthPx = screenWidthPx,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadEraPageItem(
    document: PdfDocument,
    pageIndex: Int,
    readingMode: ReadingMode,
    scheme: ReaderColorScheme,
    screenWidthPx: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex, document.id) {
        mutableStateOf(PdfPageRendererManager.getCachedBitmap(pageIndex))
    }
    var isLoading by remember(pageIndex, document.id) {
        mutableStateOf(bitmap == null)
    }

    val aspectRatio = remember(pageIndex, document.id) {
        PdfPageRendererManager.getPageAspectRatio(pageIndex)
    }

    LaunchedEffect(pageIndex, document.id) {
        if (bitmap == null) {
            isLoading = true
            val rendered = PdfPageRendererManager.renderPage(
                context = context,
                document = document,
                pageIndex = pageIndex,
                maxWidthPx = screenWidthPx.coerceAtLeast(720),
            )
            bitmap = rendered
            isLoading = false
        }
    }

    val colorFilter = remember(readingMode) {
        ReaderPageColorFilter.getColorFilter(readingMode)
    }

    Surface(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp)),
        color = when (readingMode) {
            ReadingMode.DARK, ReadingMode.OLED_DARK -> Color(0xFF1E1E1E)
            ReadingMode.SEPIA -> Color(0xFFFBF0D9)
            else -> Color.White
        },
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        strokeWidth = 2.dp,
                        color = scheme.accentColor.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
