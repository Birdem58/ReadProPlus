package com.example.readproplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.example.readproplus.model.ReadingMode
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.pdf.getPdfDisplayName
import com.example.readproplus.pdf.rememberPdfPickerLauncher
import com.example.readproplus.ui.components.PdfErrorBanner
import com.example.readproplus.ui.components.PdfLoadingIndicator
import com.example.readproplus.ui.components.PdfPasswordDialog
import com.example.readproplus.ui.theme.readerColorScheme
import com.example.readproplus.ui.viewmodel.PdfExtractorUiState
import com.example.readproplus.ui.viewmodel.PdfExtractorViewModel

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
) {
    val scheme = readerColorScheme(readingMode)
    val viewState by viewModel.state.collectAsState()
    val libraryBooks by viewModel.libraryBooks.collectAsState()
    val context = LocalContext.current

    val pickPdfLauncher = rememberPdfPickerLauncher(
        onPdfSelected = { uri ->
            viewModel.onPdfPicked(uri)
        }
    )

    var showPasswordDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    when (val state = viewState) {
        is PdfExtractorUiState.NeedsPassword -> {
            showPasswordDialog = true
        }
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
                (doc.author?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
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
                                Text(
                                    "Search books...",
                                    color = scheme.navigationContent.copy(alpha = 0.6f),
                                )
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
                            Text(
                                "ReadProPlus",
                                fontWeight = FontWeight.Bold,
                                color = scheme.navigationContent,
                            )
                        }
                    }
                },
                actions = {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surfaceColor,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pickPdfLauncher.launch(arrayOf("application/pdf"))
                },
                containerColor = scheme.accentColor,
                contentColor = scheme.background,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add PDF",
                )
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
                    currentPage = 1,
                    totalPages = 1,
                    progress = loading.progress,
                    scheme = scheme,
                )
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
                            text = if (isSearchActive && libraryBooks.isNotEmpty()) "Try a different search term" else "Tap + to add a PDF",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.pageNumberColor.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredBooks, key = { it.id }) { document ->
                        PdfBookCard(
                            document = document,
                            readingMode = readingMode,
                            onClick = { onBookClick(document) },
                            onDelete = { viewModel.removeBook(document.id) },
                        )
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
) {
    val scheme = readerColorScheme(readingMode)
    val colorIndex = kotlin.math.abs(document.title.hashCode()) % bookColors.size
    val bookColor = bookColors[colorIndex]
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(bookColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = document.title.take(1),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = bookColor,
                )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
        }
    }
}
