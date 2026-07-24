package com.example.readproplus.model

data class BookCollection(
    val id: String,
    val name: String,
    val bookIds: List<String> = emptyList(),
)

data class BookFolder(
    val id: String,
    val name: String,
    val bookIds: List<String> = emptyList(),
)

data class Series(
    val name: String,
    val bookIds: List<String> = emptyList(),
)
