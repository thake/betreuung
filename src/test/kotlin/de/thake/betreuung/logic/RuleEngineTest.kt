package de.thake.betreuung.logic

import de.thake.betreuung.model.*
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RuleEngineTest {

    private val sampleTx =
            MappedTransaction(
                    date = LocalDate.of(2024, 1, 1),
                    payee = "Amazon Market",
                    purpose = "Kauf 12345",
                    amount = 50.0,
                    type = TransactionType.EXPENSE
            )

    private val sampleBetreuter =
            Betreuter(
                    id = "1",
                    nachname = "Mustermann",
                    vorname = "Max",
                    geburtsdatum = "01.01.1980",
                    aktenzeichen = "AZ123",
                    wohnort = "Berlin",
                    accounts = emptyList()
            )

    @Test
    fun `test STARTS_WITH condition matches`() {
        val rule =
                ReplacementRule(
                        id = "r1",
                        name = "Test Rule",
                        condition =
                                ReplacementCondition(
                                        RuleField.PAYEE,
                                        ReplacementConditionType.STARTS_WITH,
                                        "Amazon"
                                ),
                        action = ReplacementAction(RuleField.PURPOSE, "Online Order"),
                        isActive = true
                )

        val result = RuleEngine.applyRules(listOf(sampleTx), listOf(rule), sampleBetreuter)
        assertEquals("Online Order", result[0].purpose)
    }

    @Test
    fun `test CONTAINS condition matches`() {
        val rule =
                ReplacementRule(
                        id = "r1",
                        name = "Test Rule",
                        condition =
                                ReplacementCondition(
                                        RuleField.PAYEE,
                                        ReplacementConditionType.CONTAINS,
                                        "Market"
                                ),
                        action = ReplacementAction(RuleField.PURPOSE, "Market Purchase"),
                        isActive = true
                )

        val result = RuleEngine.applyRules(listOf(sampleTx), listOf(rule), sampleBetreuter)
        assertEquals("Market Purchase", result[0].purpose)
    }

    @Test
    fun `test EQUALS condition matches`() {
        // "Amazon Market" equals "Amazon Market"
        val rule =
                ReplacementRule(
                        id = "r1",
                        name = "Test Rule",
                        condition =
                                ReplacementCondition(
                                        RuleField.PAYEE,
                                        ReplacementConditionType.EQUALS,
                                        "Amazon Market"
                                ),
                        action = ReplacementAction(RuleField.PURPOSE, "Exact Match"),
                        isActive = true
                )

        val result = RuleEngine.applyRules(listOf(sampleTx), listOf(rule), sampleBetreuter)
        assertEquals("Exact Match", result[0].purpose)
    }

    @Test
    fun `test condition does NOT match`() {
        val rule =
                ReplacementRule(
                        id = "r1",
                        name = "Test Rule",
                        condition =
                                ReplacementCondition(
                                        RuleField.PAYEE,
                                        ReplacementConditionType.STARTS_WITH,
                                        "Google"
                                ),
                        action = ReplacementAction(RuleField.PURPOSE, "Should Not Happen"),
                        isActive = true
                )

        val result = RuleEngine.applyRules(listOf(sampleTx), listOf(rule), sampleBetreuter)
        assertEquals("Kauf 12345", result[0].purpose)
    }

    @Test
    fun `test template replacement`() {
        val rule =
                ReplacementRule(
                        id = "r1",
                        name = "Test Rule",
                        condition =
                                ReplacementCondition(
                                        RuleField.PAYEE,
                                        ReplacementConditionType.STARTS_WITH,
                                        "Amazon"
                                ),
                        action =
                                ReplacementAction(
                                        RuleField.PURPOSE,
                                        "Order for {nachname}, {vorname}"
                                ),
                        isActive = true
                )

        val result = RuleEngine.applyRules(listOf(sampleTx), listOf(rule), sampleBetreuter)
        assertEquals("Order for Mustermann, Max", result[0].purpose)
    }

    @Test
    fun `test global rule applies when mappingId is null`() {
        val rule =
                ReplacementRule(
                        id = "r1",
                        name = "Global Rule",
                        mappingId = null,
                        condition =
                                ReplacementCondition(
                                        RuleField.PAYEE,
                                        ReplacementConditionType.STARTS_WITH,
                                        "Amazon"
                                ),
                        action = ReplacementAction(RuleField.PURPOSE, "Global"),
                        isActive = true
                )

        // different mapping ID active
        val result =
                RuleEngine.applyRules(
                        listOf(sampleTx),
                        listOf(rule),
                        sampleBetreuter,
                        activeMappingId = "someMappingId"
                )
        assertEquals("Global", result[0].purpose)
    }

    @Test
    fun `test specific rule applies only when mappingId matches`() {
        val rule =
                ReplacementRule(
                        id = "r1",
                        name = "Specific Rule",
                        mappingId = "m1",
                        condition =
                                ReplacementCondition(
                                        RuleField.PAYEE,
                                        ReplacementConditionType.STARTS_WITH,
                                        "Amazon"
                                ),
                        action = ReplacementAction(RuleField.PURPOSE, "Specific"),
                        isActive = true
                )

        // Match
        val result1 =
                RuleEngine.applyRules(
                        listOf(sampleTx),
                        listOf(rule),
                        sampleBetreuter,
                        activeMappingId = "m1"
                )
        assertEquals("Specific", result1[0].purpose)

        // No match
        val result2 =
                RuleEngine.applyRules(
                        listOf(sampleTx),
                        listOf(rule),
                        sampleBetreuter,
                        activeMappingId = "m2"
                )
        assertEquals("Kauf 12345", result2[0].purpose) // Unchanged
    }
}
