package de.thake.betreuung.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainLayout(appState: AppStateModel, content: @Composable () -> Unit) {
    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Betreuung Vermögensverwaltung") },
                        actions = {
                            TextButton(onClick = { appState.navigateTo(Screen.BETREUTEN_LIST) }) {
                                Icon(Icons.Default.AccountBox, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Betreute", color = MaterialTheme.colors.onPrimary)
                            }
                            TextButton(onClick = { appState.navigateTo(Screen.MAPPING_LIST) }) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Mappings", color = MaterialTheme.colors.onPrimary)
                            }
                            TextButton(onClick = { appState.navigateTo(Screen.RULES) }) {
                                Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null
                                ) // Reusing List icon or maybe Build?
                                Spacer(Modifier.width(4.dp))
                                Text("Regeln", color = MaterialTheme.colors.onPrimary)
                            }
                            TextButton(onClick = { appState.navigateTo(Screen.WORKFLOW) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Erstellen", color = MaterialTheme.colors.onPrimary)
                            }
                        }
                )
            }
    ) { content() }
}
