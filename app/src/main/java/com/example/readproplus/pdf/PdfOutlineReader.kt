package com.example.readproplus.pdf

import com.example.readproplus.model.pdf.TocEntry
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination

object PdfOutlineReader {
    fun readOutline(doc: PDDocument): List<TocEntry> {
        val outline = doc.documentCatalog.documentOutline ?: return emptyList()

        val result = mutableListOf<TocEntry>()
        for (item in outline.children()) {
            processItem(item, 0, result, doc)
        }
        return result
    }

    private fun processItem(
        item: com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem,
        depth: Int,
        result: MutableList<TocEntry>,
        doc: PDDocument,
    ) {
        val pageNumber = resolvePageNumber(item, doc)
        result.add(
            TocEntry(
                title = item.title,
                pageNumber = pageNumber,
                depth = depth,
            )
        )

        for (child in item.children()) {
            processItem(child, depth + 1, result, doc)
        }
    }

    private fun resolvePageNumber(
        item: com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem,
        doc: PDDocument,
    ): Int {
        return try {
            val destination = resolveDestination(item)
            if (destination is PDPageDestination) {
                val page = destination.page
                if (page != null) {
                    doc.pages.indexOf(page) + 1
                } else {
                    (destination.pageNumber + 1).coerceAtLeast(1)
                }
            } else {
                1
            }
        } catch (_: Exception) {
            1
        }
    }

    private fun resolveDestination(
        item: com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem,
    ): Any? {
        val action = item.action
        if (action is PDActionGoTo) {
            return action.destination
        }
        return item.destination
    }
}
