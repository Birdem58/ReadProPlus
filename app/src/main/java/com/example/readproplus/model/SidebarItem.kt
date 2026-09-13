package com.example.readproplus.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material.icons.filled.BarChart

import androidx.compose.material.icons.filled.Headphones

enum class SidebarSection(
    val label: String,
    val icon: ImageVector,
) {
    READING_NOW("Reading Now", Icons.Default.AutoStories),
    BOOKS_AND_DOCUMENTS("Books and Documents", Icons.AutoMirrored.Filled.LibraryBooks),
    AUDIOBOOKS("Audiobooks", Icons.Default.Headphones),
    FAVORITES("Favorites", Icons.Default.Favorite),
    TO_READ("To Read", Icons.AutoMirrored.Filled.PlaylistAddCheck),
    HAVE_READ("Have Read", Icons.AutoMirrored.Filled.MenuBook),
    AUTHORS("Authors", Icons.Default.People),
    SERIES("Series", Icons.Default.CollectionsBookmark),
    COLLECTIONS("Collections", Icons.Default.BookmarkBorder),
    FOLDERS("Folders", Icons.Default.Folder),
    DOWNLOADS("Downloads", Icons.Default.Download),
    READING_STATS("Reading Stats", Icons.Default.BarChart),
    TRASH("Trash", Icons.Default.Delete),
    SETTINGS("Settings", Icons.Default.Settings),
}
