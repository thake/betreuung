package de.thake.betreuung.logic

import de.thake.betreuung.model.BankAccount
import de.thake.betreuung.model.Betreuter
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class XmlGenerationTest {

    @TempDir lateinit var tempDir: File

    @Test
    fun `generateXml processes dates and dataset ids correctly`() {
        // Setup
        val account = BankAccount("id1", "DE123", "TestBank")
        val betreuter =
                Betreuter("b1", "Mustermann", "Max", "01.01.1980", "AZ123", listOf(account), "City")

        // Transaction with 2-digit year
        val tx = MappedTransaction("01.01.24", "Payee", "Purpose", 10.0, TransactionType.EXPENSE)

        val accountTransactions = mapOf(account to listOf(tx))
        val outFile = File(tempDir, "output.xml")

        // Execute
        XmlGenerator.generateXml(
                betreuter,
                accountTransactions,
                "01.01.2024",
                "31.12.2024",
                100.0,
                outFile
        )

        // Verify
        assertTrue(outFile.exists())

        // Parse XML to check details
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(outFile)

        // 1. Check Dataset ID for first account (should be "linked_Abrechnungstabelle", NOT "...1")
        val datasets = doc.getElementsByTagName("dataset")
        var foundMainDataset = false
        for (i in 0 until datasets.length) {
            val el = datasets.item(i) as org.w3c.dom.Element
            val id = el.getAttribute("id")
            if (id == "linked_Abrechnungstabelle") {
                foundMainDataset = true
            }
            assertFalse(
                    id == "linked_Abrechnungstabelle1",
                    "Should not contain linked_Abrechnungstabelle1"
            )
        }
        assertTrue(foundMainDataset, "Should contain linked_Abrechnungstabelle")

        // 2. Check Date format (should be normalized to 4-digit year)
        val elements = doc.getElementsByTagName("element")
        var dateFound = false
        for (i in 0 until elements.length) {
            val el = elements.item(i) as org.w3c.dom.Element
            if (el.getAttribute("id") == "DateS2") {
                val text = el.textContent
                // Expect 01.01.2024 00:00:00, NOT 01.01.24 ...
                assertTrue(text.contains("2024"), "Date should have 4-digit year: $text")
                assertTrue(text.startsWith("01.01.2024"), "Date format incorrect: $text")
                dateFound = true
            }
        }
        assertTrue(dateFound, "Transaction date element not found")
    }
}
