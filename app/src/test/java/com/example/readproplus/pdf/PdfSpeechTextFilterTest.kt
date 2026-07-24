package com.example.readproplus.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PdfSpeechTextFilterTest {

    @Test
    fun `keeps body text while removing common PDF chrome`() {
        val cleanedPages = PdfSpeechTextFilter.mainTextPages(
            listOf(
                "Research Notes\nThe model learns useful representations.\n1",
                "Research Notes\nFigure 2: Accuracy by epoch\nIt improves with more data.\nPage 2 of 20",
            ),
        )

        assertEquals(
            listOf(
                "The model learns useful representations.",
                "It improves with more data.",
            ),
            cleanedPages,
        )
    }

    @Test
    fun `removes standalone links and joins hyphenated line breaks`() {
        val cleanedPage = PdfSpeechTextFilter.mainTextPages(
            listOf(
                "The inter-\nnational result is clear.\nhttps://example.com/source\n12",
            ),
        ).single()

        assertEquals("The international result is clear.", cleanedPage)
        assertFalse(cleanedPage.contains("example.com"))
    }
}
