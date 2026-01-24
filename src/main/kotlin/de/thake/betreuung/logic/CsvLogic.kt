package de.thake.betreuung.logic

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Locale
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.mozilla.universalchardet.UniversalDetector

object CsvLogic {

    fun detectCharset(file: File): Charset {
        val buf = ByteArray(4096)
        val fis = java.io.FileInputStream(file)
        val detector = UniversalDetector(null)

        try {
            var nread: Int
            while (fis.read(buf).also { nread = it } > 0 && !detector.isDone) {
                detector.handleData(buf, 0, nread)
            }
            detector.dataEnd()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fis.close()
        }

        val encoding = detector.detectedCharset
        detector.reset()

        return if (encoding != null) {
            try {
                Charset.forName(encoding)
            } catch (e: Exception) {
                StandardCharsets.ISO_8859_1
            }
        } else {
            // Default fallback. German CSVs are often ISO-8859-1 or Windows-1252
            StandardCharsets.ISO_8859_1
        }
    }

    private fun determineDelimiter(line: String): Char {
        return if (line.count { it == ';' } >= line.count { it == ',' }) ';' else ','
    }

    private data class FormatResult(val format: CSVFormat, val skipLines: Int)

    private fun detectFormatAndStartLine(file: File, safeCharset: Charset? = null): FormatResult {
        // Detect Charset (if not provided)
        val charset = safeCharset ?: detectCharset(file)

        // Read first 20 lines to find the one with most delimiters or columns
        var maxDelimiters = 0
        var bestLineIndex = 0
        var delimiter = ';'

        file.useLines(charset) { lines ->
            lines.take(20).forEachIndexed { index, line ->
                if (line.isBlank()) return@forEachIndexed

                val semiCount = line.count { it == ';' }
                val commaCount = line.count { it == ',' }
                val currentMax = maxOf(semiCount, commaCount)
                val currentDelimiter = if (semiCount >= commaCount) ';' else ','

                // Simple Heuristic: The header row usually has significant columns.
                // We pick the first line that has "many" columns, or the one with the maximum if
                // they vary.
                if (currentMax > maxDelimiters) {
                    maxDelimiters = currentMax
                    bestLineIndex = index
                    delimiter = currentDelimiter
                }
            }
        }

        // If no delimiters found, default to 0 skip
        if (maxDelimiters == 0)
                return FormatResult(
                        CSVFormat.DEFAULT.builder().setIgnoreHeaderCase(true).setTrim(true).build(),
                        0
                )

        // Construct format
        val format =
                CSVFormat.DEFAULT
                        .builder()
                        .setDelimiter(delimiter)
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build()

        return FormatResult(format, bestLineIndex)
    }

    fun readHeaders(file: File): List<String> {
        val charset = detectCharset(file)
        val (format, skipLines) = detectFormatAndStartLine(file, charset)

        val reader = file.bufferedReader(charset)
        repeat(skipLines) { reader.readLine() }

        val parser = CSVParser(reader, format)
        return parser.use { it.headerNames }
    }

    fun readRows(file: File): List<Map<String, String>> {
        val charset = detectCharset(file)
        val (format, skipLines) = detectFormatAndStartLine(file, charset)

        val reader = file.bufferedReader(charset)
        repeat(skipLines) { reader.readLine() }

        val parser = CSVParser(reader, format)
        return parser.use { p -> p.records.map { record -> record.toMap() } }
    }

    fun parseAmount(value: String): Double {
        if (value.isBlank()) return 0.0
        // Clean up currency symbols and spaces. Keep minus.
        // Remove currency symbols like € but keep digits, commas, dots, minus.
        // Also remove non-breaking spaces if any?
        val cleanValue = value.replace(Regex("[^0-9,.-]"), "")

        try {
            // Try German format first (1.000,00)
            val formatDE = NumberFormat.getInstance(Locale.GERMANY)
            return formatDE.parse(cleanValue).toDouble()
        } catch (e: Exception) {
            try {
                // Try US format (1,000.00)
                val formatUS = NumberFormat.getInstance(Locale.US)
                return formatUS.parse(cleanValue).toDouble()
            } catch (e2: Exception) {
                return 0.0 // Fallback
            }
        }
    }
}
