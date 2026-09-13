package com.example.readproplus.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfTitleResolverTest {
    @Test
    fun removesPdfExtensionFromProviderDisplayName() {
        assertEquals("My research paper", PdfTitleResolver.fromDisplayName("My research paper.pdf"))
    }

    @Test
    fun ignoresPlaceholderTitles() {
        assertNull(PdfTitleResolver.usableTitle("Untitled"))
        assertEquals("Real title", PdfTitleResolver.usableTitle("  Real   title "))
    }
}
