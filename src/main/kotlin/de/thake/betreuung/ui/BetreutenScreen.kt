package de.thake.betreuung.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.thake.betreuung.logic.DataManager
import de.thake.betreuung.model.BankAccount
import de.thake.betreuung.model.Betreuter
import java.util.UUID

@Composable
fun BetreutenScreen(appState: AppStateModel) {
    var showDialog by remember { mutableStateOf(false) }
    var editingBetreuter by remember { mutableStateOf<Betreuter?>(null) }

    // Save helper
    fun save() {
        DataManager.saveBetreuten(appState.betreutenList, appState.password.value)
    }

    Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                        onClick = {
                            editingBetreuter =
                                    Betreuter(
                                            id = UUID.randomUUID().toString(),
                                            nachname = "",
                                            vorname = "",
                                            geburtsdatum = "",
                                            aktenzeichen = "",
                                            wohnort = "",
                                            kuerzel = ""
                                    )
                            showDialog = true
                        }
                ) { Icon(Icons.Default.Add, contentDescription = "Neuer Betreuter") }
            }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp)) {
            if (appState.betreutenList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Betreuten vorhanden. Klicken Sie auf + um einen hinzuzufügen.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(appState.betreutenList) { b ->
                        Card(
                                modifier =
                                        Modifier.fillMaxWidth().padding(4.dp).clickable {
                                            editingBetreuter = b
                                            showDialog = true
                                        },
                                elevation = 2.dp
                        ) {
                            Text(
                                    text = "${b.nachname}, ${b.vorname}",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.body1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog && editingBetreuter != null) {
        BetreuterEditDialog(
                initialBetreuter = editingBetreuter!!,
                onSave = { updated ->
                    val idx = appState.betreutenList.indexOfFirst { it.id == updated.id }
                    if (idx != -1) {
                        appState.betreutenList[idx] = updated
                    } else {
                        appState.betreutenList.add(updated)
                    }
                    save()
                    showDialog = false
                    editingBetreuter = null
                },
                onDelete = {
                    appState.betreutenList.removeIf { it.id == editingBetreuter!!.id }
                    save()
                    showDialog = false
                    editingBetreuter = null
                },
                onDismiss = {
                    showDialog = false
                    editingBetreuter = null
                }
        )
    }
}

@Composable
fun BetreuterEditDialog(
        initialBetreuter: Betreuter,
        onSave: (Betreuter) -> Unit,
        onDelete: () -> Unit,
        onDismiss: () -> Unit
) {
    // We need local state to edit before saving
    var betreuter by remember { mutableStateOf(initialBetreuter) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            if (initialBetreuter.nachname.isEmpty()) "Neuer Betreuter"
                            else "Betreuter bearbeiten"
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Löschen", tint = Color.Red)
                    }
                }
            },
            text = {
                LazyColumn(modifier = Modifier.semantics { testTag = "edit_list" }) {
                    item {
                        Row {
                            OutlinedTextField(
                                    value = betreuter.nachname,
                                    onValueChange = { betreuter = betreuter.copy(nachname = it) },
                                    label = { Text("Nachname") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                    value = betreuter.vorname,
                                    onValueChange = { betreuter = betreuter.copy(vorname = it) },
                                    label = { Text("Vorname") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                                value = betreuter.kuerzel,
                                onValueChange = { betreuter = betreuter.copy(kuerzel = it) },
                                label = { Text("Kürzel") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        val dateValid =
                                betreuter.geburtsdatum.isEmpty() ||
                                        de.thake.betreuung.util.ValidationUtils.isValidDate(
                                                betreuter.geburtsdatum
                                        )
                        OutlinedTextField(
                                value = betreuter.geburtsdatum,
                                onValueChange = { betreuter = betreuter.copy(geburtsdatum = it) },
                                label = {
                                    Text(
                                            if (dateValid) "Geburtsdatum (dd.MM.yyyy)"
                                            else "Geburtsdatum (Ungültiges Format)"
                                    )
                                },
                                isError = !dateValid,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors =
                                        TextFieldDefaults.outlinedTextFieldColors(
                                                errorLabelColor = MaterialTheme.colors.error,
                                                errorBorderColor = MaterialTheme.colors.error
                                        )
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                                value = betreuter.aktenzeichen,
                                onValueChange = { betreuter = betreuter.copy(aktenzeichen = it) },
                                label = { Text("Aktenzeichen (geschNr)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        OutlinedTextField(
                                value = betreuter.wohnort,
                                onValueChange = { betreuter = betreuter.copy(wohnort = it) },
                                label = { Text("Wohnort") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))
                    }

                    item { Text("Bankkonten", style = MaterialTheme.typography.h6) }

                    items(betreuter.accounts) { acc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                    value = acc.bankName,
                                    onValueChange = { newName ->
                                        val newAccs =
                                                betreuter.accounts.map {
                                                    if (it.id == acc.id) it.copy(bankName = newName)
                                                    else it
                                                }
                                        betreuter = betreuter.copy(accounts = newAccs)
                                    },
                                    label = { Text("Bank Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            Spacer(Modifier.width(8.dp))

                            val ibanValid =
                                    acc.iban.isEmpty() ||
                                            de.thake.betreuung.util.ValidationUtils.isValidIban(
                                                    acc.iban
                                            )

                            OutlinedTextField(
                                    value = acc.iban,
                                    onValueChange = { newIban ->
                                        val newAccs =
                                                betreuter.accounts.map {
                                                    if (it.id == acc.id) it.copy(iban = newIban)
                                                    else it
                                                }
                                        betreuter = betreuter.copy(accounts = newAccs)
                                    },
                                    label = { Text(if (ibanValid) "IBAN" else "IBAN (Ungültig)") },
                                    isError = !ibanValid,
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    colors =
                                            TextFieldDefaults.outlinedTextFieldColors(
                                                    errorLabelColor = MaterialTheme.colors.error,
                                                    errorBorderColor = MaterialTheme.colors.error
                                            )
                            )
                            IconButton(
                                    onClick = {
                                        val newAccs = betreuter.accounts.filter { it.id != acc.id }
                                        betreuter = betreuter.copy(accounts = newAccs)
                                    }
                            ) { Icon(Icons.Default.Delete, "Konto löschen") }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        Button(
                                onClick = {
                                    val newAcc = BankAccount(UUID.randomUUID().toString(), "", "")
                                    val newAccs = betreuter.accounts + newAcc
                                    betreuter = betreuter.copy(accounts = newAccs)
                                }
                        ) { Text("Konto hinzufügen") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onSave(betreuter) }, enabled = betreuter.nachname.isNotBlank()) {
                    Text("Speichern")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
