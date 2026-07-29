package com.example.readproplus.parser

import android.content.Context
import android.net.Uri
import com.example.readproplus.model.pdf.FormatType
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.pdf.TocEntry
import java.nio.charset.Charset

class MobiAzw3Parser : DocumentParser {

    override fun canHandle(extension: String): Boolean =
        extension.equals("mobi", ignoreCase = true) || extension.equals("azw3", ignoreCase = true)

    override fun parse(context: Context, uri: Uri): PdfDocument {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        val bytes = input.use { it.readBytes() }
        val extension = UniversalDocumentExtractor.getExtension(context, uri)
            val parsed = parsePalmDatabase(bytes)
        val pages = parsed.html
            .split(Regex("(?i)<mbp:pagebreak\\s*/?>|<pagebreak\\s*/?>"))
            .map(::cleanHtml)
            .filter { it.isNotBlank() }
            .flatMap { it.chunked(1800) }
            .ifEmpty { throw IllegalArgumentException("No readable text records found in $extension document") }
        return PdfDocument(
            id = uri.toString(),
            title = parsed.title.ifBlank { uri.lastPathSegment?.substringBeforeLast('.') ?: "Kindle Document" },
            author = parsed.author ?: "Unknown Author",
            totalPages = pages.size,
            pages = pages,
            toc = pages.mapIndexed { index, _ -> TocEntry("Section ${index + 1}", index + 1) },
            format = if (extension == "azw3") FormatType.AZW3 else FormatType.MOBI,
            sourceUri = uri.toString(),
        )
    }

    companion object {
        internal data class ParsedMobi(val title: String, val author: String?, val html: String)

        internal fun parsePalmDatabase(bytes: ByteArray): ParsedMobi {
            require(bytes.size >= 78) { "MOBI/PDB header is truncated" }
            val databaseTitle = readAscii(bytes, 0, 32)
            val recordCount = readU16(bytes, 76)
            require(recordCount > 1 && 78 + recordCount * 8 <= bytes.size) { "MOBI text record table is invalid" }
            val offsets = (0 until recordCount).map { index -> readU32(bytes, 78 + index * 8).toInt() }
            val records = offsets.mapIndexed { index, start ->
                val end = if (index + 1 < offsets.size) offsets[index + 1] else bytes.size
                require(start in 0 until bytes.size && end in start..bytes.size) { "MOBI record range is invalid" }
                bytes.copyOfRange(start, end)
            }
            val record0 = records.first()
            val compression = readU16(record0, 0)
            val textRecordCount = readU16(record0, 8).coerceAtMost(records.size - 1)
            val text = buildString {
                for (recordIndex in 1..textRecordCount) {
                    val record = records[recordIndex]
                    when (compression) {
                        1 -> append(decodeText(record))
                        2 -> append(decodeText(decompressPalmDoc(record)))
                        else -> throw IllegalArgumentException("Unsupported MOBI compression method: $compression")
                    }
                }
            }
            val mobiHeader = record0.indexOfSubsequence("MOBI".toByteArray(Charsets.US_ASCII))
            val title = extractFullName(record0, mobiHeader).ifBlank { databaseTitle }
            val author = extractExthAuthor(record0, mobiHeader)
            return ParsedMobi(title, author, text)
        }

        internal fun decompressPalmDoc(input: ByteArray): ByteArray {
            val output = ArrayList<Byte>(input.size * 2)
            var index = 0
            while (index < input.size) {
                val command = input[index++].toInt() and 0xff
                when {
                    command in 0x01..0x08 -> {
                        repeat(minOf(command, input.size - index)) { output += input[index++] }
                    }
                    command in 0x09..0x7f || command == 0 -> output += command.toByte()
                    command in 0x80..0xbf -> {
                        require(index < input.size) { "Truncated PalmDOC back-reference" }
                        val pair = (command shl 8) or (input[index++].toInt() and 0xff)
                        val distance = pair shr 3
                        val length = (pair and 0x07) + 3
                        require(distance in 1..output.size) { "Invalid PalmDOC back-reference" }
                        repeat(length) { output += output[output.size - distance] }
                    }
                    else -> {
                        output += ' '.code.toByte()
                        output += (command xor 0x80).toByte()
                    }
                }
            }
            return output.toByteArray()
        }

        private fun decodeText(bytes: ByteArray): String {
            val utf8 = runCatching { bytes.toString(Charsets.UTF_8) }.getOrDefault("")
            return if (utf8.contains('\uFFFD')) bytes.toString(Charset.forName("windows-1252")) else utf8
        }

        private fun extractFullName(record0: ByteArray, mobiHeader: Int): String {
            if (mobiHeader < 0 || mobiHeader + 0x5c > record0.size) return ""
            val offset = readU32(record0, mobiHeader + 0x54).toInt()
            val length = readU32(record0, mobiHeader + 0x58).toInt()
            return if (offset in record0.indices && length > 0 && offset + length <= record0.size) {
                decodeText(record0.copyOfRange(offset, offset + length)).trim('\u0000', ' ', '\t', '\r', '\n')
            } else ""
        }

        private fun extractExthAuthor(record0: ByteArray, mobiHeader: Int): String? {
            if (mobiHeader < 0 || mobiHeader + 0x18 > record0.size) return null
            val mobiHeaderLength = readU32(record0, mobiHeader + 4).toInt()
            val exth = mobiHeader + mobiHeaderLength
            if (exth < 0 || exth + 12 > record0.size) return null
            if (!record0.copyOfRange(exth, exth + 4).contentEquals("EXTH".toByteArray(Charsets.US_ASCII))) return null
            val count = readU32(record0, exth + 8).toInt()
            var cursor = exth + 12
            repeat(count) {
                if (cursor + 8 > record0.size) return@repeat
                val type = readU32(record0, cursor).toInt()
                val length = readU32(record0, cursor + 4).toInt()
                if (length < 8 || cursor + length > record0.size) return@repeat
                if (type == 100 && length > 8) return decodeText(record0.copyOfRange(cursor + 8, cursor + length)).trim().ifBlank { null }
                cursor += length
            }
            return null
        }

        private fun ByteArray.indexOfSubsequence(needle: ByteArray): Int {
            if (needle.isEmpty() || needle.size > size) return -1
            for (start in 0..size - needle.size) {
                if (needle.indices.all { this[start + it] == needle[it] }) return start
            }
            return -1
        }

        private fun readAscii(bytes: ByteArray, offset: Int, length: Int): String =
            bytes.copyOfRange(offset, minOf(offset + length, bytes.size))
                .toString(Charsets.US_ASCII).trim('\u0000', ' ', '\t', '\r', '\n')

        private fun readU16(bytes: ByteArray, offset: Int): Int =
            ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

        private fun readU32(bytes: ByteArray, offset: Int): Long =
            ((bytes[offset].toLong() and 0xff) shl 24) or
                ((bytes[offset + 1].toLong() and 0xff) shl 16) or
                ((bytes[offset + 2].toLong() and 0xff) shl 8) or
                (bytes[offset + 3].toLong() and 0xff)
    }
}
