package com.example.readproplus.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.readproplus.model.BookNote
import com.example.readproplus.model.Bookmark
import com.example.readproplus.model.Highlight
import com.example.readproplus.model.ReadingStats
import java.io.File

class DataExporter(private val context: Context) {

    fun exportToMarkdown(
        highlights: List<Highlight>,
        notes: List<BookNote>,
        bookmarks: List<Bookmark>,
        stats: ReadingStats,
    ): String = generateMarkdownExport(highlights, notes, bookmarks, stats)

    /** Creates a real Markdown attachment and shares it through FileProvider. */
    fun shareExportData(content: String, mimeType: String = "text/markdown") {
        val exportUri = writeMarkdownFile(content)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "ReadProPlus Highlights & Reading Export")
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_STREAM, exportUri)
            clipData = ClipData.newRawUri("ReadProPlus export", exportUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Export ReadProPlus Data").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun writeMarkdownFile(content: String, fileName: String = defaultFileName()): android.net.Uri {
        val exportDirectory = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { defaultFileName() }
        val file = File(exportDirectory, safeName)
        file.writeText(content, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun defaultFileName(): String = "readproplus-export-${System.currentTimeMillis()}.md"
}

fun generateMarkdownExport(
    highlights: List<Highlight>,
    notes: List<BookNote>,
    bookmarks: List<Bookmark>,
    stats: ReadingStats,
): String {
    val sb = StringBuilder()
    sb.append("# ReadProPlus User Export\n\n")
    sb.append("## Reading Statistics\n")
    sb.append("- Total Reading Time: ${stats.totalReadingTimeSeconds / 60} minutes\n")
    sb.append("- Total Pages Read: ${stats.totalPagesRead}\n")
    sb.append("- Books Finished: ${stats.booksFinished}\n")
    sb.append("- Reading Streak: ${stats.currentStreakDays} days\n\n")

    sb.append("## Highlights (${highlights.size})\n")
    if (highlights.isEmpty()) {
        sb.append("_No highlights saved._\n\n")
    } else {
        highlights.forEach { h ->
            sb.append("> \"${h.text}\"\n")
            sb.append("— *${h.bookTitle}* (Page ${h.pageNumber})\n\n")
        }
    }

    sb.append("## Written Notes (${notes.size})\n")
    if (notes.isEmpty()) {
        sb.append("_No notes saved._\n\n")
    } else {
        notes.forEach { n ->
            sb.append("### Note for ${n.bookTitle} (Page ${n.pageNumber})\n")
            n.textSnippet?.let { sb.append("> Reference: \"$it\"\n\n") }
            sb.append("${n.noteText}\n\n")
        }
    }

    sb.append("## Bookmarks (${bookmarks.size})\n")
    if (bookmarks.isEmpty()) sb.append("_No bookmarks saved._\n")
    bookmarks.forEach { b -> sb.append("- ${b.bookTitle} — Page ${b.pageNumber}\n") }
    return sb.toString()
}
