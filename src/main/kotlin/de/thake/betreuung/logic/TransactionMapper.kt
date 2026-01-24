package de.thake.betreuung.logic

import de.thake.betreuung.model.XmlFields
import kotlin.math.abs

object TransactionMapper {
    fun mapRow(row: Map<String, String>, mapping: Map<String, String>): MappedTransaction? {
        val dateCol = mapping[XmlFields.DATE]
        val dateStr = if (dateCol != null) row[dateCol] else null

        if (dateStr.isNullOrBlank()) return null
        val date = DateParser.parse(dateStr) ?: return null

        val payee = row[mapping[XmlFields.PAYEE] ?: ""] ?: ""
        val purpose = row[mapping[XmlFields.PURPOSE] ?: ""] ?: ""

        val expenseCol = mapping[XmlFields.EXPENSE]
        val incomeCol = mapping[XmlFields.INCOME]

        // Check if Single Column Signed Amount Mode
        if (expenseCol != null && incomeCol != null && expenseCol == incomeCol) {
            val amountStr = row[expenseCol] ?: ""
            val amount = CsvLogic.parseAmount(amountStr)

            return if (amount > 0) {
                MappedTransaction(date, payee, purpose, amount, TransactionType.INCOME)
            } else if (amount < 0) {
                MappedTransaction(date, payee, purpose, abs(amount), TransactionType.EXPENSE)
            } else {
                null
            }
        }

        // Separate Columns Mode
        val expenseVal =
                if (expenseCol != null) CsvLogic.parseAmount(row[expenseCol] ?: "") else 0.0
        val incomeVal = if (incomeCol != null) CsvLogic.parseAmount(row[incomeCol] ?: "") else 0.0

        return if (expenseVal > 0) {
            MappedTransaction(date, payee, purpose, expenseVal, TransactionType.EXPENSE)
        } else if (incomeVal > 0) {
            MappedTransaction(date, payee, purpose, incomeVal, TransactionType.INCOME)
        } else {
            null
        }
    }
}
