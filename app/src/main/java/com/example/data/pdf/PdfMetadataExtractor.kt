package com.example.data.pdf

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

data class ExtractedPdfMetadata(
    val title: String,
    val author: String
)

/**
 * Extracts embedded PDF metadata (/Title and /Author from the PDF Info dictionary
 * or XMP metadata stream) without requiring external libraries, with clean filename fallback.
 */
object PdfMetadataExtractor {

    fun extract(file: File, fallbackFileName: String): ExtractedPdfMetadata {
        val rawSnippet = readHeadAndTailSnippet(file)
        return extractFromText(rawSnippet, fallbackFileName)
    }

    fun extractFromBytes(bytes: ByteArray, fallbackFileName: String): ExtractedPdfMetadata {
        val snippet = if (bytes.size <= 262_144) {
            String(bytes, StandardCharsets.ISO_8859_1)
        } else {
            val head = String(bytes, 0, 131_072, StandardCharsets.ISO_8859_1)
            val tail = String(bytes, bytes.size - 131_072, 131_072, StandardCharsets.ISO_8859_1)
            head + "\n" + tail
        }
        return extractFromText(snippet, fallbackFileName)
    }

    internal fun extractFromText(content: String, fallbackFileName: String): ExtractedPdfMetadata {
        val title = parseInfoField(content, "Title")
            ?: parseXmpTitle(content)
            ?: cleanFileName(fallbackFileName)

        val author = parseInfoField(content, "Author")
            ?: parseXmpAuthor(content)
            ?: "Unknown Author"

        return ExtractedPdfMetadata(
            title = title.trim().take(200).ifBlank { "Untitled Book" },
            author = author.trim().take(120).ifBlank { "Unknown Author" }
        )
    }

    private fun readHeadAndTailSnippet(file: File): String {
        if (!file.exists() || file.length() == 0L) return ""
        val maxRead = 131_072
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val len = raf.length()
                if (len <= maxRead * 2L) {
                    val buf = ByteArray(len.toInt())
                    raf.readFully(buf)
                    String(buf, StandardCharsets.ISO_8859_1)
                } else {
                    val head = ByteArray(maxRead)
                    raf.readFully(head)
                    raf.seek(len - maxRead)
                    val tail = ByteArray(maxRead)
                    raf.readFully(tail)
                    String(head, StandardCharsets.ISO_8859_1) + "\n" + String(tail, StandardCharsets.ISO_8859_1)
                }
            }
        }.getOrDefault("")
    }

    private fun parseInfoField(content: String, key: String): String? {
        // Match PDF literal string: /Title (Some Title)
        val literalRegex = Regex("""/$key\s*\(([^()\\]*(?:\\.[^()\\]*)*)\)""")
        literalRegex.findAll(content).lastOrNull()?.let { match ->
            val decoded = decodePdfLiteralString(match.groupValues[1])
            if (decoded.isNotBlank() && !looksLikeSoftwareTool(decoded)) {
                return decoded
            }
        }

        // Match PDF hex string: /Title <FEFF0041...>
        val hexRegex = Regex("""/$key\s*<([0-9A-Fa-f\s]+)>""")
        hexRegex.findAll(content).lastOrNull()?.let { match ->
            val decoded = decodePdfHexString(match.groupValues[1])
            if (decoded.isNotBlank() && !looksLikeSoftwareTool(decoded)) {
                return decoded
            }
        }
        return null
    }

    private fun parseXmpTitle(content: String): String? {
        val regex = Regex("""<dc:title[^>]*>.*?<rdf:li[^>]*>(.*?)</rdf:li>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        return regex.find(content)?.groupValues?.getOrNull(1)?.let(::unescapeXml)?.takeIf { it.isNotBlank() }
    }

    private fun parseXmpAuthor(content: String): String? {
        val regex = Regex("""<dc:creator[^>]*>.*?<rdf:li[^>]*>(.*?)</rdf:li>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        return regex.find(content)?.groupValues?.getOrNull(1)?.let(::unescapeXml)?.takeIf { it.isNotBlank() }
    }

    private fun decodePdfLiteralString(raw: String): String {
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (ch == '\\' && i + 1 < raw.length) {
                val next = raw[i + 1]
                when (next) {
                    'n' -> { bytes.add('\n'.code.toByte()); i += 2 }
                    'r' -> { bytes.add('\r'.code.toByte()); i += 2 }
                    't' -> { bytes.add('\t'.code.toByte()); i += 2 }
                    'b' -> { bytes.add('\b'.code.toByte()); i += 2 }
                    '(', ')', '\\' -> { bytes.add(next.code.toByte()); i += 2 }
                    in '0'..'7' -> {
                        val oct = raw.substring(i + 1).take(3).takeWhile { it in '0'..'7' }
                        bytes.add(oct.toInt(8).toByte())
                        i += 1 + oct.length
                    }
                    else -> { bytes.add(next.code.toByte()); i += 2 }
                }
            } else {
                bytes.add((ch.code and 0xFF).toByte())
                i++
            }
        }
        return decodePdfByteBuffer(bytes.toByteArray())
    }

    private fun decodePdfHexString(hexRaw: String): String {
        val clean = hexRaw.filter { !it.isWhitespace() }
        if (clean.length < 2) return ""
        val padded = if (clean.length % 2 == 1) "${clean}0" else clean
        val bytes = ByteArray(padded.length / 2) { idx ->
            padded.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
        return decodePdfByteBuffer(bytes)
    }

    private fun decodePdfByteBuffer(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val text = if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        } else if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        } else {
            String(bytes, StandardCharsets.UTF_8)
        }
        return text.replace("\u0000", "").trim()
    }

    private fun unescapeXml(raw: String): String =
        raw.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .trim()

    private fun looksLikeSoftwareTool(value: String): Boolean {
        val lower = value.lowercase()
        return lower.startsWith("microsoft word") || lower.endsWith(".docx") || lower == "untitled"
    }

    fun cleanFileName(fileName: String): String {
        val base = fileName.substringAfterLast('/')
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        return base.ifBlank { "Untitled Book" }
    }
}
