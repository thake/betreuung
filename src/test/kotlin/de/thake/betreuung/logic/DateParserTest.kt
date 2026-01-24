package de.thake.betreuung.logic

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DateParserTest {

    @Test
    fun `parse handles standard German format`() {
        assertEquals(LocalDate.of(2024, 1, 1), DateParser.parse("01.01.2024"))
        assertEquals(LocalDate.of(2024, 1, 31), DateParser.parse("31.01.2024"))
        assertEquals(LocalDate.of(2024, 12, 31), DateParser.parse("31.12.2024"))
    }

    @Test
    fun `parse handles single digit German format`() {
        assertEquals(LocalDate.of(2024, 1, 1), DateParser.parse("1.1.2024"))
        assertEquals(LocalDate.of(2024, 2, 5), DateParser.parse("5.2.2024"))
    }

    @Test
    fun `parse handles short year format`() {
        assertEquals(LocalDate.of(2024, 1, 1), DateParser.parse("01.01.24"))
        assertEquals(LocalDate.of(2024, 1, 1), DateParser.parse("1.1.24"))
    }

    @Test
    fun `parse handles ISO format`() {
        assertEquals(LocalDate.of(2024, 1, 1), DateParser.parse("2024-01-01"))
    }

    @Test
    fun `parse handles long German format`() {
        assertEquals(LocalDate.of(2026, 1, 1), DateParser.parse("1. Januar 2026"))
        assertEquals(LocalDate.of(2026, 3, 15), DateParser.parse("15. März 2026"))
        assertEquals(LocalDate.of(2026, 3, 15), DateParser.parse("15. Maerz 2026"))
        assertEquals(LocalDate.of(2026, 12, 24), DateParser.parse("24. Dezember 2026"))

        // Month short names
        assertEquals(LocalDate.of(2026, 1, 1), DateParser.parse("1. Jan 2026"))
        assertEquals(LocalDate.of(2026, 2, 1), DateParser.parse("1. Feb 2026"))
    }

    @Test
    fun `parse returns null for invalid dates`() {
        assertNull(DateParser.parse(null))
        assertNull(DateParser.parse(""))
        assertNull(DateParser.parse("invalid"))
        assertNull(DateParser.parse("32.01.2024")) // Invalid day
        assertNull(DateParser.parse("01.13.2024")) // Invalid month
    }
}
