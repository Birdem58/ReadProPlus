package com.example.readproplus.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomReaderNavBarProgressTest {

    @Test
    fun `slider position at first page is zero`() {
        assertEquals(0f, pageToSliderPosition(1, 100), 0.001f)
        assertEquals(0f, pageToSliderPosition(1, 1), 0.001f)
        assertEquals(0f, pageToSliderPosition(1, 2), 0.001f)
        assertEquals(0f, pageToSliderPosition(1, 0), 0.001f)
    }

    @Test
    fun `slider position at last page is one`() {
        assertEquals(1f, pageToSliderPosition(100, 100), 0.001f)
        assertEquals(1f, pageToSliderPosition(2, 2), 0.001f)
    }

    @Test
    fun `slider position at middle page`() {
        assertEquals(0.5f, pageToSliderPosition(50, 99), 0.001f)
        assertEquals(0.5f, pageToSliderPosition(6, 11), 0.001f)
    }

    @Test
    fun `slider position for single page document`() {
        assertEquals(0f, pageToSliderPosition(1, 1), 0.001f)
    }

    @Test
    fun `slider position for empty document`() {
        assertEquals(0f, pageToSliderPosition(1, 0), 0.001f)
    }

    @Test
    fun `page from slider at start`() {
        assertEquals(1, sliderToPage(0f, 100))
        assertEquals(1, sliderToPage(0f, 1))
        assertEquals(1, sliderToPage(0f, 2))
    }

    @Test
    fun `page from slider at end`() {
        assertEquals(100, sliderToPage(1f, 100))
        assertEquals(2, sliderToPage(1f, 2))
    }

    @Test
    fun `page from slider in middle`() {
        assertEquals(50, sliderToPage(0.5f, 99))
        assertEquals(6, sliderToPage(0.5f, 11))
    }

    @Test
    fun `page from slider with rounding`() {
        assertEquals(1, sliderToPage(0.01f, 100))
        assertEquals(2, sliderToPage(0.02f, 100))
        assertEquals(50, sliderToPage(0.495f, 100))
        assertEquals(50, sliderToPage(0.505f, 100))
    }

    @Test
    fun `page from slider for single page document`() {
        assertEquals(1, sliderToPage(0f, 1))
        assertEquals(1, sliderToPage(0.5f, 1))
        assertEquals(1, sliderToPage(1f, 1))
    }

    @Test
    fun `page from slider for empty document`() {
        assertEquals(1, sliderToPage(0f, 0))
        assertEquals(1, sliderToPage(1f, 0))
    }

    @Test
    fun `slider and page conversions are inverses for edge pages`() {
        val pageCounts = listOf(2, 3, 10, 50, 100, 500)
        for (totalPages in pageCounts) {
            assertEquals(1, sliderToPage(pageToSliderPosition(1, totalPages), totalPages))
            assertEquals(
                totalPages,
                sliderToPage(pageToSliderPosition(totalPages, totalPages), totalPages),
            )
        }
    }

    @Test
    fun `slider and page conversions are inverses for middle pages`() {
        val totalPages = 100
        for (page in listOf(25, 50, 75)) {
            val sliderPos = pageToSliderPosition(page, totalPages)
            val convertedBack = sliderToPage(sliderPos, totalPages)
            assertEquals(page, convertedBack)
        }
    }

    @Test
    fun `slider positions are monotonic`() {
        val totalPages = 50
        var prevPos = -1f
        for (page in 1..totalPages) {
            val pos = pageToSliderPosition(page, totalPages)
            if (prevPos >= 0f) {
                assert(pos > prevPos) { "Position for page $page not > page ${page - 1}" }
            }
            prevPos = pos
        }
    }

    @Test
    fun `slider mapping clamps invalid values`() {
        assertEquals(0f, pageToSliderPosition(0, 100), 0.001f)
        assertEquals(1f, pageToSliderPosition(101, 100), 0.001f)
        assertEquals(1, sliderToPage(-1f, 100))
        assertEquals(100, sliderToPage(2f, 100))
    }

    @Test
    fun `page navigation does not go below 1`() {
        assertEquals(1, previousPage(1))
        assertEquals(1, previousPage(0))
        assertEquals(1, previousPage(-5))
    }

    @Test
    fun `page navigation does not exceed total`() {
        assertEquals(100, nextPage(100, 100))
        assertEquals(100, nextPage(101, 100))
    }

    @Test
    fun `previous page works correctly`() {
        assertEquals(4, previousPage(5))
        assertEquals(1, previousPage(2))
    }

    @Test
    fun `next page works correctly`() {
        assertEquals(6, nextPage(5, 10))
        assertEquals(10, nextPage(9, 10))
    }

    companion object {
        fun previousPage(currentPage: Int): Int {
            return if (currentPage > 1) currentPage - 1 else 1
        }

        fun nextPage(currentPage: Int, totalPages: Int): Int {
            return if (currentPage < totalPages) currentPage + 1 else totalPages
        }
    }
}
