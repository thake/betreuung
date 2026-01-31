package de.thake.betreuung.logic

import de.thake.betreuung.model.Betreuter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document

data class MappedTransaction(
        val date: java.time.LocalDate,
        val payee: String,
        val purpose: String,
        val amount: Double,
        val type: TransactionType
)

enum class TransactionType {
    INCOME,
    EXPENSE
}

object XmlGenerator {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun generateXml(
            betreuter: Betreuter,
            accountTransactions: Map<de.thake.betreuung.model.BankAccount, List<MappedTransaction>>,
            periodStart: String, // dd.MM.yyyy (Input from UI is still String)
            periodEnd: String, // dd.MM.yyyy
            initialBalance: Double,
            outputFile: File
    ) {
        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val doc = docBuilder.newDocument()

        // Root: <xml-data>
        val rootElement = doc.createElement("xml-data")
        rootElement.setAttribute("xmlns", "http://www.lucom.com/ffw/xml-data-1.0.xsd")
        doc.appendChild(rootElement)

        // <form>
        val form = doc.createElement("form")
        form.textContent = "catalog://Formulare/BS24T"
        rootElement.appendChild(form)

        // <instance>
        val instance = doc.createElement("instance")
        rootElement.appendChild(instance)

        // Calculate sums (Total across all accounts)
        val allTransactions = accountTransactions.values.flatten()
        val totalIncome =
                allTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense =
                allTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val finalBalance = initialBalance + totalIncome - totalExpense

        // Main <datarow>
        val datarow = doc.createElement("datarow")
        instance.appendChild(datarow)

        // Fill generic fields
        addElement(doc, datarow, "geschNr", betreuter.aktenzeichen)
        addElement(doc, datarow, "nname", betreuter.nachname)
        addElement(doc, datarow, "vname", betreuter.vorname)
        // Betreuter dates are still strings in model, so we treat them as such or parse?
        // Logic says simple string passing if it matches, but let's be safe if needed.
        // For now, assuming Betreuter model strings are correct or we use ensureTime(String).
        addElement(doc, datarow, "gebdatum", ensureTime(betreuter.geburtsdatum))
        addElement(doc, datarow, "zeitvom", ensureTime(periodStart))
        addElement(doc, datarow, "zeitbis", ensureTime(periodEnd))

        // Map accounts info
        val accounts = accountTransactions.keys.toList()
        accounts.forEachIndexed { index, account ->
            if (index < 5) {
                val suffix = index + 1 // 1-based
                addElement(doc, datarow, "NameBank$suffix", account.bankName)
                addElement(doc, datarow, "KtoNr$suffix", account.iban)
            }
        }

        // If fewer than 1 account (shouldn't happen per UI checks), nothing added.
        addElement(doc, datarow, "ort", betreuter.wohnort)

        // Sums
        addElement(doc, datarow, "AbrechPos1", formatAmount(initialBalance))
        addElement(doc, datarow, "AbrechPos2", formatAmount(totalIncome))
        addElement(doc, datarow, "SummePos1Pos2", formatAmount(initialBalance + totalIncome))
        addElement(doc, datarow, "AbrechPos3", formatAmount(totalExpense))

        // Final results
        addElement(doc, datarow, "zwAusgaben", formatAmount(totalExpense))
        addElement(doc, datarow, "zwEinnahmen", formatAmount(totalIncome))
        addElement(doc, datarow, "Summe", formatAmount(finalBalance))

        addElement(doc, datarow, "aktdate", LocalDateTime.now().format(dateTimeFormatter))

        // Transactions: <dataset id="linked_Abrechnungstabelle1"> ... 5

        for (i in 0 until 5) {
            val suffix = i + 1
            // Use "linked_Abrechnungstabelle" for the first one, "linked_Abrechnungstabelle2" for
            // the second, etc.
            val datasetId =
                    if (i == 0) "linked_Abrechnungstabelle" else "linked_Abrechnungstabelle$suffix"

            val dataset = doc.createElement("dataset")
            dataset.setAttribute("id", datasetId)
            instance.appendChild(dataset)

            if (i < accounts.size) {
                // Fill with info from account i
                val account = accounts[i]
                val txList = (accountTransactions[account] ?: emptyList()).sortedBy { it.date }

                txList.forEach { tx ->
                    val txRow = doc.createElement("datarow")
                    dataset.appendChild(txRow)

                    // Format LocalDate to String for XML
                    addElement(doc, txRow, "DateS2", ensureTime(tx.date))
                    addElement(doc, txRow, "BezeichnungPos3", truncateToLength(tx.payee, 20))
                    addElement(doc, txRow, "BezeichnungPos4", truncateToLength(tx.purpose, 24))

                    if (tx.type == TransactionType.EXPENSE) {
                        addElement(doc, txRow, "ausgaben", formatAmount(tx.amount))
                    } else {
                        addElement(doc, txRow, "einnahmen", formatAmount(tx.amount))
                    }
                }
            } else {
                // Empty dataset row (structure requirement?)
                val emptyRow = doc.createElement("datarow")
                dataset.appendChild(emptyRow)
            }
        }

        // Write to file
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")

        val source = DOMSource(doc)
        val result = StreamResult(outputFile)
        transformer.transform(source, result)
    }

    private fun addElement(doc: Document, parent: org.w3c.dom.Element, id: String, value: String) {
        val element = doc.createElement("element")
        element.setAttribute("id", id)
        element.textContent = value
        parent.appendChild(element)
    }

    // Overload for LocalDate
    private fun ensureTime(date: java.time.LocalDate): String {
        return date.format(dateFormatter) + " 00:00:00"
    }

    private fun ensureTime(dateStr: String): String {
        // First try to normalize the date with DateParser to handle various inputs
        val parsed = DateParser.parse(dateStr)
        if (parsed != null) {
            return ensureTime(parsed)
        }

        // Fallback: Just return what we got if parsing fails (legacy behavior expectation?)
        // Or we could append 00:00:00 if it looks like a date.
        // Let's try to keep meaningful content.
        if (dateStr.length <= 10 && dateStr.contains(".")) {
            return "$dateStr 00:00:00"
        }
        return dateStr
    }

    private fun formatAmount(amount: Double): String {
        return "%.2f".format(amount)
                .replace(
                        ",",
                        "."
                ) // XML usually implies dot or locale specific? Example uses dot "100.52"
    }

    fun truncateToLength(value: String, maxLength: Int): String {
        if (value.length <= maxLength) return value

        val words = value.split(" ")
        val sb = StringBuilder()

        for (word in words) {
            // +1 for space if not first word
            val extraSpace = if (sb.isNotEmpty()) 1 else 0
            if (sb.length + extraSpace + word.length <= maxLength) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(word)
            } else {
                break
            }
        }

        // Fallback: If even the first word is too long, hard truncate
        if (sb.isEmpty() && words.isNotEmpty()) {
            return value.substring(0, maxLength)
        }

        return sb.toString()
    }
}
