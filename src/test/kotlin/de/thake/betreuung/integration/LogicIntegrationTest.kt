package de.thake.betreuung.integration

import de.thake.betreuung.logic.CsvLogic
import de.thake.betreuung.logic.MappedTransaction
import de.thake.betreuung.logic.TransactionType
import de.thake.betreuung.logic.XmlGenerator
import de.thake.betreuung.model.BankAccount
import de.thake.betreuung.model.Betreuter
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LogicIntegrationTest {

        @TempDir lateinit var tempFolder: File

        @Test
        fun testCsvToXmlFlow() {
                // 1. Setup Test Data
                val betreuter =
                        Betreuter(
                                id = "1",
                                nachname = "Mustermann",
                                vorname = "Max",
                                geburtsdatum = "01.01.1980",
                                aktenzeichen = "AZ12345",
                                wohnort = "Musterstadt",
                                accounts = listOf(BankAccount("acc1", "DE1234567890", "TestBank"))
                        )
                val account = betreuter.accounts.first()

                // 2. Create Dummy CSV
                // @TempDir gives a directory. We create file in it.
                val csvFile = File(tempFolder, "test.csv")
                csvFile.writeText(
                        """
            Datum;Empfaenger;Verwendungszweck;Betrag
            01.01.2024;Arbeitgeber;Gehalt;1.500,00
            05.01.2024;Vermieter;Miete;-800,00
            10.01.2024;Supermarkt;Einkauf;-50,50
            """.trimIndent(),
                        StandardCharsets.ISO_8859_1
                )

                // 3. Parse CSV (Mimic UI Logic)
                val rows = CsvLogic.readRows(csvFile)
                assertEquals(3, rows.size)

                // 4. Map Data (Mimic UI Mapper)
                val mappedTransactions =
                        rows.mapNotNull { row ->
                                val date = row["Datum"]!!
                                val payee = row["Empfaenger"]!!
                                val purpose = row["Verwendungszweck"]!!
                                val amountStr = row["Betrag"]!!
                                val amount =
                                        CsvLogic.parseAmount(
                                                amountStr
                                        ) // handle 1.500,00 vs -800,00

                                // Simple sign Logic for test
                                if (amount > 0) {
                                        MappedTransaction(
                                                date,
                                                payee,
                                                purpose,
                                                amount,
                                                TransactionType.INCOME
                                        )
                                } else {
                                        MappedTransaction(
                                                date,
                                                payee,
                                                purpose,
                                                kotlin.math.abs(amount),
                                                TransactionType.EXPENSE
                                        )
                                }
                        }

                assertEquals(3, mappedTransactions.size)
                assertEquals(1500.0, mappedTransactions[0].amount, 0.01) // Income
                assertEquals(TransactionType.INCOME, mappedTransactions[0].type)
                assertEquals(800.0, mappedTransactions[1].amount, 0.01) // Expense
                assertEquals(TransactionType.EXPENSE, mappedTransactions[1].type)

                // 5. Generate XML
                // 5. Generate XML
                val xmlFile = File(tempFolder, "result.xml")
                val initialBalance = 1000.00

                XmlGenerator.generateXml(
                        betreuter = betreuter,
                        accountTransactions = mapOf(account to mappedTransactions),
                        periodStart = "01.01.2024",
                        periodEnd = "31.01.2024",
                        initialBalance = initialBalance,
                        outputFile = xmlFile
                )

                // 6. Verify XML Content
                val xmlContent = xmlFile.readText()
                println(xmlContent)

                // Check generic fields
                assertTrue(xmlContent.contains("<element id=\"nname\">Mustermann</element>"))
                assertTrue(xmlContent.contains("<element id=\"KtoNr1\">DE1234567890</element>"))

                // Check Sums
                // Initial: 1000
                // Income: 1500
                // Expense: 800 + 50.50 = 850.50
                // Final: 1000 + 1500 - 850.50 = 1649.50

                // XML Generator formats with dot: "1649.50"
                assertTrue(
                        xmlContent.contains("<element id=\"zwEinnahmen\">1500.00</element>"),
                        "Should contain Total Income 1500.00"
                )
                assertTrue(
                        xmlContent.contains("<element id=\"zwAusgaben\">850.50</element>"),
                        "Should contain Total Expense 850.50"
                )
                assertTrue(
                        xmlContent.contains("<element id=\"Summe\">1649.50</element>"),
                        "Should contain Final Balance 1649.50"
                ) // check ID in gen

                // Dataset entries
                assertTrue(
                        xmlContent.contains("<element id=\"BezeichnungPos3\">Arbeitgeber</element>")
                )
                assertTrue(xmlContent.contains("<element id=\"BezeichnungPos4\">Miete</element>"))
        }

        @Test
        fun testMultiAccountXmlGeneration() {
                // 1. Setup Data with 2 Accounts
                val acc1 = BankAccount("id1", "DE100", "Bank A")
                val acc2 = BankAccount("id2", "DE200", "Bank B")
                val betreuter =
                        Betreuter(
                                id = "1",
                                nachname = "Mustermann",
                                vorname = "Max",
                                geburtsdatum = "01.01.1980",
                                aktenzeichen = "AZ12345",
                                wohnort = "Musterstadt",
                                accounts = listOf(acc1, acc2)
                        )

                // 2. Mock Transactions (merged from 2 CSVs conceptually)
                val tx1 =
                        MappedTransaction(
                                "01.01.2024",
                                "P1",
                                "Purpose1",
                                100.0,
                                TransactionType.INCOME
                        )
                val tx2 =
                        MappedTransaction(
                                "02.01.2024",
                                "P2",
                                "Purpose2",
                                50.0,
                                TransactionType.EXPENSE
                        )
                val mergedTransactions = listOf(tx1, tx2)

                // 3. Generate XML
                val xmlFile = File(tempFolder, "multi_result.xml")

                XmlGenerator.generateXml(
                        betreuter = betreuter,
                        accountTransactions = mapOf(acc1 to listOf(tx1), acc2 to listOf(tx2)),
                        periodStart = "01.01.2024",
                        periodEnd = "31.01.2024",
                        initialBalance = 0.0,
                        outputFile = xmlFile
                )

                // 4. Verify
                val xmlContent = xmlFile.readText()

                // Check if both IBANs are present
                assertTrue(xmlContent.contains("DE100"), "Should contain IBAN 1")
                assertTrue(xmlContent.contains("DE200"), "Should contain IBAN 2")

                // Check if sums are correct (Income 100 - Expense 50 = 50)
                assertTrue(
                        xmlContent.contains("<element id=\"Summe\">50.00</element>"),
                        "Final balance should be 50.00"
                )

                // Verify Split Datasets
                // Account 1 (tx1, 100 income) should be in linked_Abrechnungstabelle1
                assertTrue(
                        xmlContent.contains("id=\"linked_Abrechnungstabelle1\""),
                        "Should have dataset 1"
                )
                // Account 2 (tx2, 50 expense) should be in linked_Abrechnungstabelle2
                assertTrue(
                        xmlContent.contains("id=\"linked_Abrechnungstabelle2\""),
                        "Should have dataset 2"
                )

                // We should find tx1 near dataset 1
                // Simple regex check or just containment
                assertTrue(xmlContent.contains("Purpose1"), "Should contain Purpose1")
                assertTrue(xmlContent.contains("Purpose2"), "Should contain Purpose2")

                // To be precise, we could parse XML, but string check is likely enough for
                // integration test if ID is unique
        }

        @Test
        fun testParseDirtyCsv() {
                // 1. Create Dirty CSV (mimic 24-01-2026 file)
                val csvFile = File(tempFolder, "dirty.csv")
                csvFile.writeText(
                        """
                        "Girokonto";"DE123"
                        
                        "Kontostand:";"100 €"
                        ""
                        "Buchungsdatum";"Empfaenger";"Betrag"
                        "01.01.2024";"A";"10,00"
                        "02.01.2024";"B";"-5,00"
                        """.trimIndent(),
                        StandardCharsets.ISO_8859_1
                )

                // 2. Read headers
                // Should auto-skip first 4 lines
                val headers = CsvLogic.readHeaders(csvFile)
                assertTrue(headers.contains("Buchungsdatum"), "Should find Buchungsdatum header")
                assertTrue(headers.contains("Empfaenger"), "Should find Empfaenger header")

                // 3. Read rows
                val rows = CsvLogic.readRows(csvFile)
                assertEquals(2, rows.size)
                assertEquals("A", rows[0]["Empfaenger"])
                assertEquals("B", rows[1]["Empfaenger"])
        }
}
