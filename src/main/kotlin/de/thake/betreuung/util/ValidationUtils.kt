package de.thake.betreuung.util

object ValidationUtils {
    // Basic IBAN Regex (approximate):
    // 2 letter Country Code
    // 2 digit Checksum
    // 8 digit Bank Code (BLZ) - Germany specific? Generic IBAN is varying length.
    // 10 digit Account Number
    // Simple check: Starts with 2 letters, total length betw 15 and 34, alphanumeric.
    private val IBAN_REGEX = Regex("^[A-Z]{2}[0-9A-Z]{13,32}\$")

    // Date format dd.MM.yyyy
    private val DATE_REGEX = Regex("^\\d{2}\\.\\d{2}\\.\\d{4}\$")

    fun isValidIban(iban: String): Boolean {
        // Remove spaces for validation
        val cleanIban = iban.replace("\\s".toRegex(), "").uppercase()
        return IBAN_REGEX.matches(cleanIban)
        // Note: Real IBAN validation would require Modulo 97 check.
        // For this app, structure check is a good start.
    }

    fun isValidDate(date: String): Boolean {
        if (!DATE_REGEX.matches(date)) return false

        // Logical check
        try {
            val parts = date.split(".")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            if (month !in 1..12) return false
            if (day !in 1..31) return false
            // Simple leap year/days per month check could be added but this is sufficient for quick
            // win

            return true
        } catch (e: Exception) {
            return false
        }
    }
}
