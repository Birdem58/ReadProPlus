package com.example.readproplus.pdf

import android.net.Uri

/** Resolves a user-visible title when a PDF does not contain a usable title. */
internal object PdfTitleResolver {
    fun fromDisplayName(displayName: String?): String? {
        val name = displayName
            ?.trim()
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return name
            .substringBeforeLast('.', missingDelimiterValue = name)
            .trim()
            .takeIf { it.isNotBlank() }
    }

    fun fromUri(uri: Uri): String? {
        val pathName = uri.path
            ?.substringAfterLast('/')
            ?.takeIf { it.contains('.') }
        return fromDisplayName(pathName)
    }

    fun usableTitle(title: String?): String? {
        val cleaned = title?.replace('\u0000', ' ')?.replace(Regex("\\s+"), " ")?.trim()
        return cleaned?.takeIf {
            it.isNotBlank() && !it.equals("untitled", ignoreCase = true) &&
                !it.equals("(untitled)", ignoreCase = true)
        }
    }
}
