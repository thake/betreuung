package de.thake.betreuung.logic

import de.thake.betreuung.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TransactionMapperTest {

        @Test
        fun testSeparateColumns() {
                val row =
                        mapOf(
                                "Date" to "01.01.2024",
                                "Text" to "Shop",
                                "Debit" to "10,50",
                                "Credit" to ""
                        )
                val mapping =
                        mapOf(
                                XmlFields.DATE to "Date",
                                XmlFields.PAYEE to "Text",
                                XmlFields.EXPENSE to "Debit",
                                XmlFields.INCOME to "Credit"
                        )

                val tx = TransactionMapper.mapRow(row, mapping, isCents = false)
                assertNotNull(tx)
                assertEquals(10.50, tx!!.amount)
                assertEquals(TransactionType.EXPENSE, tx.type)
                assertEquals(java.time.LocalDate.of(2024, 1, 1), tx.date)
        }

        @Test
        fun testSingleSignedColumnPositive() {
                val row = mapOf("Date" to "01.01.2024", "Text" to "Salary", "Amount" to "1500,00")
                // User maps BOTH to "Amount"
                val mapping =
                        mapOf(
                                XmlFields.DATE to "Date",
                                XmlFields.PAYEE to "Text",
                                XmlFields.EXPENSE to "Amount",
                                XmlFields.INCOME to "Amount"
                        )

                val tx = TransactionMapper.mapRow(row, mapping, isCents = false)
                assertNotNull(tx)
                assertEquals(1500.00, tx!!.amount)
                assertEquals(TransactionType.INCOME, tx.type)
                assertEquals(java.time.LocalDate.of(2024, 1, 1), tx.date)
        }

        @Test
        fun testSingleSignedColumnNegative() {
                val row = mapOf("Date" to "01.01.2024", "Text" to "Rent", "Amount" to "-800,00")
                // User maps BOTH to "Amount"
                val mapping =
                        mapOf(
                                XmlFields.DATE to "Date",
                                XmlFields.PAYEE to "Text",
                                XmlFields.EXPENSE to "Amount",
                                XmlFields.INCOME to "Amount"
                        )

                val tx = TransactionMapper.mapRow(row, mapping, isCents = false)
                assertNotNull(tx)
                assertEquals(800.00, tx!!.amount) // Should be positive absolute amount
                assertEquals(TransactionType.EXPENSE, tx.type)
                assertEquals(java.time.LocalDate.of(2024, 1, 1), tx.date)
        }

        @Test
        fun testCentsConversion() {
                // Case: 534 cents -> 5.34 EUR
                val row = mapOf("Date" to "01.01.2024", "Text" to "Shop", "Debit" to "534")
                val mapping =
                        mapOf(
                                XmlFields.DATE to "Date",
                                XmlFields.PAYEE to "Text",
                                XmlFields.EXPENSE to "Debit"
                        )

                val tx = TransactionMapper.mapRow(row, mapping, isCents = true)
                assertNotNull(tx)
                // 534 / 100 = 5.34
                assertEquals(5.34, tx!!.amount, 0.0001)
        }

        @Test
        fun testCentsDetectLogic() {
                // Helper test for logic
                val centsRows = listOf("100", "534", "1234")
                assertTrue(CsvLogic.detectCurrencyIsCents(centsRows))

                val euroRows = listOf("100,00", "5,34")
                assertFalse(CsvLogic.detectCurrencyIsCents(euroRows))

                val mixedRows = listOf("100", "5,34")
                assertFalse(CsvLogic.detectCurrencyIsCents(mixedRows)) // Has separator -> Euro

                val empty = emptyList<String>()
                assertFalse(CsvLogic.detectCurrencyIsCents(empty))

                val blank = listOf("", " ")
                assertFalse(CsvLogic.detectCurrencyIsCents(blank))

                val validAndBlank = listOf("123", "")
                assertTrue(CsvLogic.detectCurrencyIsCents(validAndBlank))
        }
}
