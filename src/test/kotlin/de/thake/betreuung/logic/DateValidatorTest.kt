package de.thake.betreuung.logic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DateValidatorTest {

    private fun createTx(date: String): MappedTransaction {
        return MappedTransaction(
                date = date,
                payee = "Test",
                purpose = "Test",
                amount = 10.0,
                type = TransactionType.INCOME
        )
    }

    @Test
    fun `validateTransactions returns valid for correct date range`() {
        val transactions =
                listOf(createTx("02.01.2024"), createTx("15.06.2024"), createTx("30.12.2024"))
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertTrue(result.isValid)
        assertTrue(result.invalidTransactions.isEmpty())
    }

    @Test
    fun `validateTransactions returns valid for correct date range in other format`() {
        val transactions = listOf(createTx("02.01.24"), createTx("15.06.24"), createTx("30.12.24"))
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertTrue(result.isValid)
        assertTrue(result.invalidTransactions.isEmpty())
    }

    @Test
    fun `validateTransactions returns invalid for date before range`() {
        val transactions = listOf(createTx("31.12.2023"), createTx("01.01.2024"))
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertFalse(result.isValid)
        assertEquals(1, result.invalidTransactions.size)
        assertEquals("31.12.2023", result.invalidTransactions[0].date)
        assertTrue(result.errorMessage!!.contains("31.12.2023"))
    }

    @Test
    fun `validateTransactions returns invalid for date after range`() {
        val transactions = listOf(createTx("01.01.2025"), createTx("01.01.2024"))
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertFalse(result.isValid)
        assertEquals(1, result.invalidTransactions.size)
        assertEquals("01.01.2025", result.invalidTransactions[0].date)
    }

    @Test
    fun `validateTransactions handles invalid date format in period`() {
        val transactions = listOf(createTx("01.01.2024"))
        val result = DateValidator.validateTransactions(transactions, "invalid", "31.12.2024")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("Ungültiges Datumsformat"))
    }

    @Test
    fun `validateTransactions handles invalid date format in transaction`() {
        val transactions = listOf(createTx("invalid-date"))
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertFalse(result.isValid)
        assertEquals(1, result.invalidTransactions.size)
    }

    @Test
    fun `validateTransactions with empty list is valid`() {
        val transactions = emptyList<MappedTransaction>()
        val result = DateValidator.validateTransactions(transactions, "01.01.2024", "31.12.2024")
        assertTrue(result.isValid)
    }
}
