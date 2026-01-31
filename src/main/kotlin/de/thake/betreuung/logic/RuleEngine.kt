package de.thake.betreuung.logic

import de.thake.betreuung.model.*
import io.github.oshai.kotlinlogging.KotlinLogging

object RuleEngine {
    private val logger = KotlinLogging.logger {}

    fun applyRules(
            transactions: List<MappedTransaction>,
            rules: List<ReplacementRule>,
            betreuter: Betreuter,
            activeMappingId: String? = null // To filter specific rules
    ): List<MappedTransaction> {
        // Filter rules that are either Global OR specific to this mapping
        val applicableRules =
                rules.filter { rule ->
                    rule.isActive && (rule.mappingId == null || rule.mappingId == activeMappingId)
                }

        if (applicableRules.isEmpty()) return transactions

        return transactions.map { transaction ->
            applyRulesToTransaction(transaction, applicableRules, betreuter)
        }
    }

    private fun applyRulesToTransaction(
            transaction: MappedTransaction,
            rules: List<ReplacementRule>,
            betreuter: Betreuter
    ): MappedTransaction {
        var currentTx = transaction

        for (rule in rules) {
            if (matches(currentTx, rule.condition)) {
                currentTx = applyAction(currentTx, rule.action, betreuter)
            }
        }
        return currentTx
    }

    private fun matches(tx: MappedTransaction, condition: ReplacementCondition): Boolean {
        val value = getValue(tx, condition.field)
        val criteria = condition.value

        return when (condition.type) {
            ReplacementConditionType.STARTS_WITH -> value.startsWith(criteria, ignoreCase = true)
            ReplacementConditionType.CONTAINS -> value.contains(criteria, ignoreCase = true)
            ReplacementConditionType.EQUALS -> value.equals(criteria, ignoreCase = true)
        }
    }

    private fun applyAction(
            tx: MappedTransaction,
            action: ReplacementAction,
            betreuter: Betreuter
    ): MappedTransaction {
        val newValue = replacePlaceholders(action.template, betreuter)
        return setValue(tx, action.targetField, newValue)
    }

    private fun replacePlaceholders(template: String, betreuter: Betreuter): String {
        return template.replace("{nachname}", betreuter.nachname)
                .replace("{vorname}", betreuter.vorname)
                .replace("{aktenzeichen}", betreuter.aktenzeichen)
                .replace("{wohnort}", betreuter.wohnort)
    }

    private fun getValue(tx: MappedTransaction, field: RuleField): String {
        return when (field) {
            RuleField.DATE ->
                    tx.date.toString() // Or formatted? Using toString for stable comparison
            // typically
            RuleField.PAYEE -> tx.payee
            RuleField.PURPOSE -> tx.purpose
            RuleField.EXPENSE ->
                    if (tx.type == TransactionType.EXPENSE) tx.amount.toString() else ""
            RuleField.INCOME -> if (tx.type == TransactionType.INCOME) tx.amount.toString() else ""
        }
    }

    private fun setValue(
            tx: MappedTransaction,
            field: RuleField,
            value: String
    ): MappedTransaction {
        return when (field) {
            RuleField.DATE -> tx // Not supported for replacement currently
            RuleField.PAYEE -> tx.copy(payee = value)
            RuleField.PURPOSE -> tx.copy(purpose = value)
            RuleField.EXPENSE -> tx // Amount replacement from string template is tricky math
            RuleField.INCOME -> tx // Amount replacement from string template is tricky math
        }
    }
}
