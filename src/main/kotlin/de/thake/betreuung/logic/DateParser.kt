package de.thake.betreuung.logic

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateParser {
    private val logger = KotlinLogging.logger {}

    private val germanMonthNames =
            mapOf(
                    "januar" to 1,
                    "jan" to 1,
                    "februar" to 2,
                    "feb" to 2,
                    "märz" to 3,
                    "mrz" to 3,
                    "maerz" to 3,
                    "april" to 4,
                    "apr" to 4,
                    "mai" to 5,
                    "juni" to 6,
                    "jun" to 6,
                    "juli" to 7,
                    "jul" to 7,
                    "august" to 8,
                    "aug" to 8,
                    "september" to 9,
                    "sep" to 9,
                    "oktober" to 10,
                    "okt" to 10,
                    "november" to 11,
                    "nov" to 11,
                    "dezember" to 12,
                    "dez" to 12
            )

    private val formatters =
            listOf(
                    // Standard German
                    DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                    DateTimeFormatter.ofPattern("d.M.yyyy"),
                    // Short Year German
                    DateTimeFormatter.ofPattern("dd.MM.yy"),
                    DateTimeFormatter.ofPattern("d.M.yy"),
                    // ISO
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    // Dots but ISO-like (sometimes happens)
                    DateTimeFormatter.ofPattern("yyyy.MM.dd")
            )

    fun parse(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null

        val trimmed = dateStr.trim()

        // 1. Try standard formatters
        for (fmt in formatters) {
            try {
                return LocalDate.parse(trimmed, fmt)
            } catch (e: Exception) {
                logger.trace(e) { "Failed to parse '$trimmed' with formatter $fmt" }
            }
        }

        // 2. Try parsing "Long German" manually (e.g. "1. Januar 2026")
        // Regex for: digits + dot + space + word + space + digits
        // or: digits + space + word + space + digits (some variants)
        val match = Regex("""(\d{1,2})\.?\s+([a-zA-ZäöüÄÖÜß]+)\s+(\d{2,4})""").find(trimmed)
        if (match != null) {
            val (dayStr, monthStr, yearStr) = match.destructured
            val month = germanMonthNames[monthStr.lowercase()]
            if (month != null) {
                try {
                    val day = dayStr.toInt()
                    var year = yearStr.toInt()
                    // Handle 2 digit year for long format? Unlikely but possible "1. Jan 24"
                    if (year < 100) {
                        year += 2000 // Assumption for 21st century
                    }
                    return LocalDate.of(year, month, day)
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to parse '$trimmed' with regex fallback" }
                }
            }
        }

        return null
    }
}
