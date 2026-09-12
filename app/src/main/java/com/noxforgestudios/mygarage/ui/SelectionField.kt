package com.noxforgestudios.mygarage.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noxforgestudios.mygarage.domain.VehicleCatalog

@Composable
fun SelectionField(label: String, value: String, options: List<String>, enabled: Boolean = true,
                   allowCustom: Boolean = true, loading: Boolean = false, onValue: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedCard(onClick = { if (enabled) { query = ""; open = true } },
        enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(value.ifBlank { "Seleccionar" }, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.ExpandMore, "Abrir $label")
        }
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) }, text = {
        Column {
            OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Buscar") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            val filtered = remember(options, query) { options.distinct().filter { VehicleCatalog.searchKey(it).contains(VehicleCatalog.searchKey(query)) } }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                items(filtered, key = { it }) { option ->
                    ListItem(headlineContent = { Text(option) }, trailingContent = {
                        if (option == value) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }, modifier = Modifier.clickable { onValue(option); open = false })
                }
                if (filtered.isEmpty()) item { Text("No hay coincidencias", Modifier.padding(16.dp)) }
            }
            if (allowCustom && query.isNotBlank() && options.none { it.equals(query.trim(), true) }) {
                TextButton(onClick = { onValue(query.trim()); open = false }) { Icon(Icons.Default.Add, null); Text("Usar «${query.trim()}»") }
            }
            if (allowCustom) Text("¿No aparece? Escríbelo arriba para añadirlo.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { open = false }) { Text("Cerrar") } },
        dismissButton = { if (value.isNotBlank()) TextButton(onClick = { onValue(""); open = false }) { Text("Quitar selección") } })
}
