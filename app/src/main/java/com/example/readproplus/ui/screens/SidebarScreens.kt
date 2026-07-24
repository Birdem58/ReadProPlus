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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.People

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.BookCollection
import com.example.readproplus.model.BookFolder
import com.example.readproplus.model.ReaderProgress
import com.example.readproplus.model.SidebarSection
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.ui.theme.ReaderColorScheme

private val bookColors = listOf(
    Color(0xFF2E7D32), Color(0xFF8B5E3C), Color(0xFFC62828),
    Color(0xFF6A1B9A), Color(0xFF1565C0), Color(0xFFE65100),
    Color(0xFF37474F), Color(0xFF4E342E), Color(0xFF558B2F),
    Color(0xFFB71C1C), Color(0xFF4A148C), Color(0xFF0D47A1),
)

private fun bookColor(title: String): Color {
    val idx = kotlin.math.abs(title.hashCode()) % bookColors.size
    return bookColors[idx]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingNowScreen(
    readingProgress: List<ReaderProgress>,
    allBooks: List<PdfDocument>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
    onRemoveProgress: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoStories, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.READING_NOW.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (readingProgress.isEmpty()) {
            EmptyState(scheme, Icons.Default.AutoStories, "No books in progress", "Reading progress will appear here")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(readingProgress, key = { it.bookId }) { progress ->
                    val book = allBooks.find { it.id == progress.bookId }
                    ProgressCard(
                        progress = progress,
                        book = book,
                        scheme = scheme,
                        onClick = { book?.let(onBookClick) },
                        onRemove = { onRemoveProgress(progress.bookId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    progress: ReaderProgress,
    book: PdfDocument?,
    scheme: ReaderColorScheme,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(12.dp).clip(CircleShape).background(bookColor(progress.bookTitle)),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = progress.bookTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Page ${progress.currentPage} of ${progress.totalPages}",
                    fontSize = 13.sp,
                    color = scheme.pageNumberColor,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(progress.progressFraction * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.accentColor,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.progressFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = scheme.accentColor,
                trackColor = scheme.surfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Remove progress", tint = scheme.pageNumberColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksAndDocumentsScreen(
    books: List<PdfDocument>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.BOOKS_AND_DOCUMENTS.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (books.isEmpty()) {
            EmptyState(scheme, Icons.AutoMirrored.Filled.LibraryBooks, "No books yet", "Import PDFs to see them here")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(books, key = { it.id }) { doc ->
                    BookCard(doc, scheme, onClick = { onBookClick(doc) })
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    document: PdfDocument,
    scheme: ReaderColorScheme,
    onClick: () -> Unit,
) {
    val color = bookColor(document.title)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(color.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(document.title.take(1), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(document.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = scheme.textColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(document.author ?: "Unknown Author", style = MaterialTheme.typography.bodySmall, color = scheme.pageNumberColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(6.dp))
                    Text("${document.totalPages} pages", fontSize = 11.sp, color = scheme.pageNumberColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    books: List<PdfDocument>,
    favorites: Set<String>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val favBooks = books.filter { it.id in favorites }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.FAVORITES.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (favBooks.isEmpty()) {
            EmptyState(scheme, Icons.Default.Favorite, "No favorites yet", "Tap the heart icon on any book to add it here")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(favBooks, key = { it.id }) { doc ->
                    BookCard(doc, scheme, onClick = { onBookClick(doc) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToReadScreen(
    books: List<PdfDocument>,
    toRead: Set<String>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
    onToggleToRead: (String) -> Unit,
) {
    val toReadBooks = books.filter { it.id in toRead }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.TO_READ.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (toReadBooks.isEmpty()) {
            EmptyState(scheme, Icons.AutoMirrored.Filled.PlaylistAddCheck, "Nothing to read yet", "Add books to your reading list")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(toReadBooks, key = { it.id }) { doc ->
                    BookCard(doc, scheme, onClick = { onBookClick(doc) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaveReadScreen(
    books: List<PdfDocument>,
    haveRead: Set<String>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
) {
    val readBooks = books.filter { it.id in haveRead }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.HAVE_READ.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (readBooks.isEmpty()) {
            EmptyState(scheme, Icons.AutoMirrored.Filled.MenuBook, "No books read yet", "Finished books will appear here")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(readBooks, key = { it.id }) { doc ->
                    BookCard(doc, scheme, onClick = { onBookClick(doc) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorsScreen(
    authors: Map<String, List<PdfDocument>>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.AUTHORS.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (authors.isEmpty()) {
            EmptyState(scheme, Icons.Default.People, "No authors found", "Books with author metadata will appear here")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(authors.toList(), key = { _, pair -> pair.first }) { _, (author, authorBooks) ->
                    Column {
                        Text(
                            text = author,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.accentColor,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        authorBooks.forEach { doc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { onBookClick(doc) }.padding(vertical = 6.dp, horizontal = 8.dp),
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(bookColor(doc.title)))
                                Spacer(Modifier.width(12.dp))
                                Text(doc.title, color = scheme.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("${doc.totalPages}p", fontSize = 12.sp, color = scheme.pageNumberColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    seriesMap: Map<String, List<PdfDocument>>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CollectionsBookmark, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.SERIES.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (seriesMap.isEmpty()) {
            EmptyState(scheme, Icons.Default.CollectionsBookmark, "No series yet", "Books grouped by series will appear here")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(seriesMap.toList(), key = { _, pair -> pair.first }) { _, (seriesName, seriesBooks) ->
                    Column {
                        Text(
                            text = seriesName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.accentColor,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        seriesBooks.forEach { doc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { onBookClick(doc) }.padding(vertical = 6.dp, horizontal = 8.dp),
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(bookColor(doc.title)))
                                Spacer(Modifier.width(12.dp))
                                Text(doc.title, color = scheme.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("${doc.totalPages}p", fontSize = 12.sp, color = scheme.pageNumberColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    collections: List<BookCollection>,
    books: List<PdfDocument>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDeleteCollection: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; newName = "" },
            title = { Text("New Collection") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Collection name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = { if (newName.isNotBlank()) { onCreateCollection(newName.trim()); newName = ""; showDialog = false } }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; newName = "" }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BookmarkBorder, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.COLLECTIONS.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, "Create collection", tint = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (collections.isEmpty()) {
            EmptyState(scheme, Icons.Default.BookmarkBorder, "No collections yet", "Tap + to create your first collection")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(collections, key = { it.id }) { collection ->
                    val collectionBooks = books.filter { it.id in collection.bookIds }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BookmarkBorder, null, tint = scheme.accentColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(collection.name, fontWeight = FontWeight.SemiBold, color = scheme.textColor, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onDeleteCollection(collection.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", tint = scheme.pageNumberColor, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (collectionBooks.isEmpty()) {
                                Text("Empty collection", fontSize = 13.sp, color = scheme.pageNumberColor, modifier = Modifier.padding(start = 32.dp, top = 8.dp))
                            } else {
                                Spacer(Modifier.height(8.dp))
                                collectionBooks.forEach { doc ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable { onBookClick(doc) }.padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(bookColor(doc.title)))
                                        Spacer(Modifier.width(8.dp))
                                        Text(doc.title, fontSize = 13.sp, color = scheme.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    folders: List<BookFolder>,
    books: List<PdfDocument>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; newName = "" },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Folder name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = { if (newName.isNotBlank()) { onCreateFolder(newName.trim()); newName = ""; showDialog = false } }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; newName = "" }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.FOLDERS.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, "Create folder", tint = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (folders.isEmpty()) {
            EmptyState(scheme, Icons.Default.Folder, "No folders yet", "Tap + to create your first folder")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(folders, key = { it.id }) { folder ->
                    val folderBooks = books.filter { it.id in folder.bookIds }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FolderOpen, null, tint = scheme.accentColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(folder.name, fontWeight = FontWeight.SemiBold, color = scheme.textColor, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onDeleteFolder(folder.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", tint = scheme.pageNumberColor, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (folderBooks.isEmpty()) {
                                Text("Empty folder", fontSize = 13.sp, color = scheme.pageNumberColor, modifier = Modifier.padding(start = 32.dp, top = 8.dp))
                            } else {
                                Spacer(Modifier.height(8.dp))
                                folderBooks.forEach { doc ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable { onBookClick(doc) }.padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(bookColor(doc.title)))
                                        Spacer(Modifier.width(8.dp))
                                        Text(doc.title, fontSize = 13.sp, color = scheme.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    books: List<PdfDocument>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.DOWNLOADS.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (books.isEmpty()) {
            EmptyState(scheme, Icons.Default.Download, "No downloads yet", "Downloaded books will appear here")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(books, key = { it.id }) { doc ->
                    BookCard(doc, scheme, onClick = { onBookClick(doc) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashBooks: List<PdfDocument>,
    scheme: ReaderColorScheme,
    onBookClick: (PdfDocument) -> Unit,
    onRestore: (String) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.TRASH.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                actions = {
                    if (trashBooks.isNotEmpty()) {
                        IconButton(onClick = onEmptyTrash) {
                            Icon(Icons.Default.DeleteForever, "Empty trash", tint = scheme.navigationContent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        if (trashBooks.isEmpty()) {
            EmptyState(scheme, Icons.Default.Delete, "Trash is empty", "Deleted books will appear here")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(trashBooks, key = { it.id }) { doc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(bookColor(doc.title)))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, fontWeight = FontWeight.SemiBold, color = scheme.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(doc.author ?: "Unknown", fontSize = 12.sp, color = scheme.pageNumberColor)
                            }
                            IconButton(onClick = { onRestore(doc.id) }) {
                                Icon(Icons.Default.RestoreFromTrash, "Restore", tint = scheme.accentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scheme: ReaderColorScheme,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = scheme.accentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SidebarSection.SETTINGS.label, fontWeight = FontWeight.Bold, color = scheme.navigationContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surfaceColor),
            )
        },
        containerColor = scheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingsSection("Reading", listOf(
                    "Default Font Size" to "Adjust reading text size",
                    "Scroll Mode" to "Paged / Vertical scrolling",
                    "Reading Mode" to "Light / Dark / Sepia / etc.",
                ), scheme)
            }
            item {
                SettingsSection("TTS (Text-to-Speech)", listOf(
                    "TTS Engine" to "Kokoro / System TTS",
                    "Speech Speed" to "Adjust reading speed",
                    "Default Voice" to "Choose TTS voice",
                    "Volume" to "Adjust TTS volume",
                ), scheme)
            }
            item {
                SettingsSection("Library", listOf(
                    "Auto-import" to "Automatically scan for PDFs",
                    "Sort by" to "Title / Author / Date added",
                ), scheme)
            }
            item {
                SettingsSection("Storage", listOf(
                    "Cache Size" to "Manage book cache",
                    "Clear Cache" to "Free up storage space",
                ), scheme)
            }
            item {
                SettingsSection("About", listOf(
                    "Version" to "1.0.3",
                    "License" to "MIT License",
                ), scheme)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    items: List<Pair<String, String>>,
    scheme: ReaderColorScheme,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.accentColor,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                items.forEachIndexed { index, (label, desc) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, color = scheme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(desc, color = scheme.pageNumberColor, fontSize = 12.sp)
                        }
                    }
                    if (index < items.size - 1) {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    scheme: ReaderColorScheme,
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = scheme.pageNumberColor.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.pageNumberColor)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.pageNumberColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}
