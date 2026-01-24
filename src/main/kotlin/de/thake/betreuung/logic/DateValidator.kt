package de.thake.betreuung.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

object DateValidator {
    private val logger = LoggerFactory.getLogger(DateValidator::class.java)

    data class ValidationResult(
            val isValid: Boolean,
            val invalidTransactions: List<MappedTransaction> = emptyList(),
            val errorMessage: String? = null
    )

    fun validateTransactions(
            transactions: List<MappedTransaction>,
            periodStart: String,
            periodEnd: String
    ): ValidationResult {
        val formatters =
                listOf(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                        DateTimeFormatter.ofPattern("dd.MM.yy")
                )
        val pStart: LocalDate
        val pEnd: LocalDate

        try {
            // Period is always expected in full format
            pStart = LocalDate.parse(periodStart, formatters[0])
            pEnd = LocalDate.parse(periodEnd, formatters[0])
        } catch (e: Exception) {
            return ValidationResult(
                    false,
                    errorMessage = "Ungültiges Datumsformat im Zeitraum (Erwartet: dd.MM.yyyy)."
            )
        }

        val invalidTransactions =
                transactions.filter { tx ->
                    var d: LocalDate? = null
                    for (fmt in formatters) {
                        try {
                            d = LocalDate.parse(tx.date, fmt)
                            break
                        } catch (e: Exception) {
                            // continue
                        }
                    }
                    if (d != null) {
                        d.isBefore(pStart) || d.isAfter(pEnd)
                    } else {
                        true // Invalid format
                    }
                }

        if (invalidTransactions.isNotEmpty()) {
            val firstThree = invalidTransactions.take(3).joinToString(", ") { it.date }
            return ValidationResult(
                    isValid = false,
                    invalidTransactions = invalidTransactions,
                    errorMessage =
                            "Es gibt ${invalidTransactions.size} Buchungen außerhalb des Zeitraums ($periodStart - $periodEnd).\nBsp: $firstThree..."
            )
        }

        return ValidationResult(true)
    }
}
