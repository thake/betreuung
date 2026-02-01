package de.thake.betreuung.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.thake.betreuung.logic.DataManager
import de.thake.betreuung.model.BankAccount
import de.thake.betreuung.model.Betreuter
import java.util.UUID

@Composable
fun BetreutenScreen(appState: AppStateModel) {
    var selectedBetreuter by remember { mutableStateOf<Betreuter?>(null) }

    // Save helper
    fun save() {
        DataManager.saveBetreuten(appState.betreutenList, appState.password.value)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // List Panel
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
            Button(
                    onClick = {
                        val newB =
                                Betreuter(
                                        id = UUID.randomUUID().toString(),
                                        nachname = "Neu",
                                        vorname = "",
                                        geburtsdatum = "",
                                        aktenzeichen = "",
                                        wohnort = "",
                                        kuerzel = ""
                                )
                        appState.betreutenList.add(newB)
                        selectedBetreuter = newB
                        save()
                    },
                    modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Text("Neuer Betreuter")
            }

            LazyColumn {
                items(appState.betreutenList) { b ->
                    Card(
                            modifier =
                                    Modifier.fillMaxWidth().padding(4.dp).clickable {
                                        selectedBetreuter = b
                                    },
                            backgroundColor =
                                    if (selectedBetreuter?.id == b.id)
                                            MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colors.surface
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

        // Detail Panel
        Box(modifier = Modifier.weight(2f).fillMaxHeight().padding(8.dp).background(Color.White)) {
            if (selectedBetreuter != null) {
                BetreuterEditForm(
                        betreuter = selectedBetreuter!!,
                        onUpdate = { updated ->
                            val idx = appState.betreutenList.indexOfFirst { it.id == updated.id }
                            if (idx != -1) {
                                appState.betreutenList[idx] = updated
                                selectedBetreuter = updated
                            }
                            save()
                        },
                        onDelete = {
                            appState.betreutenList.removeIf { it.id == selectedBetreuter!!.id }
                            selectedBetreuter = null
                            save()
                        }
                )
            } else {
                Text(
                        "Bitte wählen Sie einen Betreuten aus.",
                        modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun BetreuterEditForm(betreuter: Betreuter, onUpdate: (Betreuter) -> Unit, onDelete: () -> Unit) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text("Details bearbeiten", style = MaterialTheme.typography.h6)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Löschen", tint = Color.Red)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            Row {
                OutlinedTextField(
                        value = betreuter.nachname,
                        onValueChange = { onUpdate(betreuter.copy(nachname = it)) },
                        label = { Text("Nachname") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                        value = betreuter.vorname,
                        onValueChange = { onUpdate(betreuter.copy(vorname = it)) },
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
                    onValueChange = { onUpdate(betreuter.copy(kuerzel = it)) },
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
                    onValueChange = { onUpdate(betreuter.copy(geburtsdatum = it)) },
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
                    onValueChange = { onUpdate(betreuter.copy(aktenzeichen = it)) },
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
                    onValueChange = { onUpdate(betreuter.copy(wohnort = it)) },
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
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                            value = acc.bankName,
                            onValueChange = { newName ->
                                val newAccs =
                                        betreuter.accounts.map {
                                            if (it.id == acc.id) it.copy(bankName = newName) else it
                                        }
                                onUpdate(betreuter.copy(accounts = newAccs))
                            },
                            label = { Text("Bank Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    val ibanValid =
                            acc.iban.isEmpty() ||
                                    de.thake.betreuung.util.ValidationUtils.isValidIban(acc.iban)

                    OutlinedTextField(
                            value = acc.iban,
                            onValueChange = { newIban ->
                                val newAccs =
                                        betreuter.accounts.map {
                                            if (it.id == acc.id) it.copy(iban = newIban) else it
                                        }
                                onUpdate(betreuter.copy(accounts = newAccs))
                            },
                            label = { Text(if (ibanValid) "IBAN" else "IBAN (Ungültig)") },
                            isError = !ibanValid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors =
                                    TextFieldDefaults.outlinedTextFieldColors(
                                            errorLabelColor = MaterialTheme.colors.error,
                                            errorBorderColor = MaterialTheme.colors.error
                                    )
                    )
                }
                IconButton(
                        onClick = {
                            val newAccs = betreuter.accounts.filter { it.id != acc.id }
                            onUpdate(betreuter.copy(accounts = newAccs))
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
                        onUpdate(betreuter.copy(accounts = newAccs))
                    }
            ) { Text("Konto hinzufügen") }
        }
    }
}
