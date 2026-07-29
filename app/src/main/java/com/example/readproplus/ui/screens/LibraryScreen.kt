package com.example.readproplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.LibraryViewMode
import com.example.readproplus.model.ReadingMode
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.pdf.getPdfDisplayName
import com.example.readproplus.pdf.rememberBatchDocumentPickerLauncher
import com.example.readproplus.pdf.rememberFolderPickerLauncher
import com.example.readproplus.pdf.rememberPdfPickerLauncher
import com.example.readproplus.storage.BatchImporter
import com.example.readproplus.storage.StorageScanner
import com.example.readproplus.ui.components.DocumentCover
import com.example.readproplus.ui.components.PdfErrorBanner
import com.example.readproplus.ui.components.PdfLoadingIndicator
import com.example.readproplus.ui.components.PdfPasswordDialog
import com.example.readproplus.ui.theme.readerColorScheme
import com.example.readproplus.ui.viewmodel.PdfExtractorUiState
import com.example.readproplus.ui.viewmodel.PdfExtractorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext

private const val DELETE_REVEAL_HOLD_MILLIS = 2_000L

/** Reveals a card's destructive action only after a deliberate two-second hold. */
private fun Modifier.revealDeleteAfterHold(onHold: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val heldForTwoSeconds = withTimeoutOrNull(DELETE_REVEAL_HOLD_MILLIS) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: return@withTimeoutOrNull false

                if (!change.pressed) return@withTimeoutOrNull false
                if (change.positionChange().getDistance() > viewConfiguration.touchSlop) {
                    return@withTimeoutOrNull false
                }
            }
        } == null

        if (heldForTwoSeconds) {
            onHold()

            // Prevent the same long press from also activating the card's normal click.
            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                change?.consume()
            } while (change?.pressed == true)
        }
    }
}

private fun bookScanMessage(count: Int): String = when (count) {
    0 -> "No books found"
    1 -> "Found 1 book"
    else -> "Found $count books"
}

