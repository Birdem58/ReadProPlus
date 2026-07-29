package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.PdfDocument

interface DocumentParser {
    fun canHandle(extension: String): Boolean
    fun parse(context: Context, uri: Uri): PdfDocument
}
