package de.thake.betreuung.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.thake.betreuung.logic.DataManager
import de.thake.betreuung.model.*
import java.util.UUID

@Composable
fun RuleScreen(appState: AppStateModel) {
    var showDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<ReplacementRule?>(null) }

    // Load Rules if empty
    LaunchedEffect(Unit) {
        if (appState.rulesList.isEmpty()) {
            val loaded = DataManager.loadRules()
            if (loaded.isNotEmpty()) {
                appState.rulesList.addAll(loaded)
            }
        }
    }

    fun saveRule(rule: ReplacementRule) {
        val index = appState.rulesList.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            appState.rulesList[index] = rule
        } else {
            appState.rulesList.add(rule)
        }
        DataManager.saveRules(appState.rulesList)
        showDialog = false
        editingRule = null
    }

    fun deleteRule(rule: ReplacementRule) {
        appState.rulesList.remove(rule)
        DataManager.saveRules(appState.rulesList)
    }

    Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                        onClick = {
                            editingRule = null
                            showDialog = true
                        }
                ) { Icon(Icons.Default.Add, contentDescription = "Regel hinzufügen") }
            }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Ersetzungs-Regeln", style = MaterialTheme.typography.h5)
            Text(
                    "Regeln werden beim Erstellen der XML angewendet.",
                    style = MaterialTheme.typography.caption
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(appState.rulesList) { rule ->
                    val mappingText =
                            if (rule.mappingIds.isEmpty()) "Global"
                            else {
                                val names =
                                        rule.mappingIds.mapNotNull { id ->
                                            appState.mappingsList.find { it.id == id }?.name
                                        }
                                if (names.isEmpty()) "Unbekannte Mappings"
                                else names.joinToString(", ")
                            }

                    RuleItem(
                            rule = rule,
                            mappingName = mappingText,
                            onEdit = {
                                editingRule = rule
                                showDialog = true
                            },
                            onDelete = { deleteRule(rule) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        RuleEditDialog(
                initialRule = editingRule,
                mappings = appState.mappingsList,
                onSave = { saveRule(it) },
                onDismiss = {
                    showDialog = false
                    editingRule = null
                }
        )
    }
}

@Composable
fun RuleItem(rule: ReplacementRule, mappingName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEdit() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.subtitle1)
                Text(
                        "Bedingung: ${rule.condition.field.label} ${rule.condition.type} \"${rule.condition.value}\"",
                        style = MaterialTheme.typography.body2
                )
                Text(
                        "Aktion: Setze ${rule.action.targetField.label} = \"${rule.action.template}\"",
                        style = MaterialTheme.typography.body2
                )
                Text("Gültigkeit: $mappingName", style = MaterialTheme.typography.caption)
            }
            // Removed redundant Edit Button as the whole card is clickable
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colors.error)
            }
        }
    }
}

@Composable
fun RuleEditDialog(
        initialRule: ReplacementRule?,
        mappings: List<MappingProfile>,
        onSave: (ReplacementRule) -> Unit,
        onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var isGlobal by remember { mutableStateOf(initialRule?.mappingIds?.isEmpty() ?: true) }
    // Using a set for easier toggling
    var selectedMappingIds by remember {
        mutableStateOf(initialRule?.mappingIds?.toSet() ?: emptySet())
    }

    // Condition
    var condField by remember { mutableStateOf(initialRule?.condition?.field ?: RuleField.PAYEE) }
    var condType by remember {
        mutableStateOf(initialRule?.condition?.type ?: ReplacementConditionType.STARTS_WITH)
    }
    var condValue by remember { mutableStateOf(initialRule?.condition?.value ?: "") }

    // Action
    var actionTarget by remember {
        mutableStateOf(initialRule?.action?.targetField ?: RuleField.PURPOSE)
    }
    var actionTemplate by remember { mutableStateOf(initialRule?.action?.template ?: "") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (initialRule == null) "Neue Regel" else "Regel bearbeiten") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name der Regel") },
                            modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Gültigkeit", style = MaterialTheme.typography.subtitle2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                                checked = isGlobal,
                                onCheckedChange = {
                                    isGlobal = it
                                    if (it) selectedMappingIds = emptySet()
                                }
                        )
                        Text("Global (für alle Mappings)")
                    }

                    if (!isGlobal && mappings.isNotEmpty()) {
                        Text("Wähle Mappings (Mehrfachauswahl möglich)")
                        mappings.forEach { m ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                        checked = selectedMappingIds.contains(m.id),
                                        onCheckedChange = { checked ->
                                            selectedMappingIds =
                                                    if (checked) {
                                                        selectedMappingIds + m.id
                                                    } else {
                                                        selectedMappingIds - m.id
                                                    }
                                        }
                                )
                                Text(m.name)
                            }
                        }
                    } else if (!isGlobal && mappings.isEmpty()) {
                        Text(
                                "Keine Mappings verfügbar. Regel muss global sein.",
                                color = MaterialTheme.colors.error
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    Text("Bedingung (Wenn...)", style = MaterialTheme.typography.subtitle2)
                    // Field
                    EnumDropdown(RuleField.values(), condField, { condField = it }, { it.label })
                    // Type
                    EnumDropdown(
                            ReplacementConditionType.values(),
                            condType,
                            { condType = it },
                            { it.label }
                    )
                    // Value
                    OutlinedTextField(
                            value = condValue,
                            onValueChange = { condValue = it },
                            label = { Text("Wert") },
                            modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    Text("Aktion (Dann...)", style = MaterialTheme.typography.subtitle2)
                    Text("Ziel Feld:")
                    EnumDropdown(
                            RuleField.values(),
                            actionTarget,
                            { actionTarget = it },
                            { it.label }
                    )

                    OutlinedTextField(
                            value = actionTemplate,
                            onValueChange = { actionTemplate = it },
                            label = { Text("Vorlage") },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                            "Platzhalter: {kuerzel}, {nachname}, {vorname}, {aktenzeichen}, {wohnort}\nBei einem Regulären Ausdruck: $1, $2, ... für Gruppen",
                            style = MaterialTheme.typography.caption
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            val rule =
                                    ReplacementRule(
                                            id = initialRule?.id ?: UUID.randomUUID().toString(),
                                            name = name,
                                            mappingIds =
                                                    if (isGlobal) emptyList()
                                                    else selectedMappingIds.toList(),
                                            condition =
                                                    ReplacementCondition(
                                                            condField,
                                                            condType,
                                                            condValue
                                                    ),
                                            action =
                                                    ReplacementAction(actionTarget, actionTemplate),
                                            isActive = true
                                    )
                            onSave(rule)
                        },
                        enabled =
                                name.isNotBlank() &&
                                        condValue.isNotBlank() &&
                                        actionTemplate.isNotBlank()
                ) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
fun <T> EnumDropdown(values: Array<T>, selected: T, onSelect: (T) -> Unit, label: (T) -> String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label(selected)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { v ->
                DropdownMenuItem(
                        onClick = {
                            onSelect(v)
                            expanded = false
                        }
                ) { Text(label(v)) }
            }
        }
    }
}