private val bookColors = listOf(
    Color(0xFF2E7D32), Color(0xFF8B5E3C), Color(0xFFC62828),
    Color(0xFF6A1B9A), Color(0xFF1565C0), Color(0xFFE65100),
    Color(0xFF37474F), Color(0xFF4E342E), Color(0xFF558B2F),
    Color(0xFFB71C1C), Color(0xFF4A148C), Color(0xFF0D47A1),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    readingMode: ReadingMode,
    viewModel: PdfExtractorViewModel,
    onBookClick: (PdfDocument) -> Unit,
    onMenuClick: () -> Unit = {},
    favorites: Set<String> = emptySet(),
    toRead: Set<String> = emptySet(),
    haveRead: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    onToggleToRead: (String) -> Unit = {},
    onToggleHaveRead: (String) -> Unit = {},
) {
    val scheme = readerColorScheme(readingMode)
    val viewState by viewModel.state.collectAsState()
    val libraryBooks by viewModel.libraryBooks.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID) }
    var showImportMenu by remember { mutableStateOf(false) }

    val pickSingleLauncher = rememberPdfPickerLauncher(
        onPdfSelected = { uri -> viewModel.onPdfPicked(uri) }
    )

    val batchPickerLauncher = rememberBatchDocumentPickerLauncher(
        onDocumentsSelected = { uris ->
            viewModel.onDocumentsPicked(uris)
            scope.launch {
                snackbarHostState.showSnackbar(bookScanMessage(uris.size))
            }
        }
    )

    val folderPickerLauncher = rememberFolderPickerLauncher(
        onFolderSelected = { treeUri ->
            scope.launch {
                val uris = withContext(Dispatchers.IO) {
                    BatchImporter(context).scanFolderTree(treeUri)
                }
                viewModel.onDocumentsPicked(uris)
                snackbarHostState.showSnackbar(bookScanMessage(uris.size))
            }
        }
    )

    LaunchedEffect(Unit) {
        val foundUris = withContext(Dispatchers.IO) { StorageScanner(context).scanForBooks() }
        viewModel.onDocumentsPicked(foundUris)
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    when (viewState) {
        is PdfExtractorUiState.NeedsPassword -> showPasswordDialog = true
        else -> {}
    }

    if (showPasswordDialog) {
        val pwState = viewState as? PdfExtractorUiState.NeedsPassword
        PdfPasswordDialog(
            fileName = if (pwState != null) getPdfDisplayName(context, pwState.uri) else null,
            onConfirm = { password ->
                viewModel.onPasswordEntered(password)
                showPasswordDialog = false
            },
            onDismiss = {
                viewModel.onPasswordCancelled()
                showPasswordDialog = false
            },
        )
    }

    val filteredBooks = if (searchQuery.isBlank()) {
        libraryBooks
    } else {
        libraryBooks.filter { doc ->
            doc.title.contains(searchQuery, ignoreCase = true) ||
                (doc.author?.contains(searchQuery, ignoreCase = true) == true) ||
                (doc.subject?.contains(searchQuery, ignoreCase = true) == true) ||
                (doc.keywords?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = scheme.navigationContent,
                        )
                    }
                },
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Search books...", color = scheme.navigationContent.copy(alpha = 0.6f))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = scheme.navigationContent,
                                unfocusedTextColor = scheme.navigationContent,
                                cursorColor = scheme.accentColor,
                                focusedBorderColor = scheme.accentColor,
                                unfocusedBorderColor = scheme.navigationContent.copy(alpha = 0.5f),
                                focusedContainerColor = scheme.surfaceColor,
                                unfocusedContainerColor = scheme.surfaceColor,
                            ),
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                contentDescription = null,
                                tint = scheme.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("ReadProPlus", fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                        }
                    }
                },
                actions = {
                    // Find Books Action
                    IconButton(onClick = {
                        scope.launch {
                            val foundUris = withContext(Dispatchers.IO) {
                                StorageScanner(context).scanForBooks()
                            }
                            if (foundUris.isEmpty()) {
                                batchPickerLauncher.launch(arrayOf("*/*"))
                                snackbarHostState.showSnackbar(
                                    "No books found automatically. Select your book files.",
                                )
                            } else {
                                viewModel.onDocumentsPicked(foundUris)
                                snackbarHostState.showSnackbar(bookScanMessage(foundUris.size))
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Find Books on Device",
                            tint = scheme.navigationContent,
                        )
                    }

                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close search",
                                tint = scheme.navigationContent,
                            )
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = scheme.navigationContent,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showImportMenu = true },
                    containerColor = scheme.accentColor,
                    contentColor = scheme.background,
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Import Options")
                }

                DropdownMenu(
                    expanded = showImportMenu,
                    onDismissRequest = { showImportMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Select Document") },
                        onClick = {
                            showImportMenu = false
                            pickSingleLauncher.launch(arrayOf("*/*"))
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Batch Import Files") },
                        onClick = {
                            showImportMenu = false
                            batchPickerLauncher.launch(arrayOf("*/*"))
                        },
                        leadingIcon = { Icon(Icons.Default.ViewModule, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Batch Folder Import") },
                        onClick = {
                            showImportMenu = false
                            folderPickerLauncher.launch(null)
                        },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                    )
                }
            }
        },
        containerColor = scheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PdfErrorBanner(
                message = (viewState as? PdfExtractorUiState.Error)?.message ?: "",
                visible = viewState is PdfExtractorUiState.Error,
                onDismiss = { viewModel.dismissError() },
            )

            if (viewState is PdfExtractorUiState.Loading) {
                val loading = viewState as PdfExtractorUiState.Loading
                PdfLoadingIndicator(
                    currentPage = loading.currentPage,
                    totalPages = loading.totalPages,
                    progress = loading.progress,
                    scheme = scheme,
                    currentDocument = loading.currentDocument,
                    totalDocuments = loading.totalDocuments,
                )
            }

            // View Mode Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "View Mode:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.pageNumberColor,
                )

                LibraryViewMode.entries
                    .filterNot { it == LibraryViewMode.FULL_DETAIL }
                    .forEach { mode ->
                    FilterChip(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        label = { Text(mode.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.accentColor,
                            selectedLabelColor = scheme.background,
                        ),
                    )
                }
            }

            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = null,
                            tint = scheme.pageNumberColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (isSearchActive && libraryBooks.isNotEmpty()) "No results found" else "No books yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.pageNumberColor,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isSearchActive && libraryBooks.isNotEmpty()) "Try a different search term" else "Tap + or Refresh to scan storage",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.pageNumberColor.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                when (viewMode) {
                    LibraryViewMode.GRID -> GridViewLayout(
                        books = filteredBooks,
                        readingMode = readingMode,
                        onClick = onBookClick,
                        onDelete = { viewModel.removeBook(it) },
                        favorites = favorites,
                        toRead = toRead,
                        haveRead = haveRead,
                        onToggleFavorite = onToggleFavorite,
                        onToggleToRead = onToggleToRead,
                        onToggleHaveRead = onToggleHaveRead,
                    )
                    LibraryViewMode.COMPACT -> CompactViewLayout(filteredBooks, readingMode, onBookClick, { viewModel.removeBook(it) })
                    LibraryViewMode.THUMBNAILS -> ThumbnailsViewLayout(
                        books = filteredBooks,
                        readingMode = readingMode,
                        onClick = onBookClick,
                        onDelete = { viewModel.removeBook(it) },
                        favorites = favorites,
                        toRead = toRead,
                        haveRead = haveRead,
                        onToggleFavorite = onToggleFavorite,
                        onToggleToRead = onToggleToRead,
                        onToggleHaveRead = onToggleHaveRead,
                    )
                    LibraryViewMode.FULL_DETAIL -> FullDetailViewLayout(filteredBooks, readingMode, onBookClick, { viewModel.removeBook(it) })
                }
            }
        }
    }
}

