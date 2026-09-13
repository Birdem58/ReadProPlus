package com.example.readproplus

import com.example.readproplus.data.DataExporter
import com.example.readproplus.model.BookNote
import com.example.readproplus.model.Bookmark
import com.example.readproplus.model.Highlight
import com.example.readproplus.model.LibraryViewMode
import com.example.readproplus.model.ReadingStats
import com.example.readproplus.model.pdf.FormatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadProPlusUnitTest {

    @Test
    fun testFormatTypeResolution() {
        assertEquals(FormatType.EPUB, FormatType.fromExtension("epub"))
        assertEquals(FormatType.MOBI, FormatType.fromExtension("mobi"))
        assertEquals(FormatType.FB2, FormatType.fromExtension("fb2"))
        assertEquals(FormatType.DOCX, FormatType.fromExtension("docx"))
        assertEquals(FormatType.ODT, FormatType.fromExtension("odt"))
        assertEquals(FormatType.DJVU, FormatType.fromExtension("djvu"))
        assertEquals(FormatType.CBZ, FormatType.fromExtension("cbz"))
        assertEquals(FormatType.CBR, FormatType.fromExtension("cbr"))
    }

    @Test
    fun testDataExporterMarkdownFormatting() {
        val highlights = listOf(
            Highlight("1", "book1", "Test Book", 5, "Sample highlight text", 0xFFFFFF, System.currentTimeMillis())
        )
        val notes = listOf(
            BookNote("1", "book1", "Test Book", 5, "My custom note", "Sample highlight text", System.currentTimeMillis())
        )
        val bookmarks = listOf(
            Bookmark("1", "book1", "Test Book", 5, System.currentTimeMillis())
        )
        val stats = ReadingStats(totalReadingTimeSeconds = 3600, totalPagesRead = 120, booksFinished = 3, currentStreakDays = 5)

        val markdown = com.example.readproplus.data.generateMarkdownExport(highlights, notes, bookmarks, stats)

        assertTrue(markdown.contains("ReadProPlus User Export"))
        assertTrue(markdown.contains("Total Reading Time: 60 minutes"))
        assertTrue(markdown.contains("Sample highlight text"))
        assertTrue(markdown.contains("My custom note"))
    }

    @Test
    fun testAudiobookModelAndPlaybackStatePageNumbering() {
        val book = com.example.readproplus.model.audiobook.Audiobook(
            id = "ab1",
            bookId = "book1",
            title = "Test Audiobook",
            voiceId = "piper_tr_TR_dfki_medium",
            voiceName = "DFKI",
            startPage = 1,
            endPage = 11,
            totalPages = 11,
            currentPageIndex = 7,
            sourceUri = "content://media/external/file/123",
            filePath = "/storage/emulated/0/Books/PixelCNN.pdf",
            mainTextOnly = true,
        )

        assertEquals(7, book.currentPage)
        assertEquals("content://media/external/file/123", book.sourceUri)
        assertEquals("/storage/emulated/0/Books/PixelCNN.pdf", book.filePath)
        assertTrue(book.mainTextOnly)

        val state = com.example.readproplus.tts.AudiobookPlaybackState(
            audiobook = book,
            currentPageIndex = 7,
            totalDurationMs = 120_000L,
            currentPositionMs = 45_000L,
        )

        assertEquals(7, state.currentPage)
    }
}
