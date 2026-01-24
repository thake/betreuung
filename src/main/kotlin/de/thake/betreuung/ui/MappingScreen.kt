package de.thake.betreuung.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.thake.betreuung.logic.DataManager

@Composable
fun MappingScreen(appState: AppStateModel) {
    
    fun save() {
        DataManager.saveMappings(appState.mappingsList)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Gespeicherte Mappings", style = MaterialTheme.typography.h5)
        Text("Hier können Sie alte Mappings verwalten. Neue Mappings werden im 'Erstellen' Prozess angelegt.", style = MaterialTheme.typography.caption)
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn {
            items(appState.mappingsList) { mapping ->
                Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mapping.name, style = MaterialTheme.typography.h6)
                            Text("${mapping.columnMapping.size} Felder verknüpft", style = MaterialTheme.typography.body2)
                        }
                        IconButton(onClick = {
                            appState.mappingsList.remove(mapping)
                            save()
                        }) {
                            Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colors.error)
                        }
                    }
                }
            }
        }
    }
}
