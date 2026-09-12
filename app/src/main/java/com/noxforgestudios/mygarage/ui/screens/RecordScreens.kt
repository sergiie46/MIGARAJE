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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noxforgestudios.mygarage.domain.*
import com.noxforgestudios.mygarage.ui.*
import java.util.Date

private val maintenanceTypes = listOf("Aceite", "Filtro aceite", "Filtro aire", "Filtro combustible", "Filtro habitáculo", "Distribución", "Cadena distribución", "Correa accesorios", "Líquido refrigerante", "Líquido frenos", "Aceite caja", "Aceite diferencial", "Embrague", "Frenos delanteros", "Frenos traseros", "Bujías", "Calentadores", "Batería", "Neumáticos", "Alineación", "Suspensión", "Revisión general", "Personalizado")
private val modificationCategories = listOf("Motor", "Turbo", "Admisión", "Escape", "Electrónica", "Suspensión", "Frenos", "Transmisión", "Diferencial", "Ruedas", "Neumáticos", "Interior", "Exterior", "Iluminación", "Audio", "Otros")
private val expenseCategories = listOf("Combustible", "Mantenimiento", "Reparación", "Seguro", "Impuestos", "ITV", "Parking", "Peajes", "Multas", "Lavado", "Modificaciones", "Piezas", "Neumáticos", "Otros")

