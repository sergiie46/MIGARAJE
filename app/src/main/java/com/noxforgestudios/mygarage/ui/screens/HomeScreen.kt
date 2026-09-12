@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.noxforgestudios.mygarage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxforgestudios.mygarage.domain.*
import com.noxforgestudios.mygarage.ui.*
import java.util.Calendar
import java.util.Date

@Composable
fun HomeScreen(
    state: GarageUiState,
    onVehicle: () -> Unit,
    onAddVehicle: () -> Unit,
    onSelectVehicle: (String) -> Unit,
    onShareVehicle: () -> Unit,
    onUpdateKm: (Long, Boolean) -> Unit,
    onSearch: () -> Unit,
    onQuick: () -> Unit,
    onRecord: (GarageRecord) -> Unit
) {
    val vehicle = state.selectedVehicle
    var showKm by remember { mutableStateOf(false) }
    var kmText by remember(vehicle?.id, state.preferences.distanceUnit) { mutableStateOf(vehicle?.odometerKm?.let { UnitFormatters.editableDistance(it, state.preferences) }.orEmpty()) }
    var forceDecrease by remember { mutableStateOf(false) }
    val now = Date()
    val monthStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.time
    val yearStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0) }.time
    val monthSpent = state.records.filter { it.date >= monthStart }.sumOf { it.cost }
    val yearSpent = state.records.filter { it.date >= yearStart }.sumOf { it.cost }
    val upcoming = state.records.filter { it.nextDueKm != null || it.nextDueDate != null || it.nextDate != null || it.endDate != null }
        .sortedBy { minOf(it.nextDueDate?.time ?: Long.MAX_VALUE, it.nextDate?.time ?: Long.MAX_VALUE, it.endDate?.time ?: Long.MAX_VALUE) }
        .take(5)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.titleMedium)
                    Text(state.user?.displayName?.substringBefore(" ").orEmpty().ifBlank { "conductor" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Buscar") }
            }
            SyncStatusChip(state.syncState, state.online)
        }
        if (vehicle == null) {
            item { EmptyState(Icons.Default.DirectionsCar, "No tienes vehículos todavía.", "Añadir mi primer vehículo", onAddVehicle) }
        } else {
            item { VehicleShowcase(state.vehicles, vehicle.id, onSelectVehicle, onVehicle, onShareVehicle) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { showKm = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Speed, null); Spacer(Modifier.width(8.dp)); Text("Actualizar km") }
                    FilledTonalButton(onClick = onQuick, modifier = Modifier.weight(1f)) { Icon(Icons.Default.AddCircle, null); Spacer(Modifier.width(8.dp)); Text("Registrar") }
                }
            }
            item { Text("Próximo mantenimiento y avisos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (upcoming.isEmpty()) item { ElevatedCard(Modifier.fillMaxWidth()) { Text("Aún no hay próximos vencimientos. Registra un mantenimiento, ITV o seguro con próxima fecha/km.", Modifier.padding(16.dp)) } }
            items(upcoming, key = { "${it.kind}_${it.id}" }) { r ->
                ElevatedCard(onClick = { onRecord(r) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(recordIcon(r.kind), null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title.ifBlank { r.kind.displayName }, fontWeight = FontWeight.SemiBold)
                            val details = buildList {
                                r.nextDueKm?.let { due -> add("${UnitFormatters.formatDistance(due - vehicle.odometerKm, state.preferences)} restantes") }
                                (r.nextDueDate ?: r.nextDate ?: r.endDate)?.let { due -> add("${VehicleCalculators.remainingDays(now, due)} días") }
                            }
                            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Text("Resumen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Este mes", UnitFormatters.money(monthSpent, state.preferences.currencyCode), Icons.Default.CalendarMonth, Modifier.weight(1f))
                    StatCard("Este año", UnitFormatters.money(yearSpent, state.preferences.currencyCode), Icons.Default.Payments, Modifier.weight(1f))
                }
            }
            item {
                val fuel = state.records.filter { it.kind == RecordKind.REFUEL }.sumOf { it.cost }
                val maint = state.records.filter { it.kind == RecordKind.MAINTENANCE }.sumOf { it.cost }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Combustible", UnitFormatters.money(fuel, state.preferences.currencyCode), Icons.Default.LocalGasStation, Modifier.weight(1f))
                    StatCard("Mantenimiento", UnitFormatters.money(maint, state.preferences.currencyCode), Icons.Default.Build, Modifier.weight(1f))
                }
            }
        }
    }

    if (showKm && vehicle != null) {
        AlertDialog(
            onDismissRequest = { showKm = false },
            title = { Text("Actualizar kilómetros") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = kmText, onValueChange = { kmText = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Kilometraje actual (${UnitFormatters.distanceLabel(state.preferences)})") }, singleLine = true)
                    val entered = kmText.replace(',', '.').toDoubleOrNull()?.let { UnitFormatters.distanceToKm(it, state.preferences) }
                    if (entered != null && entered < vehicle.odometerKm) {
                        Text("El valor es menor que ${UnitFormatters.formatDistance(vehicle.odometerKm, state.preferences)}. Activa la confirmación solo si es intencionado.", color = MaterialTheme.colorScheme.error)
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(forceDecrease, { forceDecrease = it }); Text("Confirmo que quiero reducir el kilometraje registrado") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = kmText.replace(',', '.').toDoubleOrNull()?.let { UnitFormatters.distanceToKm(it, state.preferences) } ?: return@TextButton
                    if (value >= vehicle.odometerKm || forceDecrease) { onUpdateKm(value, forceDecrease); showKm = false }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showKm = false }) { Text("Cancelar") } }
        )
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) { in 5..12 -> "Buenos días"; in 13..19 -> "Buenas tardes"; else -> "Buenas noches" }
}

@Composable
fun QuickAddScreen(onBack: () -> Unit, onKind: (RecordKind) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Añadir registro") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(RecordKind.MODIFICATION, RecordKind.PART, RecordKind.REFUEL, RecordKind.MAINTENANCE, RecordKind.REPAIR, RecordKind.EXPENSE, RecordKind.ODOMETER, RecordKind.REMINDER)) { kind ->
                ElevatedCard(onClick = { onKind(kind) }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(recordIcon(kind), null); Spacer(Modifier.width(14.dp)); Text(kind.displayName, style = MaterialTheme.typography.titleMedium) } }
            }
        }
    }
}
