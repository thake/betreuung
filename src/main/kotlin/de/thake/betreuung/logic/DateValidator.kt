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
        val pStart: LocalDate
        val pEnd: LocalDate

        try {
            // Use DateParser to be robust with UI inputs too, though UI usually enforces dd.MM.yyyy
            pStart =
                    DateParser.parse(periodStart)
                            ?: throw IllegalArgumentException("Invalid start date")
            pEnd = DateParser.parse(periodEnd) ?: throw IllegalArgumentException("Invalid end date")
        } catch (e: Exception) {
            return ValidationResult(false, errorMessage = "Ungültiges Datumsformat im Zeitraum.")
        }

        val invalidTransactions =
                transactions.filter { tx ->
                    // tx.date is now LocalDate, so we just compare
                    tx.date.isBefore(pStart) || tx.date.isAfter(pEnd)
                }

        if (invalidTransactions.isNotEmpty()) {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val firstThree =
                    invalidTransactions.take(3).joinToString(", ") { it.date.format(formatter) }
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
