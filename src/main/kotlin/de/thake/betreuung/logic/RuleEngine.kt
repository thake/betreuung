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
                    rule.isActive &&
                            (rule.mappingIds.isEmpty() ||
                                    (activeMappingId != null &&
                                            rule.mappingIds.contains(activeMappingId)))
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
            val groups = matches(currentTx, rule.condition)
            if (groups != null) {
                currentTx = applyAction(currentTx, rule.action, betreuter, groups)
            }
        }
        return currentTx
    }

    private fun matches(tx: MappedTransaction, condition: ReplacementCondition): List<String>? {
        val value = getValue(tx, condition.field)
        val criteria = condition.value

        return when (condition.type) {
            ReplacementConditionType.STARTS_WITH ->
                    if (value.startsWith(criteria, ignoreCase = true)) emptyList() else null
            ReplacementConditionType.CONTAINS ->
                    if (value.contains(criteria, ignoreCase = true)) emptyList() else null
            ReplacementConditionType.EQUALS ->
                    if (value.equals(criteria, ignoreCase = true)) emptyList() else null
            ReplacementConditionType.REGEX -> {
                try {
                    val regex = criteria.toRegex(RegexOption.IGNORE_CASE)
                    regex.find(value)?.groupValues
                } catch (e: Exception) {
                    logger.error(e) { "Invalid regex: $criteria" }
                    null
                }
            }
        }
    }

    private fun applyAction(
            tx: MappedTransaction,
            action: ReplacementAction,
            betreuter: Betreuter,
            groups: List<String>
    ): MappedTransaction {
        val newValue = replacePlaceholders(action.template, betreuter, groups)
        return setValue(tx, action.targetField, newValue)
    }

    private fun replacePlaceholders(
            template: String,
            betreuter: Betreuter,
            groups: List<String>
    ): String {
        var result =
                template.replace("{nachname}", betreuter.nachname)
                        .replace("{vorname}", betreuter.vorname)
                        .replace("{aktenzeichen}", betreuter.aktenzeichen)
                        .replace("{wohnort}", betreuter.wohnort)
                        .replace("{kuerzel}", betreuter.kuerzel)

        for (i in groups.indices) {
            result = result.replace("\$$i", groups[i])
        }

        return result
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
