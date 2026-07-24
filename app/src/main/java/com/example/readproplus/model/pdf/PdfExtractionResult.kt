package com.example.readproplus.model.pdf

sealed class PdfExtractionResult {
    data class Success(val document: PdfDocument) : PdfExtractionResult()
    data object PasswordProtected : PdfExtractionResult()
    data class WrongPassword(val attemptsRemaining: Int = 0) : PdfExtractionResult()
    data class Corrupted(val error: String) : PdfExtractionResult()
    data object TooLarge : PdfExtractionResult()
    data object NoText : PdfExtractionResult()
}
