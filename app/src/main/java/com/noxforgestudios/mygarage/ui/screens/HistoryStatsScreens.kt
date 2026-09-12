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
import java.util.Date

@Composable
fun HistoryScreen(state: GarageUiState, onRecord: (GarageRecord) -> Unit, onAddKind: (RecordKind) -> Unit) {
    var filter by remember { mutableStateOf<RecordKind?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    val filtered = state.records.filter { filter == null || it.kind == filter }
    Scaffold(topBar = { TopAppBar(title = { Text("Historial") }, actions = { Box { IconButton(onClick = { addMenu = true }) { Icon(Icons.Default.Add, "Añadir") }; DropdownMenu(addMenu, { addMenu = false }) { RecordKind.entries.filter { it != RecordKind.ODOMETER }.forEach { k -> DropdownMenuItem(text = { Text(k.displayName) }, leadingIcon = { Icon(recordIcon(k), null) }, onClick = { addMenu = false; onAddKind(k) }) } } } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todo") }) }
                items(RecordKind.entries) { k -> FilterChip(selected = filter == k, onClick = { filter = k }, label = { Text(k.displayName) }) }
            }
            if (filtered.isEmpty()) EmptyState(Icons.Default.History, "Aún no hay historial para mostrar.")
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(filtered, key = { "${it.kind}-${it.id}" }) { r -> RecordRow(r, state.preferences) { onRecord(r) }; HorizontalDivider(Modifier.padding(start = 56.dp)) }
            }
        }
    }
}

private enum class StatsRange(val label: String, val days: Int?) { DAYS30("30 días", 30), M3("3 meses", 90), M6("6 meses", 180), Y1("1 año", 365), ALL("Todo", null) }

@Composable
fun StatsScreen(state: GarageUiState) {
    var range by remember { mutableStateOf(StatsRange.Y1) }
    val cutoff = range.days?.let { Date(System.currentTimeMillis() - it * 86_400_000L) }
    val records = state.records.filter { cutoff == null || it.date >= cutoff }
    val vehicle = state.selectedVehicle
    val odo = records.mapNotNull { it.odometerKm }.sorted()
    val startOdo = odo.firstOrNull()
    val endOdo = odo.lastOrNull() ?: vehicle?.odometerKm
    val stats = VehicleCalculators.expenseStats(records, startOdo, endOdo)
    val refuels = records.filter { it.kind == RecordKind.REFUEL }.sortedBy { it.date }
    val consumptionValues = refuels.zipWithNext().mapNotNull { (a, b) ->
        VehicleCalculators.fuelMetrics(a, b).litersPer100Km?.let { UnitFormatters.consumptionFromL100(it, state.preferences).toFloat() }
    }
    val fuelPrices = refuels.mapNotNull { it.pricePerLiter?.let { price -> UnitFormatters.pricePerLiterToDisplayedVolume(price, state.preferences).toFloat() } }
    val totalLiters = refuels.sumOf { it.liters ?: 0.0 }
    val averageFuelPrice = refuels.mapNotNull { it.pricePerLiter }.takeIf { it.isNotEmpty() }?.average()
    val distanceText = UnitFormatters.formatDistance(stats.distanceKm, state.preferences)
    val costPerDistance = stats.costPerKm?.let { UnitFormatters.costPerDistanceFromPerKm(it, state.preferences) }
    val costPerDistanceText = costPerDistance?.let { "${UnitFormatters.money(it, state.preferences.currencyCode)}/${UnitFormatters.distanceLabel(state.preferences)}" } ?: "—"

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Estadísticas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item { androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(StatsRange.entries) { r -> FilterChip(selected = range == r, onClick = { range = r }, label = { Text(r.label) }) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Gasto total", UnitFormatters.money(stats.total, state.preferences.currencyCode), Icons.Default.Payments, Modifier.weight(1f)); StatCard("Coste/${UnitFormatters.distanceLabel(state.preferences)}", costPerDistanceText, Icons.Default.Speed, Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Combustible", UnitFormatters.money(stats.fuel, state.preferences.currencyCode), Icons.Default.LocalGasStation, Modifier.weight(1f)); StatCard("Mantenimiento", UnitFormatters.money(stats.maintenance, state.preferences.currencyCode), Icons.Default.Build, Modifier.weight(1f)) } }

        if (state.pro.isPro) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Reparaciones", UnitFormatters.money(stats.repairs, state.preferences.currencyCode), Icons.Default.Handyman, Modifier.weight(1f)); StatCard("Modificaciones", UnitFormatters.money(stats.modifications, state.preferences.currencyCode), Icons.Default.Tune, Modifier.weight(1f)) } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Recorrido", distanceText, Icons.Default.Route, Modifier.weight(1f)); StatCard("Combustible total", UnitFormatters.formatVolume(totalLiters, state.preferences), Icons.Default.LocalGasStation, Modifier.weight(1f)) } }
            item { Text("Evolución del consumo · ${UnitFormatters.consumptionLabel(state.preferences)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); ElevatedCard(Modifier.fillMaxWidth()) { SimpleLineChart(consumptionValues, Modifier.fillMaxWidth().height(190.dp).padding(8.dp)) } }
            item { Text("Evolución del precio · ${state.preferences.currencyCode}/${UnitFormatters.volumeLabel(state.preferences)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); ElevatedCard(Modifier.fillMaxWidth()) { SimpleLineChart(fuelPrices, Modifier.fillMaxWidth().height(190.dp).padding(8.dp)) } }
            if (averageFuelPrice != null) item { ListItem(headlineContent = { Text("Precio medio combustible") }, trailingContent = { Text("${UnitFormatters.money(UnitFormatters.pricePerLiterToDisplayedVolume(averageFuelPrice, state.preferences), state.preferences.currencyCode)}/${UnitFormatters.volumeLabel(state.preferences)}") }) }
            item { Text("Coste por categoría", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(stats.byCategory.entries.sortedByDescending { it.value }) { (cat, value) -> ListItem(headlineContent = { Text(cat) }, trailingContent = { Text(UnitFormatters.money(value, state.preferences.currencyCode), fontWeight = FontWeight.SemiBold) }) }
        } else {
            item { ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Estadísticas completas con PRO", fontWeight = FontWeight.Bold); Text("Desbloquea evolución de consumo y precio, kilometraje, combustible total, reparaciones, modificaciones y desglose por categorías.") } } }
        }
    }
}

@Composable
fun SearchScreen(state: GarageUiState, onBack: () -> Unit, onRecord: (GarageRecord) -> Unit) {
    var query by remember { mutableStateOf("") }
    val normalized = query.trim().lowercase()
    val results = if (normalized.length < 2) emptyList() else state.records.filter { r ->
        listOf(r.title, r.notes, r.category, r.parts, r.fault, r.symptoms, r.diagnosis, r.repairAction, r.brand, r.productModel, r.reference, r.description, r.workshop, r.station, r.provider, r.policy)
            .any { it.lowercase().contains(normalized) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Buscador global") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Buscar turbo, aceite, pieza, nota…") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            if (normalized.length < 2) Text("Escribe al menos 2 caracteres.")
            else if (results.isEmpty()) EmptyState(Icons.Default.SearchOff, "No se encontraron coincidencias.")
            else LazyColumn { items(results, key = { "${it.kind}-${it.id}" }) { RecordRow(it, state.preferences) { onRecord(it) }; HorizontalDivider() } }
        }
    }
}
