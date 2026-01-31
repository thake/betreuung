package de.thake.betreuung.integration

import de.thake.betreuung.logic.MappedTransaction
import de.thake.betreuung.logic.RuleEngine
import de.thake.betreuung.logic.TransactionType
import de.thake.betreuung.logic.XmlGenerator
import de.thake.betreuung.model.*
import java.io.File
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ApplicationE2ETest {

    @TempDir lateinit var tempFolder: File

    @Test
    fun `test complete flow with replacement rules`() {
        // 1. Setup Data
        val account = BankAccount("acc1", "DE12345", "TestBank", defaultMappingId = "map1")
        val betreuter =
                Betreuter(
                        id = "1",
                        nachname = "Mustermann",
                        vorname = "Max",
                        geburtsdatum = "01.01.1980",
                        aktenzeichen = "AZ-123",
                        wohnort = "Berlin",
                        accounts = listOf(account)
                )

        // 2. Define Transaction that needs replacement
        // Original: "Amazon DE" -> Should become "Online Order" (Specific Rule)
        // Original: "Miete" -> Should become "Rent for Mustermann" (Global Rule)
        val tx1 =
                MappedTransaction(
                        LocalDate.of(2024, 1, 10),
                        "Amazon DE",
                        "Purchase",
                        50.0,
                        TransactionType.EXPENSE
                )
        val tx2 =
                MappedTransaction(
                        LocalDate.of(2024, 1, 1),
                        "Landlord",
                        "Miete January",
                        800.0,
                        TransactionType.EXPENSE
                )
        val txList = listOf(tx1, tx2)

        // 3. Define Rules
        val specificRule =
                ReplacementRule(
                        id = "r1",
                        name = "Amazon Rule",
                        mappingId = "map1",
                        condition =
                                ReplacementCondition(
                                        RuleField.PAYEE,
                                        ReplacementConditionType.STARTS_WITH,
                                        "Amazon"
                                ),
                        action = ReplacementAction(RuleField.PAYEE, "Online Order"),
                        isActive = true
                )
        val globalRule =
                ReplacementRule(
                        id = "r2",
                        name = "Rent Rule",
                        mappingId = null, // Global
                        condition =
                                ReplacementCondition(
                                        RuleField.PURPOSE,
                                        ReplacementConditionType.CONTAINS,
                                        "Miete"
                                ),
                        action = ReplacementAction(RuleField.PURPOSE, "Rent for {nachname}"),
                        isActive = true
                )
        val rules = listOf(specificRule, globalRule)

        // 4. Mimic WorkScreen Logic: Apply Rules
        // Note: In real app, we load rules from DataManager. Here we pass them directly for testing
        // the flow logic.
        val processedTx =
                RuleEngine.applyRules(
                        transactions = txList,
                        rules = rules,
                        betreuter = betreuter,
                        activeMappingId = "map1"
                )

        // 5. Generate XML
        val xmlFile = File(tempFolder, "e2e_result.xml")
        XmlGenerator.generateXml(
                betreuter = betreuter,
                accountTransactions = mapOf(account to processedTx),
                periodStart = "01.01.2024",
                periodEnd = "31.01.2024",
                initialBalance = 1000.0,
                outputFile = xmlFile
        )

        // 6. Verify Output
        val xmlContent = xmlFile.readText()
        println(xmlContent)

        // Verify Specific Rule Applied
        // "Amazon DE" (Payee) replaced by "Online Order"
        // Wait, Rule was: Matches "Amazon" in PAYEE, Action: set PAYEE to "Online Order"
        // So checking if XML contains "Online Order" in Payee field (BezeichnungPos3)
        assertTrue(
                xmlContent.contains("<element id=\"BezeichnungPos3\">Online Order</element>"),
                "Should contain modified Payee"
        )

        // Verify Global Rule Applied
        // "Miete January" (Purpose) replaced by "Rent for Mustermann"
        // Purpose is "BezeichnungPos4"
        assertTrue(
                xmlContent.contains(
                        "<element id=\"BezeichnungPos4\">Rent for Mustermann</element>"
                ),
                "Should contain modified Purpose with template substitution"
        )
    }
}
