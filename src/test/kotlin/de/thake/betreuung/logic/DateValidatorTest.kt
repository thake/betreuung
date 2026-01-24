package de.thake.betreuung.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DateValidatorTest {

    private fun createTx(date: java.time.LocalDate): MappedTransaction {
        return MappedTransaction(
                date = date,
                payee = "Test",
                purpose = "Test",
                amount = 10.0,
                type = TransactionType.INCOME
        )
    }

    // Helper to parse for test readability
    private fun d(s: String): java.time.LocalDate = DateParser.parse(s)!!

    @Test
    fun `validateTransactions returns valid for correct date range`() {
        val transactions =
                listOf(
                        createTx(d("02.01.2024")),
                        createTx(d("15.06.2024")),
                        createTx(d("30.12.2024"))
                )
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertTrue(result.isValid)
        assertTrue(result.invalidTransactions.isEmpty())
    }

    @Test
    fun `validateTransactions returns valid for correct date range in other format`() {
        // Validation now happens on LocalDate objects, so the input format to createTx doesn't
        // matter as much
        // as long as it parses. The "other format" aspect is mainly about the PERIOD strings now,
        // or confirming DateParser works (which is tested separately).
        // But we can still test that transactions created from short dates are valid against long
        // period strings.
        val transactions =
                listOf(createTx(d("02.01.24")), createTx(d("15.06.24")), createTx(d("30.12.24")))
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertTrue(result.isValid)
        assertTrue(result.invalidTransactions.isEmpty())
    }

    @Test
    fun `validateTransactions returns invalid for date before range`() {
        val transactions = listOf(createTx(d("31.12.2023")), createTx(d("01.01.2024")))
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertFalse(result.isValid)
        assertEquals(1, result.invalidTransactions.size)
        // Date in MappedTransaction is LocalDate, so comparisons work.
        // Note: The error message might contain formatted date now.
        assertEquals(d("31.12.2023"), result.invalidTransactions[0].date)
        assertTrue(result.errorMessage!!.contains("31.12.2023"))
    }

    @Test
    fun `validateTransactions returns invalid for date after range`() {
        val transactions = listOf(createTx(d("01.01.2025")), createTx(d("01.01.2024")))
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertFalse(result.isValid)
        assertEquals(1, result.invalidTransactions.size)
        assertEquals(d("01.01.2025"), result.invalidTransactions[0].date)
    }

    @Test
    fun `validateTransactions handles invalid date format in period`() {
        val transactions = listOf(createTx(d("01.01.2024")))
        val result = DateValidator.validateTransactions(transactions, "invalid", "31.12.2024")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("Ungültiges Datumsformat"))
    }

    // This test is less relevant now as MappedTransaction enforces LocalDate,
    // so we can't have an "invalid date format in transaction" anymore
    // UNLESS we mean the CSV mapping failed, but that returns null transaction.
    // We can remove or adapt it to test logic?
    // Actually, we can't create a MappedTransaction with invalid date.
    // So we remove `validateTransactions handles invalid date format in transaction`.

    @Test
    fun `validateTransactions with empty list is valid`() {
        val transactions = emptyList<MappedTransaction>()
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertTrue(result.isValid)
    }
}
