package de.thake.betreuung.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.thake.betreuung.logic.DataManager
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@Composable
fun LoginScreen(appState: AppStateModel) {
    var passwordInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var hasFile by remember { mutableStateOf(DataManager.hasBetreutenFile()) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun performLogin() {
        if (passwordInput.isBlank()) {
            error = "Passwort darf nicht leer sein"
            return
        }

        try {
            val pwChars = passwordInput.toCharArray()
            val loaded =
                    if (hasFile) {
                        DataManager.loadBetreuten(pwChars)
                    } else {
                        // Create empty file
                        DataManager.saveBetreuten(emptyList(), pwChars)
                        emptyList()
                    }

            // Success
            appState.password.value = pwChars
            appState.betreutenList.clear()
            appState.betreutenList.addAll(loaded)

            // Load mappings too (no implementation for enc yet so just load)
            appState.mappingsList.clear()
            appState.mappingsList.addAll(DataManager.loadMappings())

            appState.navigateTo(Screen.BETREUTEN_LIST)
        } catch (e: Exception) {
            logger.error(e) { "Login failed" }
            error = "Falsches Passwort oder Fehler: ${e.message}"
        }
    }

    if (showResetDialog) {
        AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Datenbank zurücksetzen?") },
                text = {
                    Text(
                            "Dies löscht alle Betreuer-Daten unwiderruflich! Andere Einstellungen bleiben erhalten."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                DataManager.deleteBetreuten()
                                hasFile = false
                                passwordInput = ""
                                error = ""
                                showResetDialog = false
                            }
                    ) { Text("Löschen", color = MaterialTheme.colors.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Abbrechen") }
                }
        )
    }

    Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
                text = if (hasFile) "Einloggen" else "Neues Passwort erstellen",
                style = MaterialTheme.typography.h4
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Passwort") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { performLogin() })
        )

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colors.error)

            if (hasFile) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showResetDialog = true }) {
                    Text("Datenbank zurücksetzen", color = MaterialTheme.colors.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { performLogin() }) { Text(if (hasFile) "Öffnen" else "Erstellen") }
    }
}
