package com.example.readproplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.BookNote
import com.example.readproplus.model.Bookmark
import com.example.readproplus.ui.theme.ReaderColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBookmarksSheet(
    bookTitle: String,
    currentPage: Int,
    bookmarks: List<Bookmark>,
    notes: List<BookNote>,
    scheme: ReaderColorScheme,
    onAddNote: (noteText: String) -> Unit,
    onDeleteNote: (id: String) -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleBookmarkAtPage: (Int) -> Unit = {},
    isCurrentPageBookmarked: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Notes & Bookmarks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.textColor,
                    )
                    Text(
                        text = "$bookTitle — Page $currentPage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.pageNumberColor,
                    )
                }

                Button(
                    onClick = onToggleBookmark,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentPageBookmarked) scheme.accentColor else scheme.surfaceVariant,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmark",
                        tint = if (isCurrentPageBookmarked) scheme.background else scheme.textColor,
                    )
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(
                        text = if (isCurrentPageBookmarked) "Bookmarked" else "Bookmark Page",
                        color = if (isCurrentPageBookmarked) scheme.background else scheme.textColor,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Add Written Note Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.background),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("Write a note for Page $currentPage...") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (noteInput.isNotBlank()) {
                                onAddNote(noteInput)
                                noteInput = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.accentColor),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = scheme.background)
                        Spacer(Modifier.padding(horizontal = 2.dp))
                        Text("Add Note", color = scheme.background)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = scheme.dividerColor)
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Saved Notes (${notes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.textColor,
            )

            Spacer(Modifier.height(8.dp))

            if (bookmarks.isNotEmpty()) {
                Text(
                    text = "Saved Bookmarks (${bookmarks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.textColor,
                )
                bookmarks.forEach { bookmark ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = scheme.accentColor)
                        Text("Page ${bookmark.pageNumber}", color = scheme.textColor, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onToggleBookmarkAtPage(bookmark.pageNumber) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove bookmark", tint = scheme.pageNumberColor)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(240.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.background),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Page ${note.pageNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.accentColor,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = note.noteText,
                                    fontSize = 13.sp,
                                    color = scheme.textColor,
                                )
                            }
                            IconButton(onClick = { onDeleteNote(note.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = scheme.pageNumberColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