@Composable
fun VehicleDetailScreen(state: GarageUiState, onBack: () -> Unit, onEditVehicle: () -> Unit, onGenerateCard: () -> Unit, onKind: (RecordKind) -> Unit) {
    val vehicle = state.selectedVehicle
    Scaffold(topBar = { TopAppBar(title = { Text(vehicle?.title ?: "Vehículo") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }, actions = { IconButton(onClick = onEditVehicle) { Icon(Icons.Default.Edit, "Editar") } }) }) { pad ->
        if (vehicle == null) return@Scaffold
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { VehicleSummaryCard(vehicle, state.preferences) }
            item {
                Button(onClick = onGenerateCard, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Generar ficha visual del coche")
                }
            }
            item { Text("Registros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(RecordKind.entries.filter { it != RecordKind.ODOMETER }) { kind ->
                ElevatedCard(onClick = { onKind(kind) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(recordIcon(kind), null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(kind.displayName, fontWeight = FontWeight.SemiBold); Text("${state.records.count { it.kind == kind }} registros", style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordListScreen(state: GarageUiState, kind: RecordKind, onBack: () -> Unit, onAdd: () -> Unit, onRecord: (GarageRecord) -> Unit, onRotateTyres: () -> Unit = {}) {
    val records = state.records.filter { it.kind == kind }
    Scaffold(topBar = { TopAppBar(title = { Text(kind.displayName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }, actions = { if (kind == RecordKind.TYRE) TextButton(onClick = onRotateTyres) { Text("Rotar") }; IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Añadir") } }) }) { pad ->
        if (records.isEmpty()) Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { EmptyState(recordIcon(kind), "Aún no has registrado ${kind.displayName.lowercase()}.", "Añadir", onAdd) }
        else LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(vertical = 8.dp)) { items(records, key = { it.id }) { RecordRow(it, state.preferences) { onRecord(it) }; HorizontalDivider() } }
    }
}

@Composable
fun RecordEditorScreen(kind: RecordKind, existing: GarageRecord?, vehicle: Vehicle?, vm: GarageViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val editorState by vm.state.collectAsStateWithLifecycle()
    val prefs = editorState.preferences
    if (vehicle == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Selecciona un vehículo") }; return }
    if (kind == RecordKind.ODOMETER) { OdometerEditor(vehicle, vm, onBack, onSaved); return }

    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var date by remember { mutableStateOf(existing?.date?.let(EsDateFormat::format) ?: EsDateFormat.format(Date())) }
    var km by remember(existing?.id, prefs.distanceUnit) { mutableStateOf(existing?.odometerKm?.let { UnitFormatters.editableDistance(it, prefs) } ?: UnitFormatters.editableDistance(vehicle.odometerKm, prefs)) }
    var cost by remember { mutableStateOf(existing?.cost?.takeIf { it != 0.0 }?.toString().orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category.orEmpty()) }
    var status by remember { mutableStateOf(existing?.status.orEmpty()) }
    var workshop by remember { mutableStateOf(existing?.workshop.orEmpty()) }
    var parts by remember { mutableStateOf(existing?.parts.orEmpty()) }
    var laborCost by remember { mutableStateOf(existing?.laborCost?.takeIf { it != 0.0 }?.toString().orEmpty()) }
    var partsCost by remember { mutableStateOf(existing?.partsCost?.takeIf { it != 0.0 }?.toString().orEmpty()) }
    var nextDueKm by remember(existing?.id, prefs.distanceUnit) { mutableStateOf(existing?.nextDueKm?.let { UnitFormatters.editableDistance(it, prefs) }.orEmpty()) }
    var nextDueDate by remember { mutableStateOf(existing?.nextDueDate?.let(EsDateFormat::format).orEmpty()) }
    var fault by remember { mutableStateOf(existing?.fault.orEmpty()) }
    var symptoms by remember { mutableStateOf(existing?.symptoms.orEmpty()) }
    var diagnosis by remember { mutableStateOf(existing?.diagnosis.orEmpty()) }
    var repairAction by remember { mutableStateOf(existing?.repairAction.orEmpty()) }
    var liters by remember(existing?.id, prefs.volumeUnit) { mutableStateOf(existing?.liters?.let { UnitFormatters.editableVolume(it, prefs) }.orEmpty()) }
    var priceLiter by remember(existing?.id, prefs.volumeUnit) { mutableStateOf(existing?.pricePerLiter?.let { UnitFormatters.editablePricePerVolume(it, prefs) }.orEmpty()) }
    var fullTank by remember { mutableStateOf(existing?.fullTank ?: false) }
    var station by remember { mutableStateOf(existing?.station.orEmpty()) }
    var fuelType by remember { mutableStateOf(existing?.fuelType.orEmpty()) }
    var brand by remember { mutableStateOf(existing?.brand.orEmpty()) }
    var productModel by remember { mutableStateOf(existing?.productModel.orEmpty()) }
    var reference by remember { mutableStateOf(existing?.reference.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var tyreSize by remember { mutableStateOf(existing?.tyreSize.orEmpty()) }
    var dot by remember { mutableStateOf(existing?.dot.orEmpty()) }
    var pressure by remember { mutableStateOf(existing?.recommendedPressureBar?.toString().orEmpty()) }
    var position by remember { mutableStateOf(existing?.position.orEmpty()) }
    var result by remember { mutableStateOf(existing?.result.orEmpty()) }
    var nextDate by remember { mutableStateOf(existing?.nextDate?.let(EsDateFormat::format).orEmpty()) }
    var minor by remember { mutableStateOf(existing?.minorDefects.orEmpty()) }
    var major by remember { mutableStateOf(existing?.majorDefects.orEmpty()) }
    var provider by remember { mutableStateOf(existing?.provider.orEmpty()) }
    var policy by remember { mutableStateOf(existing?.policy.orEmpty()) }
    var coverage by remember { mutableStateOf(existing?.coverage.orEmpty()) }
    var startDate by remember { mutableStateOf(existing?.startDate?.let(EsDateFormat::format).orEmpty()) }
    var endDate by remember { mutableStateOf(existing?.endDate?.let(EsDateFormat::format).orEmpty()) }
    var autoRenew by remember { mutableStateOf(existing?.autoRenew ?: false) }
    var assistance by remember { mutableStateOf(existing?.roadsideAssistance ?: false) }
    var taxYear by remember { mutableStateOf(existing?.taxYear?.toString().orEmpty()) }
    var paid by remember { mutableStateOf(existing?.paid ?: false) }
    var reminderByDate by remember { mutableStateOf(existing?.reminderByDate ?: (kind == RecordKind.REMINDER)) }
    var reminderByKm by remember { mutableStateOf(existing?.reminderByKm ?: false) }
    var leadKm by remember(existing?.id, prefs.distanceUnit) { mutableStateOf(existing?.reminderLeadKm?.let { UnitFormatters.editableDistance(it, prefs) }.orEmpty()) }
    var leadDays by remember { mutableStateOf(existing?.reminderLeadDays?.joinToString(",") ?: "30,7,1") }
    var confirmDelete by remember { mutableStateOf(false) }

    fun parseDate(raw: String): Date? = raw.takeIf(String::isNotBlank)?.let { runCatching { EsDateFormat.parse(it) }.getOrNull() }
    fun d(raw: String) = raw.replace(',', '.').toDoubleOrNull()
    fun defaultTitle(): String = when (kind) {
        RecordKind.REFUEL -> "Repostaje"
        RecordKind.INSPECTION -> "ITV"
        RecordKind.INSURANCE -> "Seguro"
        RecordKind.TAX -> "Impuesto ${taxYear.ifBlank { "" }}".trim()
        else -> category.ifBlank { kind.displayName }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (existing == null) "Nuevo · ${kind.displayName}" else "Editar · ${kind.displayName}") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }, actions = { if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Eliminar") } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { FieldR(title, { title = it }, "Título") }
            item { FieldR(date, { date = it }, "Fecha (DD/MM/AAAA)") }
            item { DecimalR(km, { km = it }, "Kilometraje (${UnitFormatters.distanceLabel(prefs)})") }

            when (kind) {
                RecordKind.MAINTENANCE -> {
                    item { ChoiceField("Tipo", category, maintenanceTypes) { category = it } }
                    item { FieldR(workshop, { workshop = it }, "Taller") }
                    item { FieldR(parts, { parts = it }, "Piezas utilizadas", false) }
                    item { DecimalR(partsCost, { partsCost = it }, "Coste piezas") }
                    item { DecimalR(laborCost, { laborCost = it }, "Mano de obra") }
                    item { DecimalR(cost, { cost = it }, "Coste total") }
                    item { DecimalR(nextDueKm, { nextDueKm = it }, "Próximo mantenimiento (${UnitFormatters.distanceLabel(prefs)})") }
                    item { FieldR(nextDueDate, { nextDueDate = it }, "Próximo mantenimiento (fecha)") }
                    item { ReminderFields(reminderByDate, { reminderByDate = it }, reminderByKm, { reminderByKm = it }, leadKm, { leadKm = it }, leadDays, { leadDays = it }, UnitFormatters.distanceLabel(prefs)) }
                }
                RecordKind.REPAIR -> {
                    item { ChoiceField("Estado", status, listOf("Pendiente", "Diagnosticando", "Esperando piezas", "Reparado")) { status = it } }
                    item { FieldR(fault, { fault = it }, "Avería", false) }
                    item { FieldR(symptoms, { symptoms = it }, "Síntomas", false) }
                    item { FieldR(diagnosis, { diagnosis = it }, "Diagnóstico", false) }
                    item { FieldR(repairAction, { repairAction = it }, "Reparación realizada", false) }
                    item { FieldR(parts, { parts = it }, "Piezas", false) }
                    item { FieldR(workshop, { workshop = it }, "Taller") }
                    item { DecimalR(partsCost, { partsCost = it }, "Coste piezas") }
                    item { DecimalR(laborCost, { laborCost = it }, "Coste mano de obra") }
                    item { DecimalR(cost, { cost = it }, "Coste total") }
                }
                RecordKind.REFUEL -> {
                    item { DecimalR(liters, { liters = it }, "${UnitFormatters.volumeLabel(prefs)} *") }
                    item { DecimalR(priceLiter, { priceLiter = it }, "Precio/${UnitFormatters.volumeLabel(prefs).lowercase()}") }
                    item { DecimalR(cost, { cost = it }, "Coste total") }
                    item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(fullTank, { fullTank = it }); Text("Depósito lleno") } }
                    item { FieldR(station, { station = it }, "Gasolinera") }
                    item { FieldR(fuelType, { fuelType = it }, "Tipo de combustible") }
                }
                RecordKind.EXPENSE -> { item { ChoiceField("Categoría", category, expenseCategories) { category = it } }; item { DecimalR(cost, { cost = it }, "Coste *") } }
                RecordKind.MODIFICATION -> {
                    item { ChoiceField("Categoría", category, modificationCategories) { category = it } }
                    item { ChoiceField("Estado", status, listOf("Instalada", "Pendiente", "Retirada")) { status = it } }
                    item { FieldR(brand, { brand = it }, "Marca") }; item { FieldR(productModel, { productModel = it }, "Modelo") }; item { FieldR(reference, { reference = it }, "Referencia") }
                    item { DecimalR(cost, { cost = it }, "Coste") }; item { FieldR(description, { description = it }, "Descripción", false) }
                }
                RecordKind.TYRE -> {
                    item { FieldR(brand, { brand = it }, "Marca") }; item { FieldR(productModel, { productModel = it }, "Modelo") }; item { FieldR(tyreSize, { tyreSize = it }, "Medida") }; item { FieldR(dot, { dot = it }, "DOT") }
                    item { DecimalR(pressure, { pressure = it }, "Presión recomendada (bar)") }; item { ChoiceField("Posición", position, listOf("Delantero izquierdo", "Delantero derecho", "Trasero izquierdo", "Trasero derecho")) { position = it } }; item { DecimalR(cost, { cost = it }, "Coste") }
                }
                RecordKind.INSPECTION -> {
                    item { ChoiceField("Resultado", result, listOf("Favorable", "Desfavorable", "Negativa")) { result = it } }; item { FieldR(station, { station = it }, "Estación") }; item { DecimalR(cost, { cost = it }, "Coste") }
                    item { FieldR(nextDate, { nextDate = it }, "Próxima fecha") }; item { FieldR(minor, { minor = it }, "Defectos leves", false) }; item { FieldR(major, { major = it }, "Defectos graves", false) }
                    item { ReminderFields(true, {}, false, {}, "", {}, leadDays, { leadDays = it }, UnitFormatters.distanceLabel(prefs)) }
                }
                RecordKind.INSURANCE -> {
                    item { FieldR(provider, { provider = it }, "Aseguradora") }; item { FieldR(policy, { policy = it }, "Póliza") }; item { FieldR(coverage, { coverage = it }, "Modalidad") }; item { FieldR(startDate, { startDate = it }, "Fecha inicio") }; item { FieldR(endDate, { endDate = it }, "Fecha vencimiento") }; item { DecimalR(cost, { cost = it }, "Coste") }
                    item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(autoRenew, { autoRenew = it }); Text("Renovación automática") } }; item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(assistance, { assistance = it }); Text("Asistencia") } }; item { ReminderFields(true, {}, false, {}, "", {}, leadDays, { leadDays = it }, UnitFormatters.distanceLabel(prefs)) }
                }
                RecordKind.TAX -> {
                    item { FieldR(category, { category = it }, "Tipo") }; item { NumberR(taxYear, { taxYear = it }, "Año") }; item { DecimalR(cost, { cost = it }, "Coste") }; item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(paid, { paid = it }); Text("Pagado") } }; item { FieldR(nextDueDate, { nextDueDate = it }, "Próximo vencimiento") }
                }
                RecordKind.PART -> { item { FieldR(brand, { brand = it }, "Marca") }; item { FieldR(productModel, { productModel = it }, "Modelo") }; item { FieldR(reference, { reference = it }, "Referencia") }; item { DecimalR(cost, { cost = it }, "Coste") }; item { FieldR(description, { description = it }, "Descripción", false) } }
                RecordKind.REMINDER -> {
                    item { ChoiceField("Categoría", category, listOf("Mantenimiento", "ITV", "Seguro", "Neumáticos", "Impuestos", "Personalizado")) { category = it } }
                    item { FieldR(nextDueDate, { nextDueDate = it }, "Fecha objetivo") }; item { DecimalR(nextDueKm, { nextDueKm = it }, "Kilometraje objetivo (${UnitFormatters.distanceLabel(prefs)})") }; item { ReminderFields(reminderByDate, { reminderByDate = it }, reminderByKm, { reminderByKm = it }, leadKm, { leadKm = it }, leadDays, { leadDays = it }, UnitFormatters.distanceLabel(prefs)) }
                }
                else -> Unit
            }
            item { FieldR(notes, { notes = it }, "Notas", false) }
            item {
                Button(onClick = {
                    val parsedDate = parseDate(date) ?: run { vm.showMessage("Fecha inválida. Usa DD/MM/AAAA"); return@Button }
                    val displayedVolume = d(liters)
                    val displayedUnitPrice = d(priceLiter)
                    val l = displayedVolume?.let { UnitFormatters.volumeToLiters(it, prefs) }
                    val pp = displayedUnitPrice?.let { UnitFormatters.pricePerDisplayedVolumeToPerLiter(it, prefs) }
                    val internalKm = d(km)?.let { UnitFormatters.distanceToKm(it, prefs) }
                    val internalNextKm = d(nextDueKm)?.let { UnitFormatters.distanceToKm(it, prefs) }
                    val internalLeadKm = d(leadKm)?.let { UnitFormatters.distanceToKm(it, prefs) }
                    val finalCost = d(cost) ?: if (kind == RecordKind.REFUEL && displayedVolume != null && displayedUnitPrice != null) displayedVolume * displayedUnitPrice else (d(partsCost) ?: 0.0) + (d(laborCost) ?: 0.0)
                    val record = (existing ?: GarageRecord(kind = kind)).copy(
                        kind = kind, title = title.trim().ifBlank { defaultTitle() }, date = parsedDate, odometerKm = internalKm, cost = finalCost, notes = notes.trim(), category = category, status = status,
                        workshop = workshop, parts = parts, laborCost = d(laborCost) ?: 0.0, partsCost = d(partsCost) ?: 0.0, nextDueKm = internalNextKm, nextDueDate = parseDate(nextDueDate),
                        fault = fault, symptoms = symptoms, diagnosis = diagnosis, repairAction = repairAction, liters = l, pricePerLiter = pp, fullTank = fullTank, station = station, fuelType = fuelType,
                        brand = brand, productModel = productModel, reference = reference, description = description, tyreSize = tyreSize, dot = dot, recommendedPressureBar = d(pressure), position = position,
                        installedDate = if (kind == RecordKind.TYRE) parsedDate else existing?.installedDate, installedKm = if (kind == RecordKind.TYRE) internalKm else existing?.installedKm,
                        result = result, nextDate = parseDate(nextDate), minorDefects = minor, majorDefects = major, provider = provider, policy = policy, coverage = coverage, startDate = parseDate(startDate), endDate = parseDate(endDate), autoRenew = autoRenew, roadsideAssistance = assistance,
                        taxYear = taxYear.toIntOrNull(), paid = paid, reminderByDate = reminderByDate || kind == RecordKind.INSPECTION || kind == RecordKind.INSURANCE, reminderByKm = reminderByKm, reminderLeadKm = internalLeadKm, reminderLeadDays = leadDays.split(',').mapNotNull { it.trim().toIntOrNull() }, reminderEnabled = true
                    )
                    vm.saveRecord(record, onSaved)
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Guardar") }
            }
        }
    }
    if (confirmDelete && existing != null) ConfirmDialog("Eliminar registro", "Este registro se eliminará del historial.", { vm.deleteRecord(existing); confirmDelete = false; onBack() }, { confirmDelete = false })
}

@Composable private fun ReminderFields(byDate: Boolean, setDate: (Boolean) -> Unit, byKm: Boolean, setKm: (Boolean) -> Unit, leadKm: String, setLeadKm: (String) -> Unit, leadDays: String, setLeadDays: (String) -> Unit, distanceLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Avisos", fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(byDate, setDate); Text("Por fecha") }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(byKm, setKm); Text("Por kilómetros") }
        if (byDate) FieldR(leadDays, setLeadDays, "Avisar días antes (ej. 30,7,1)")
        if (byKm) DecimalR(leadKm, setLeadKm, "Avisar $distanceLabel antes")
    }
}

@Composable private fun ChoiceField(label: String, value: String, options: List<String>, onValue: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(value, {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded, { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValue(option); expanded = false }) } }
    }
}
@Composable private fun FieldR(value: String, onValue: (String) -> Unit, label: String, singleLine: Boolean = true) = OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth())
@Composable private fun NumberR(value: String, onValue: (String) -> Unit, label: String) = OutlinedTextField(value, { onValue(it.filter(Char::isDigit)) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
@Composable private fun DecimalR(value: String, onValue: (String) -> Unit, label: String) = OutlinedTextField(value, { onValue(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

@Composable
private fun OdometerEditor(vehicle: Vehicle, vm: GarageViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val uiState by vm.state.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    var km by remember(vehicle.id, prefs.distanceUnit) { mutableStateOf(UnitFormatters.editableDistance(vehicle.odometerKm, prefs)) }
    var confirm by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Actualizar kilómetros") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Actual: ${UnitFormatters.formatDistance(vehicle.odometerKm, prefs)}", style = MaterialTheme.typography.titleLarge)
            DecimalR(km, { km = it }, "Nuevo kilometraje (${UnitFormatters.distanceLabel(prefs)})")
            val entered = km.replace(',', '.').toDoubleOrNull()?.let { UnitFormatters.distanceToKm(it, prefs) }
            if (entered != null && entered < vehicle.odometerKm) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(confirm, { confirm = it }); Text("Confirmo la reducción") }
            Button(onClick = { val value = km.replace(',', '.').toDoubleOrNull()?.let { UnitFormatters.distanceToKm(it, prefs) } ?: return@Button; vm.updateOdometer(value, confirm) { ok -> if (ok) onSaved() } }, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
        }
    }
}
