package com.example.readproplus.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.rendering.DocumentPageRenderer
import com.example.readproplus.ui.theme.ReaderColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val coverRenderMutex = Mutex()

/**
 * Shows the first rendered page as a real document cover when the source is
 * still available. Text-only formats and unavailable SAF sources use the
 * consistent metadata fallback instead.
 */
@Composable
fun DocumentCover(
    document: PdfDocument,
    scheme: ReaderColorScheme,
    modifier: Modifier = Modifier,
    maxWidthPx: Int = 640,
) {
    val context = LocalContext.current
    var bitmap by remember(document.id, document.sourceUri, document.filePath, document.format) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(document.id, document.sourceUri, document.filePath, document.format, maxWidthPx) {
        bitmap = null
        bitmap = withContext(Dispatchers.IO) {
            coverRenderMutex.withLock {
                runCatching {
                    DocumentPageRenderer.render(context, document, pageIndex = 0, maxWidthPx = maxWidthPx)
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier.background(scheme.surfaceColor),
        contentAlignment = Alignment.Center,
    ) {
        val pageBitmap = bitmap
        if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap.asImageBitmap(),
                contentDescription = "${document.title} cover",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            DocumentCoverFallback(document = document, scheme = scheme)
        }
    }
}

@Composable
private fun DocumentCoverFallback(
    document: PdfDocument,
    scheme: ReaderColorScheme,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.accentColor.copy(alpha = 0.16f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = document.title.take(1).ifBlank { "?" },
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.accentColor,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = document.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.textColor,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = document.format.displayName,
            fontSize = 9.sp,
            color = scheme.pageNumberColor,
        )
    }
}
