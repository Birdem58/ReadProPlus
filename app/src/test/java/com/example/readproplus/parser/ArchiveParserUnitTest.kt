package com.example.readproplus.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveParserUnitTest {

    @Test
    fun epubResolvesPackageSpineAndNcx() {
        val document = EpubParser.parseArchive(
            archive(zip(
                "META-INF/container.xml" to """
                    <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                      <rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles>
                    </container>
                """.trimIndent(),
                "OEBPS/content.opf" to """
                    <package xmlns="http://www.idpf.org/2007/opf" version="2.0">
                      <metadata><dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">Test EPUB</dc:title>
                        <dc:creator xmlns:dc="http://purl.org/dc/elements/1.1/">A Writer</dc:creator></metadata>
                      <manifest>
                        <item id="one" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                        <item id="two" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
                        <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                      </manifest>
                      <spine toc="ncx"><itemref idref="one"/><itemref idref="two"/></spine>
                    </package>
                """.trimIndent(),
                "OEBPS/chapter1.xhtml" to "<html><body><h1>First</h1><p>Hello EPUB</p></body></html>",
                "OEBPS/chapter2.xhtml" to "<html><body><h1>Second</h1><p>More text</p></body></html>",
                "OEBPS/toc.ncx" to """
                    <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/">
                      <navMap><navPoint><navLabel><text>First chapter</text></navLabel><content src="chapter1.xhtml"/></navPoint>
                        <navPoint><navLabel><text>Second chapter</text></navLabel><content src="chapter2.xhtml"/></navPoint></navMap>
                    </ncx>
                """.trimIndent(),
            )),
            fallbackTitle = "fallback",
            sourceUri = "content://test/book.epub",
        )

        assertEquals("Test EPUB", document.title)
        assertEquals("A Writer", document.author)
        assertEquals(2, document.pages.size)
        assertTrue(document.pages[0].contains("Hello EPUB"))
        assertEquals(2, document.toc.size)
        assertEquals(2, document.toc[1].pageNumber)
    }

    @Test
    fun fb2AndOdtExtractStructuredText() {
        val fb2 = Fb2Parser.parseXmlDocument(
            """
                <FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0">
                  <description><title-info><book-title>FB Title</book-title>
                    <author><first-name>Jane</first-name><last-name>Doe</last-name></author></title-info></description>
                  <body><section><title><p>Chapter One</p></title><p>FB2 paragraph</p></section></body>
                </FictionBook>
            """.trimIndent().toByteArray(),
            fallbackTitle = "fallback",
            sourceUri = "content://test/book.fb2",
        )
        val odt = OdtParser.parseContent(
            """
                <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                  <office:body><office:text><text:h>Heading</text:h><text:p>Hello <text:s c="1"/>ODT</text:p></office:text></office:body>
                </office:document-content>
            """.trimIndent().toByteArray(),
            title = "ODT Title",
            sourceUri = "content://test/book.odt",
        )

        assertEquals("FB Title", fb2.title)
        assertEquals("Jane Doe", fb2.author)
        assertTrue(fb2.pages.single().contains("FB2 paragraph"))
        assertTrue(odt.pages.single().contains("Hello ODT"))
    }

    @Test
    fun docxAndMobiHelpersReadRealPayloads() {
        val docx = DocDocxParser.parseDocx(
            archive(zip("word/document.xml" to """
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body><w:p><w:r><w:t>Hello DOCX</w:t></w:r></w:p></w:body>
                </w:document>
            """.trimIndent()))["word/document.xml"]!!,
            title = "DOCX",
            sourceUri = "content://test/book.docx",
        )
        val mobiText = MobiAzw3Parser.decompressPalmDoc(
            byteArrayOf(3, 'a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()),
        )
        val parsedMobi = MobiAzw3Parser.parsePalmDatabase(syntheticMobi())

        assertTrue(docx.pages.single().contains("Hello DOCX"))
        assertEquals("abc", mobiText.toString(Charsets.US_ASCII))
        assertEquals("Synthetic title", parsedMobi.title)
        assertTrue(parsedMobi.html.contains("Hello MOBI"))
    }

    @Test
    fun comicImageOrderIsNaturalAndCaseInsensitive() {
        assertTrue(CbzCbrParser.isImageEntry("Pages/PAGE02.PNG"))
        val sorted = listOf("page10.jpg", "page2.jpg", "page1.jpg").sortedWith(naturalPathComparator)
        assertEquals(listOf("page1.jpg", "page2.jpg", "page10.jpg"), sorted)
    }

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, contents) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun archive(bytes: ByteArray): Map<String, ByteArray> =
        readZipEntries(ByteArrayInputStream(bytes))

    private fun syntheticMobi(): ByteArray {
        val record0 = ByteArray(256)
        record0[1] = 1 // no compression
        record0[8] = 0
        record0[9] = 1 // one text record
        record0[10] = 0x10
        record0[11] = 0x00
        "MOBI".toByteArray(Charsets.US_ASCII).copyInto(record0, 16)
        writeU32(record0, 20, 0xE8)
        writeU32(record0, 16 + 0x54, 0xF0)
        writeU32(record0, 16 + 0x58, 15)
        "Synthetic title".toByteArray(Charsets.US_ASCII).copyInto(record0, 0xF0)
        val textRecord = "<p>Hello MOBI</p>".toByteArray()
        val result = ByteArray(78 + 16 + record0.size + textRecord.size)
        writeU16(result, 76, 2)
        writeU32(result, 78, 94)
        writeU32(result, 86, 94 + record0.size)
        record0.copyInto(result, 94)
        textRecord.copyInto(result, 94 + record0.size)
        return result
    }

    private fun writeU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 8).toByte()
        bytes[offset + 1] = value.toByte()
    }

    private fun writeU32(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 24).toByte()
        bytes[offset + 1] = (value ushr 16).toByte()
        bytes[offset + 2] = (value ushr 8).toByte()
        bytes[offset + 3] = value.toByte()
    }
}
