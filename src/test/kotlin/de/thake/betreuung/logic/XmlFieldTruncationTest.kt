package de.thake.betreuung.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class XmlFieldTruncationTest {

    @Test
    fun testTruncateFits() {
        assertEquals("Hello", XmlGenerator.truncateToLength("Hello", 10))
    }

    @Test
    fun testTruncateWords() {
        // "Hello World" length 11. Max 10.
        // "Hello " (6) + "World" (5) = 11 > 10.
        // Should return "Hello"
        assertEquals("Hello", XmlGenerator.truncateToLength("Hello World", 10))
    }

    @Test
    fun testTruncateWordsMultiple() {
        // "One Two Three" -> "One Two"
        assertEquals("One Two", XmlGenerator.truncateToLength("One Two Three", 8))
    }

    @Test
    fun testTruncateExact() {
        assertEquals("One Two", XmlGenerator.truncateToLength("One Two", 7))
    }

    @Test
    fun testTruncateFallback() {
        // First word too long
        // "Averylongword" (13) -> Max 5 -> "Avery"
        assertEquals("Avery", XmlGenerator.truncateToLength("Averylongword", 5))
    }

    @Test
    fun testEmpty() {
        assertEquals("", XmlGenerator.truncateToLength("", 10))
    }
}
