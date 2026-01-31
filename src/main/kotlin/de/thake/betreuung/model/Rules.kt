package de.thake.betreuung.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReplacementConditionType(val label: String) {
    STARTS_WITH("Beginnt mit"),
    CONTAINS("Enthält"),
    EQUALS("Ist gleich"),
    REGEX("Regulärer Ausdruck")
}

@Serializable
enum class RuleField(val label: String) {
    DATE("Datum"),
    PAYEE("Empfänger/Zahler"),
    PURPOSE("Verwendungszweck"),
    EXPENSE("Ausgaben"),
    INCOME("Einnahmen")
}

@Serializable
data class ReplacementCondition(
        val field: RuleField,
        val type: ReplacementConditionType,
        val value: String
)

@Serializable data class ReplacementAction(val targetField: RuleField, val template: String)

@Serializable
data class ReplacementRule(
        val id: String,
        val name: String,
        val mappingIds: List<String> = emptyList(), // empty = Global
        val condition: ReplacementCondition,
        val action: ReplacementAction,
        val isActive: Boolean = true
)
