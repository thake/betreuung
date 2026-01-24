package de.thake.betreuung.logic

import de.thake.betreuung.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TransactionMapperTest {

    @Test
    fun testSeparateColumns() {
        val row =
                mapOf("Date" to "01.01.2024", "Text" to "Shop", "Debit" to "10,50", "Credit" to "")
        val mapping =
                mapOf(
                        XmlFields.DATE to "Date",
                        XmlFields.PAYEE to "Text",
                        XmlFields.EXPENSE to "Debit",
                        XmlFields.INCOME to "Credit"
                )

        val tx = TransactionMapper.mapRow(row, mapping)
        assertNotNull(tx)
        assertEquals(10.50, tx!!.amount)
        assertEquals(TransactionType.EXPENSE, tx.type)
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

        val tx = TransactionMapper.mapRow(row, mapping)
        assertNotNull(tx)
        assertEquals(1500.00, tx!!.amount)
        assertEquals(TransactionType.INCOME, tx.type)
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

        val tx = TransactionMapper.mapRow(row, mapping)
        assertNotNull(tx)
        assertEquals(800.00, tx!!.amount) // Should be positive absolute amount
        assertEquals(TransactionType.EXPENSE, tx.type)
    }
}
