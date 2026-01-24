package de.thake.betreuung.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.thake.betreuung.logic.*
import de.thake.betreuung.model.*
import java.awt.FileDialog
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*

enum class WorkStep {
    CONFIG,
    MAPPING,
    PREVIEW
}

@Composable
fun WorkScreen(appState: AppStateModel) {
    var step by remember { mutableStateOf(WorkStep.CONFIG) }

    // Config State
    var selectedBetreuter by remember { mutableStateOf<Betreuter?>(null) }

    // Multi-CSV Support
    // ImportSource State Wrapper
    class ImportSourceState(val account: BankAccount) {
        var file by mutableStateOf<File?>(null)
        var headers by mutableStateOf<List<String>>(emptyList())
        var rows by mutableStateOf<List<Map<String, String>>>(emptyList())
        var mapping = mutableStateMapOf<String, String>()
        var startBalance by mutableStateOf("0.00")
        var isLoading by mutableStateOf(false)
    }

    val importSources = remember { mutableStateListOf<ImportSourceState>() }

    // Selection state for Config (which account we are adding a file to? logic simplified: just
    // list all)
    // Selection state for Mapping
    var activeSourceForMapping by remember { mutableStateOf<ImportSourceState?>(null) }

    var periodStart by remember { mutableStateOf("01.01.2024") }
    var periodEnd by remember { mutableStateOf("31.12.2024") }
    var autoPeriod by remember { mutableStateOf(true) }

    // Logic
    fun detectPeriodFromSources() {
        // Try to find any date in the first few rows of any source
        val yearRegex = Regex(".*(\\d{2}\\.\\d{2}\\.(\\d{4})).*")

        for (source in importSources) {
            if (source.rows.isNotEmpty()) {
                // Sample first 50 rows
                for (row in source.rows.take(50)) {
                    for (value in row.values) {
                        val match = yearRegex.find(value)
                        if (match != null) {
                            val year = match.groupValues[2]
                            periodStart = "01.01.$year"
                            periodEnd = "31.12.$year"
                            return
                        }
                    }
                }
            }
        }
    }

    // initialBalance moved to per-account state

    // Global Mapping Profile Logic (Apply profile to CURRENT active source)
    var selectedMappingProfile by remember { mutableStateOf<MappingProfile?>(null) }
    var newMappingName by remember { mutableStateOf("") }

    // Error / Success
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successPath by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // Helpers
    fun loadFile(source: ImportSourceState, f: File) {
        source.file = f
        source.isLoading = true
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val h = CsvLogic.readHeaders(f)
                val r = CsvLogic.readRows(f)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    source.headers = h
                    source.rows = r
                    source.mapping.clear()
                    source.isLoading = false

                    if (autoPeriod) {
                        detectPeriodFromSources()
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    errorMsg = "Fehler beim Lesen der CSV: ${e.message}"
                    source.isLoading = false
                }
            }
        }
    }

    fun pickFile(source: ImportSourceState) {
        val fd =
                FileDialog(
                        null as java.awt.Frame?,
                        "CSV auswählen (${source.account.bankName})",
                        FileDialog.LOAD
                )
        fd.isVisible = true
        if (fd.directory != null && fd.file != null) {
            val f = File(fd.directory, fd.file)
            loadFile(source, f)
        }
    }

    fun applyMappingProfile(profile: MappingProfile) {
        activeSourceForMapping?.let { source ->
            source.mapping.clear()
            source.mapping.putAll(profile.columnMapping)
            selectedMappingProfile = profile

            // Save choice preference
            val currentAccount = source.account
            if (currentAccount.defaultMappingId != profile.id) {
                val updatedAccount = currentAccount.copy(defaultMappingId = profile.id)

                selectedBetreuter?.let { currentBetreuter ->
                    val updatedAccounts =
                            currentBetreuter.accounts.map {
                                if (it.id == updatedAccount.id) updatedAccount else it
                            }
                    val updatedBetreuter = currentBetreuter.copy(accounts = updatedAccounts)

                    val idx = appState.betreutenList.indexOfFirst { it.id == updatedBetreuter.id }
                    if (idx >= 0) {
                        appState.betreutenList[idx] = updatedBetreuter
                        selectedBetreuter = updatedBetreuter
                        DataManager.saveBetreuten(appState.betreutenList, appState.password.value)
                    }
                }
            }
        }
    }

    fun saveMappingProfile() {
        activeSourceForMapping?.let { source ->
            if (newMappingName.isNotBlank()) {
                val newProfile =
                        MappingProfile(
                                id = UUID.randomUUID().toString(),
                                name = newMappingName,
                                columnMapping = HashMap(source.mapping)
                        )
                appState.mappingsList.add(newProfile)
                DataManager.saveMappings(appState.mappingsList)
                selectedMappingProfile = newProfile
                newMappingName = ""
            }
        }
    }

    fun generate() {
        try {
            // Find sources with files
            val validSources = importSources.filter { it.file != null }
            if (validSources.isEmpty()) {
                errorMsg = "Keine CSV Dateien ausgewählt."
                return
            }

            // Process rows for ALL sources, grouped by account
            val accountTransactions =
                    validSources.associate { source ->
                        val txList =
                                source.rows.mapNotNull { row ->
                                    TransactionMapper.mapRow(row, source.mapping)
                                }
                        source.account to txList
                    }

            // Validate Dates
            val allTransactions = accountTransactions.values.flatten()
            val validationResult =
                    DateValidator.validateTransactions(allTransactions, periodStart, periodEnd)

            if (!validationResult.isValid) {
                errorMsg = validationResult.errorMessage
                return
            }

            val fd = FileDialog(null as java.awt.Frame?, "XML Speichern", FileDialog.SAVE)

            // Calculate default filename
            val year =
                    try {
                        periodEnd.split(".").last()
                    } catch (e: Exception) {
                        "YYYY"
                    }
            val bName = selectedBetreuter?.nachname ?: "Nachname"
            val bFirstName = selectedBetreuter?.vorname ?: "Vorname"
            fd.file = "$year-$bName-$bFirstName-Vermoegensverwaltung.xml"

            fd.isVisible = true
            if (fd.directory != null && fd.file != null) {
                val outFile = File(fd.directory, fd.file)

                val totalStartBalance =
                        validSources.sumOf { source ->
                            try {
                                CsvLogic.parseAmount(source.startBalance)
                            } catch (e: Exception) {
                                0.0
                            }
                        }

                XmlGenerator.generateXml(
                        betreuter = selectedBetreuter!!,
                        accountTransactions = accountTransactions,
                        periodStart = periodStart,
                        periodEnd = periodEnd,
                        initialBalance = totalStartBalance,
                        outputFile = outFile
                )

                successPath = outFile.absolutePath
                showSuccessDialog = true
            }
        } catch (e: Exception) {
            errorMsg = "Fehler: ${e.message}"
            e.printStackTrace()
        }
    }

    // Main Container
    Column(modifier = Modifier.fillMaxSize()) {
        if (showSuccessDialog) {
            AlertDialog(
                    onDismissRequest = { showSuccessDialog = false },
                    title = { Text("Erfolg") },
                    text = { Text("Datei erfolgreich gespeichert:\n$successPath") },
                    confirmButton = {
                        Button(onClick = { showSuccessDialog = false }) { Text("OK") }
                    }
            )
        }

        if (errorMsg != null) {
            AlertDialog(
                    onDismissRequest = { errorMsg = null },
                    title = { Text("Fehler") },
                    text = { Text(errorMsg!!) },
                    confirmButton = { Button(onClick = { errorMsg = null }) { Text("OK") } }
            )
        }

        when (step) {
            WorkStep.CONFIG -> {
                // Scrollable Content
                Column(
                        modifier =
                                Modifier.weight(1f)
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp)
                ) {
                    Text("Schritt 1: Konfiguration", style = MaterialTheme.typography.h5)
                    Spacer(Modifier.height(16.dp))

                    // Select Betreuter (Dropdown)
                    Text("Betreuten auswählen:")
                    if (appState.betreutenList.isEmpty()) {
                        Text("Keine Betreuten angelegt.", color = MaterialTheme.colors.error)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                        selectedBetreuter?.let { "${it.nachname}, ${it.vorname}" }
                                                ?: "Bitte wählen..."
                                )
                            }
                            DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                            ) {
                                appState.betreutenList.forEach { b ->
                                    DropdownMenuItem(
                                            onClick = {
                                                selectedBetreuter = b
                                                // Initialize import sources for this betreuter
                                                importSources.clear()
                                                b.accounts.forEach { acc ->
                                                    val source = ImportSourceState(acc)
                                                    acc.defaultMappingId?.let { defId ->
                                                        appState.mappingsList
                                                                .find { it.id == defId }
                                                                ?.let { profile ->
                                                                    source.mapping.putAll(
                                                                            profile.columnMapping
                                                                    )
                                                                }
                                                    }
                                                    importSources.add(source)
                                                }
                                                expanded = false
                                            }
                                    ) { Text("${b.nachname}, ${b.vorname}") }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Period (Now below Betreuter)
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                                checked = autoPeriod,
                                onCheckedChange = {
                                    autoPeriod = it
                                    if (it) {
                                        detectPeriodFromSources()
                                    }
                                }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Zeitraum automatisch ermitteln")
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                                value = periodStart,
                                onValueChange = { periodStart = it },
                                label = { Text("Zeitraum Von (dd.MT.yyyy)") },
                                modifier = Modifier.weight(1f),
                                readOnly = autoPeriod,
                                enabled = !autoPeriod
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                                value = periodEnd,
                                onValueChange = { periodEnd = it },
                                label = { Text("Zeitraum Bis") },
                                modifier = Modifier.weight(1f),
                                readOnly = autoPeriod,
                                enabled = !autoPeriod
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (selectedBetreuter != null) {
                        Text("Konten & CSV Dateien:", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(8.dp))

                        if (importSources.isEmpty()) {
                            Text(
                                    "Keine Konten für diesen Betreuten. Bitte erst Konto anlegen.",
                                    color = MaterialTheme.colors.error
                            )
                        } else {
                            importSources.forEach { source ->
                                Card(
                                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                                        elevation = 4.dp
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                        source.account.bankName,
                                                        style = MaterialTheme.typography.subtitle1
                                                )
                                                Text(
                                                        source.account.iban,
                                                        style = MaterialTheme.typography.caption
                                                )
                                            }
                                            Button(
                                                    onClick = { pickFile(source) },
                                                    enabled = !source.isLoading
                                            ) {
                                                if (source.isLoading) {
                                                    CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            color = MaterialTheme.colors.onPrimary
                                                    )
                                                } else {
                                                    Text(
                                                            if (source.file == null) "CSV wählen"
                                                            else source.file!!.name
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                                value = source.startBalance,
                                                onValueChange = { source.startBalance = it },
                                                label = { Text("Anfangsbestand (€)") },
                                                modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // Sticky Footer
                Surface(elevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    activeSourceForMapping =
                                            importSources.firstOrNull { it.file != null }
                                    step = WorkStep.MAPPING
                                },
                                enabled =
                                        (selectedBetreuter != null &&
                                                importSources.isNotEmpty() &&
                                                importSources.all { it.file != null })
                        ) { Text("Weiter zu Mapping") }
                    }
                }
            }
            WorkStep.MAPPING -> {
                // Scrollable Content
                Column(
                        modifier =
                                Modifier.weight(1f)
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp)
                ) {
                    Text("Schritt 2: Spalten zuordnen", style = MaterialTheme.typography.h5)
                    Spacer(Modifier.height(16.dp))

                    // Select which source to map
                    if (importSources.count { it.file != null } > 1) {
                        Text("Konto/Datei wählen:", style = MaterialTheme.typography.subtitle2)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            importSources.filter { it.file != null }.forEach { source ->
                                Button(
                                        onClick = { activeSourceForMapping = source },
                                        colors =
                                                if (activeSourceForMapping == source)
                                                        ButtonDefaults.buttonColors(
                                                                backgroundColor =
                                                                        MaterialTheme.colors.primary
                                                        )
                                                else
                                                        ButtonDefaults.buttonColors(
                                                                backgroundColor =
                                                                        MaterialTheme.colors.surface
                                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                ) { Text(source.account.bankName) }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (activeSourceForMapping == null) {
                        Text("Keine aktive Quelle ausgewählt.")
                    } else {
                        val currentSource = activeSourceForMapping!!
                        Text(
                                "Mapping für: ${currentSource.account.bankName} (${currentSource.file?.name})",
                                style = MaterialTheme.typography.h6
                        )
                        Spacer(Modifier.height(8.dp))

                        // Load Profile logic
                        if (appState.mappingsList.isNotEmpty()) {
                            Text("Gespeicherte Mappings anwenden:")
                            Row {
                                appState.mappingsList.forEach { profile ->
                                    Button(
                                            onClick = { applyMappingProfile(profile) },
                                            colors =
                                                    if (selectedMappingProfile == profile)
                                                            ButtonDefaults.buttonColors(
                                                                    backgroundColor =
                                                                            MaterialTheme.colors
                                                                                    .secondary
                                                            )
                                                    else ButtonDefaults.buttonColors()
                                    ) { Text(profile.name) }
                                    Spacer(Modifier.width(8.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Mapping Fields
                        val fields =
                                listOf(
                                        XmlFields.DATE to "Datum",
                                        XmlFields.PAYEE to "Empfänger/Zahler",
                                        XmlFields.PURPOSE to "Verwendungszweck",
                                        XmlFields.EXPENSE to "Ausgaben (Betrag)",
                                        XmlFields.INCOME to "Einnahmen (Betrag)"
                                )

                        fields.forEach { (fieldId, label) ->
                            Text(label, style = MaterialTheme.typography.subtitle2)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                var expanded by remember { mutableStateOf(false) }
                                val currentVal =
                                        currentSource.mapping[fieldId] ?: "Nicht zugewiesen"

                                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                    OutlinedButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                    ) { Text(currentVal) }
                                    DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.heightIn(max = 300.dp)
                                    ) {
                                        DropdownMenuItem(
                                                onClick = {
                                                    currentSource.mapping.remove(fieldId)
                                                    expanded = false
                                                }
                                        ) { Text("Nicht zugewiesen") }
                                        currentSource.headers.forEach { header ->
                                            DropdownMenuItem(
                                                    onClick = {
                                                        currentSource.mapping[fieldId] = header
                                                        expanded = false
                                                    }
                                            ) { Text(header) }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Save Profile
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                                value = newMappingName,
                                onValueChange = { newMappingName = it },
                                label = { Text("Als neues Profil speichern") },
                                modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { saveMappingProfile() }) { Text("Speichern") }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Sticky Footer
                Surface(elevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    // Validation Logic
                    val requiredFields =
                            listOf(
                                    XmlFields.DATE,
                                    XmlFields.PAYEE,
                                    XmlFields.PURPOSE,
                                    XmlFields.EXPENSE,
                                    XmlFields.INCOME
                            )

                    val allSourcesValid =
                            importSources.filter { it.file != null }.all { source ->
                                requiredFields.all { field -> source.mapping.containsKey(field) }
                            }

                    Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                                onClick = { step = WorkStep.CONFIG },
                                colors = ButtonDefaults.outlinedButtonColors()
                        ) { Text("Zurück") }

                        Button(onClick = { generate() }, enabled = allSourcesValid) {
                            Text("XML Erstellen")
                        }
                    }
                }
            }
            WorkStep.PREVIEW -> {
                // Optional: Show preview before saving. For now we generate directly.
            }
        }
    }
}
