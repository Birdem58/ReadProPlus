package com.example.readproplus.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.rendering.DocumentPageRenderer
import com.example.readproplus.ui.theme.ReaderColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SmartZoomableCanvas(
    pageIndex: Int,
    document: PdfDocument?,
    scheme: ReaderColorScheme,
    showControls: Boolean = true,
    onSingleTap: () -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isZoomLocked by remember { mutableStateOf(true) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    fun toggleZoomLock() {
        isZoomLocked = !isZoomLocked
        if (isZoomLocked) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        } else if (scale <= 1f) {
            scale = 2f
        }
    }

    LaunchedEffect(document?.id, document?.sourceUri, pageIndex) {
        pageBitmap = null
        pageBitmap = withContext(Dispatchers.IO) {
            document?.let { DocumentPageRenderer.render(context, it, pageIndex) }
        }
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        isZoomLocked = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isZoomLocked) {
                    if (!isZoomLocked) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onSingleTap() },
                        onDoubleTap = { toggleZoomLock() },
                    )
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = pageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Page image unavailable", fontSize = 18.sp, color = scheme.textColor)
                        Spacer(Modifier.height(8.dp))
                        Text("Page ${pageIndex + 1}", fontSize = 13.sp, color = scheme.pageNumberColor)
                    }
                }
            }
        }

        if (showControls) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = pageIndex > 0,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(scheme.surfaceColor.copy(alpha = 0.9f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous page",
                        tint = scheme.textColor,
                    )
                }

                IconButton(
                    onClick = onNextPage,
                    enabled = document != null && pageIndex < document.totalPages - 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(scheme.surfaceColor.copy(alpha = 0.9f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next page",
                        tint = scheme.textColor,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.surfaceColor.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { scale = (scale - 0.2f).coerceAtLeast(1f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = scheme.textColor)
                }
                Text(
                    text = "${(scale * 100).toInt()}%",
                    fontSize = 13.sp,
                    color = scheme.textColor,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(onClick = { scale = (scale + 0.2f).coerceAtMost(5f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = scheme.textColor)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { toggleZoomLock() }) {
                    Icon(
                        imageVector = if (isZoomLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Zoom lock",
                        tint = if (isZoomLocked) scheme.accentColor else scheme.textColor,
                    )
                }
            }
        }
    }
}
