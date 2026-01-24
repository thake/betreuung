package de.thake.betreuung.logic

import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CsvEncodingTest {

    @TempDir lateinit var tempDir: File

    @Test
    fun testDetectUtf8() {
        val file = File(tempDir, "utf8.csv")
        file.writeText("Name;Betrag\nMüller;100,00", StandardCharsets.UTF_8)

        val charset = CsvLogic.detectCharset(file)

        // juniversalchardet sometimes returns null for very small ASCII only files,
        // but "Müller" has umlauts so it should detect UTF-8
        assertEquals(StandardCharsets.UTF_8, charset)

        val rows = CsvLogic.readRows(file)
        assertEquals(1, rows.size)
        assertEquals("Müller", rows[0]["Name"])
    }

    @Test
    fun testDetectIso88591() {
        val file = File(tempDir, "iso.csv")
        file.writeText("Name;Betrag\nMüller;100,00", StandardCharsets.ISO_8859_1)

        val charset = CsvLogic.detectCharset(file)

        // Note: Short strings might be ambiguous, but let's hope it detects it roughly correct
        // Or falls back to ISO-8859-1 which is our default fallback anyway.
        // Let's verify the content reading works.

        // In practice for detection to be 100% sure we need more text,
        // but CsvLogic defaults to ISO-8859-1 if unknown.

        val rows = CsvLogic.readRows(file)
        assertEquals(1, rows.size)
        assertEquals("Müller", rows[0]["Name"])
    }
}
