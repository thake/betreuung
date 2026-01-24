package de.thake.betreuung.model

import kotlinx.serialization.Serializable

@Serializable
data class BankAccount(
        val id: String,
        val iban:
                String, // element id="KtoNr1" (or similar if multiple accounts in XML allowed? for
        // now we pick one)
        val bankName: String, // element id="NameBank1"
        val bic: String = "",
        val defaultMappingId: String? = null
)

@Serializable
data class Betreuter(
        val id: String,
        val nachname: String,
        val vorname: String,
        val geburtsdatum: String, // format: dd.MM.yyyy
        val aktenzeichen: String, // element id="geschNr"
        val accounts: List<BankAccount> = emptyList(),
        val wohnort: String // element id="ort"
)

@Serializable
data class MappingProfile(
        val id: String,
        val name: String,
        val columnMapping: Map<String, String> // XmlFieldId -> CsvHeaderName
)

object XmlFields {
    const val DATE = "DateS2"
    const val PAYEE = "BezeichnungPos3"
    const val PURPOSE = "BezeichnungPos4"
    const val EXPENSE = "ausgaben"
    const val INCOME = "einnahmen"

    val requiredTransactionFields = listOf(DATE, PAYEE, PURPOSE)
    val amountFields = listOf(EXPENSE, INCOME)
}
