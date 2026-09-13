package com.example.readproplus.pdf

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfTextNormalizerTest {

    @Test
    fun normalizesPdfWhitespaceWithoutFlatteningPageLines() {
        val normalized = PdfTextNormalizer.normalizePage(
            "  First\u00A0line\r\nSecond\tline\n\n\nThird line  ",
        )

        assertEquals("First line\nSecond line\n\nThird line", normalized)
    }

    @Test
    fun expandsCommonLigaturesAndRemovesSoftHyphens() {
        assertEquals(
            "A first office flow",
            PdfTextNormalizer.normalizePage("A \uFB01rst of\u00ADfice \uFB02ow"),
        )
    }
}