@Composable
private fun GridViewLayout(
    books: List<PdfDocument>,
    readingMode: ReadingMode,
    onClick: (PdfDocument) -> Unit,
    onDelete: (String) -> Unit,
    favorites: Set<String>,
    toRead: Set<String>,
    haveRead: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onToggleToRead: (String) -> Unit,
    onToggleHaveRead: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(books, key = { it.id }) { document ->
            ThumbnailBookCard(
                document = document,
                readingMode = readingMode,
                onClick = { onClick(document) },
                onDelete = { onDelete(document.id) },
                isFavorite = document.id in favorites,
                isToRead = document.id in toRead,
                isHaveRead = document.id in haveRead,
                onToggleFavorite = { onToggleFavorite(document.id) },
                onToggleToRead = { onToggleToRead(document.id) },
                onToggleHaveRead = { onToggleHaveRead(document.id) },
            )
        }
    }
}

@Composable
private fun ThumbnailBookCard(
    document: PdfDocument,
    readingMode: ReadingMode,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isFavorite: Boolean,
    isToRead: Boolean,
    isHaveRead: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleToRead: () -> Unit,
    onToggleHaveRead: () -> Unit,
) {
    val scheme = readerColorScheme(readingMode)
    val colorIndex = kotlin.math.abs(document.title.hashCode()) % bookColors.size
    val bookColor = bookColors[colorIndex]
    var showDelete by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .revealDeleteAfterHold { showDelete = true }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                DocumentCover(
                    document = document,
                    scheme = scheme,
                    modifier = Modifier.fillMaxSize(),
                    maxWidthPx = 512,
                )
                Text(
                    text = "${document.totalPages} pages",
                    fontSize = 9.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(bookColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(document.title, fontSize = 11.sp, color = scheme.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (showDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = scheme.pageNumberColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            BookStatusActions(
                scheme = scheme,
                isFavorite = isFavorite,
                isToRead = isToRead,
                isHaveRead = isHaveRead,
                onToggleFavorite = onToggleFavorite,
                onToggleToRead = onToggleToRead,
                onToggleHaveRead = onToggleHaveRead,
            )
        }
    }
}

@Composable
private fun CompactViewLayout(
    books: List<PdfDocument>,
    readingMode: ReadingMode,
    onClick: (PdfDocument) -> Unit,
    onDelete: (String) -> Unit,
) {
    val scheme = readerColorScheme(readingMode)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(books, key = { it.id }) { doc ->
            var showDelete by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealDeleteAfterHold { showDelete = true }
                    .clickable { onClick(doc) },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    ) {
                        DocumentCover(
                            document = doc,
                            scheme = scheme,
                            modifier = Modifier.fillMaxSize(),
                            maxWidthPx = 256,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = scheme.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${doc.author ?: "Unknown"} • ${doc.totalPages} pages", fontSize = 11.sp, color = scheme.pageNumberColor)
                    }
                    if (showDelete) {
                        IconButton(onClick = { onDelete(doc.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = scheme.pageNumberColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailsViewLayout(
    books: List<PdfDocument>,
    readingMode: ReadingMode,
    onClick: (PdfDocument) -> Unit,
    onDelete: (String) -> Unit,
    favorites: Set<String>,
    toRead: Set<String>,
    haveRead: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onToggleToRead: (String) -> Unit,
    onToggleHaveRead: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(books, key = { it.id }) { document ->
            PdfBookCard(
                document = document,
                readingMode = readingMode,
                onClick = { onClick(document) },
                onDelete = { onDelete(document.id) },
                isFavorite = document.id in favorites,
                isToRead = document.id in toRead,
                isHaveRead = document.id in haveRead,
                onToggleFavorite = { onToggleFavorite(document.id) },
                onToggleToRead = { onToggleToRead(document.id) },
                onToggleHaveRead = { onToggleHaveRead(document.id) },
            )
        }
    }
}

@Composable
private fun FullDetailViewLayout(
    books: List<PdfDocument>,
    readingMode: ReadingMode,
    onClick: (PdfDocument) -> Unit,
    onDelete: (String) -> Unit,
) {
    val scheme = readerColorScheme(readingMode)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(books, key = { it.id }) { doc ->
            var showDelete by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealDeleteAfterHold { showDelete = true }
                    .clickable { onClick(doc) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    ) {
                        DocumentCover(
                            document = doc,
                            scheme = scheme,
                            modifier = Modifier.fillMaxSize(),
                            maxWidthPx = 384,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scheme.textColor)
                        Spacer(Modifier.height(4.dp))
                        Text("Author: ${doc.author ?: "Unknown"}", fontSize = 12.sp, color = scheme.pageNumberColor)
                        doc.subject?.let {
                            Text("Subject: $it", fontSize = 12.sp, color = scheme.pageNumberColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        doc.keywords?.let {
                            Text("Keywords: $it", fontSize = 12.sp, color = scheme.pageNumberColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("Format: ${doc.format.displayName} | Pages: ${doc.totalPages}", fontSize = 12.sp, color = scheme.pageNumberColor)
                    }
                    if (showDelete) {
                        IconButton(onClick = { onDelete(doc.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = scheme.pageNumberColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfBookCard(
    document: PdfDocument,
    readingMode: ReadingMode,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isFavorite: Boolean,
    isToRead: Boolean,
    isHaveRead: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleToRead: () -> Unit,
    onToggleHaveRead: () -> Unit,
) {
    val scheme = readerColorScheme(readingMode)
    val colorIndex = kotlin.math.abs(document.title.hashCode()) % bookColors.size
    val bookColor = bookColors[colorIndex]
    var showMenu by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .revealDeleteAfterHold { showDelete = true }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    ) {
                        DocumentCover(
                            document = document,
                            scheme = scheme,
                            modifier = Modifier.fillMaxSize(),
                            maxWidthPx = 640,
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(bookColor.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(document.format.displayName, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = document.author ?: "Unknown Author",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.pageNumberColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(bookColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${document.totalPages} pages",
                        fontSize = 11.sp,
                        color = scheme.pageNumberColor,
                    )
                    Spacer(Modifier.weight(1f))
                    if (showDelete) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = scheme.pageNumberColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Remove from library") },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                }
                BookStatusActions(
                    scheme = scheme,
                    isFavorite = isFavorite,
                    isToRead = isToRead,
                    isHaveRead = isHaveRead,
                    onToggleFavorite = onToggleFavorite,
                    onToggleToRead = onToggleToRead,
                    onToggleHaveRead = onToggleHaveRead,
                )
            }
        }
    }
}

@Composable
private fun BookStatusActions(
    scheme: com.example.readproplus.ui.theme.ReaderColorScheme,
    isFavorite: Boolean,
    isToRead: Boolean,
    isHaveRead: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleToRead: () -> Unit,
    onToggleHaveRead: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) scheme.accentColor else scheme.pageNumberColor,
                modifier = Modifier.size(17.dp),
            )
        }
        IconButton(
            onClick = onToggleToRead,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                contentDescription = if (isToRead) "Remove from To Read" else "Add to To Read",
                tint = if (isToRead) scheme.accentColor else scheme.pageNumberColor,
                modifier = Modifier.size(17.dp),
            )
        }
        IconButton(
            onClick = onToggleHaveRead,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = if (isHaveRead) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = if (isHaveRead) "Mark as unread" else "Mark as have read",
                tint = if (isHaveRead) scheme.accentColor else scheme.pageNumberColor,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}
