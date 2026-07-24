package com.example.readproplus.model.pdf

data class TocEntry(
    val title: String,
    val pageNumber: Int,
    val depth: Int,
)
