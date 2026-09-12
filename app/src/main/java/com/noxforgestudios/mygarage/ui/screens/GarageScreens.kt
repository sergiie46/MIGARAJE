@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.noxforgestudios.mygarage.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import com.noxforgestudios.mygarage.domain.VehicleCatalog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.noxforgestudios.mygarage.AppContainer
import com.noxforgestudios.mygarage.domain.Vehicle
import com.noxforgestudios.mygarage.domain.VehicleStatus
import com.noxforgestudios.mygarage.ui.*
import kotlinx.coroutines.launch
import java.util.Date
import java.io.File

@Composable
fun GarageScreen(
    state: GarageUiState,
    onAdd: () -> Unit,
    onSelect: (Vehicle) -> Unit,
    onEdit: (Vehicle) -> Unit,
    onDuplicate: (Vehicle) -> Unit,
    onArchive: (Vehicle) -> Unit,
    onDelete: (Vehicle) -> Unit
) {
    var deleteVehicle by remember { mutableStateOf<Vehicle?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Mis vehículos") }, actions = { IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Añadir vehículo") } }) }) { pad ->
        if (state.vehicles.isEmpty()) Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { EmptyState(Icons.Default.DirectionsCar, "No tienes vehículos todavía.", "Añadir mi primer vehículo", onAdd) }
        else LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.vehicles, key = { it.id }) { vehicle ->
                ElevatedCard(onClick = { onSelect(vehicle) }, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        VehicleSummaryCard(vehicle, state.preferences) { onSelect(vehicle) }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onEdit(vehicle) }) { Icon(Icons.Default.Edit, null); Text("Editar") }
                            var menu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más") }
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem(text = { Text("Duplicar") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = { menu = false; onDuplicate(vehicle) })
                                    DropdownMenuItem(text = { Text(if (vehicle.archived) "Desarchivar" else "Archivar") }, leadingIcon = { Icon(Icons.Default.Archive, null) }, onClick = { menu = false; onArchive(vehicle.copy(archived = !vehicle.archived)) })
                                    DropdownMenuItem(text = { Text("Eliminar") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menu = false; deleteVehicle = vehicle })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    deleteVehicle?.let { v -> ConfirmDialog("Eliminar vehículo", "Se eliminarán el vehículo y todos sus registros de Firestore. Esta acción no se puede deshacer.", { onDelete(v); deleteVehicle = null }, { deleteVehicle = null }) }
}

@Composable
fun VehicleEditorScreen(vehicle: Vehicle?, vm: GarageViewModel, container: AppContainer, onBack: () -> Unit, onSaved: () -> Unit) {
    val uiState by vm.state.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    var make by rememberSaveable { mutableStateOf(vehicle?.make.orEmpty()) }
    var model by rememberSaveable { mutableStateOf(vehicle?.model.orEmpty()) }
    var generation by rememberSaveable { mutableStateOf(vehicle?.generation.orEmpty()) }
    var version by rememberSaveable { mutableStateOf(vehicle?.version.orEmpty()) }
    var year by rememberSaveable { mutableStateOf(vehicle?.year?.toString().orEmpty()) }
    var plate by rememberSaveable { mutableStateOf(vehicle?.plate.orEmpty()) }
    var vin by rememberSaveable { mutableStateOf(vehicle?.vin.orEmpty()) }
    var odometer by rememberSaveable(vehicle?.id, prefs.distanceUnit) { mutableStateOf(vehicle?.odometerKm?.let { UnitFormatters.editableDistance(it, prefs) } ?: "") }
    var purchaseDate by rememberSaveable { mutableStateOf(vehicle?.purchaseDate?.let(EsDateFormat::format).orEmpty()) }
    var purchasePrice by rememberSaveable { mutableStateOf(vehicle?.purchasePrice?.toString().orEmpty()) }
    var fuel by rememberSaveable { mutableStateOf(vehicle?.fuel.orEmpty()) }
    var displacement by rememberSaveable { mutableStateOf(vehicle?.displacementCc?.toString().orEmpty()) }
    var powerCv by rememberSaveable { mutableStateOf(vehicle?.powerCv?.toString().orEmpty()) }
    var powerKw by rememberSaveable { mutableStateOf(vehicle?.powerKw?.toString().orEmpty()) }
    var transmission by rememberSaveable { mutableStateOf(vehicle?.transmission.orEmpty()) }
    var traction by rememberSaveable { mutableStateOf(vehicle?.traction.orEmpty()) }
    var color by rememberSaveable { mutableStateOf(vehicle?.color.orEmpty()) }
    var nickname by rememberSaveable { mutableStateOf(vehicle?.nickname.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(vehicle?.notes.orEmpty()) }
    var status by rememberSaveable { mutableStateOf(vehicle?.status ?: VehicleStatus.ACTUAL) }
    var croppedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var galleryUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var statusMenu by rememberSaveable { mutableStateOf(false) }

    var makes by remember { mutableStateOf(VehicleCatalog.models.keys.toList()) }
    var models by remember { mutableStateOf(VehicleCatalog.modelsFor(make)) }
    var loadingModels by remember { mutableStateOf(false) }
    var keptPhotos by rememberSaveable { mutableStateOf(vehicle?.galleryLocalPaths.orEmpty()) }
    var keptVideos by rememberSaveable { mutableStateOf(vehicle?.videoLocalPaths.orEmpty()) }
    LaunchedEffect(Unit) { makes = container.vehicleCatalog.makes() }
    LaunchedEffect(make) {
        models = VehicleCatalog.modelsFor(make)
        if (make.isNotBlank()) {
            loadingModels = true
            try { models = container.vehicleCatalog.models(make) } finally { loadingModels = false }
        }
    }
    fun safely(action: () -> Unit) { try { action() } catch (e: Exception) { vm.showMessage("No se pudo abrir el selector: "+e.message) } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) croppedUri = uri }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
        galleryUris = (galleryUris + uris).distinct().take(8)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) croppedUri = cameraUri }

    Scaffold(topBar = { TopAppBar(title = { Text(if (vehicle == null) "Nuevo vehículo" else "Editar vehículo") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val local = vehicle?.localPhotoPath?.takeIf { runCatching { File(it).exists() }.getOrDefault(false) }
                        val photoModel: Any? = croppedUri ?: local?.let(::File) ?: vehicle?.remotePhotoUrl
                        if (photoModel != null) AsyncImage(model = photoModel, contentDescription = "Foto del vehículo", modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
                        else Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, Modifier.size(72.dp)) }
                        Row { TextButton(onClick = { safely { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }) { Icon(Icons.Default.PhotoLibrary, null); Text("Foto principal") }; TextButton(onClick = { safely { val uri = container.photoRepository.createCameraUri(); cameraUri = uri; camera.launch(uri) } }) { Icon(Icons.Default.PhotoCamera, null); Text("Cámara") } }
                        TextButton(onClick = { safely { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) } }) {
                            Icon(Icons.Default.Collections, null); Text("Añadir fotos o vídeos")
                        }
                        Text("Hasta 6 fotos y 2 vídeos · vídeos de 2 min / 100 MB", style = MaterialTheme.typography.bodySmall)
                        Text("Los archivos se conservan en este dispositivo.", style = MaterialTheme.typography.bodySmall)
                        if (galleryUris.isNotEmpty()) {
                            Text("${galleryUris.size} archivos nuevos seleccionados")
                            TextButton(onClick = { galleryUris = emptyList() }) { Text("Quitar archivos nuevos") }
                        }
                        keptPhotos.forEachIndexed { index, path ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(File(path), "Foto ${index + 1}", Modifier.size(64.dp), contentScale = ContentScale.Crop)
                                Text("Foto ${index + 1}", Modifier.weight(1f).padding(8.dp))
                                IconButton(onClick = { keptPhotos = keptPhotos - path }) { Icon(Icons.Default.Close, "Quitar foto ${index + 1}") }
                            }
                        }
                        keptVideos.forEachIndexed { index, path ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Movie, null)
                                Text("Vídeo ${index + 1}", Modifier.weight(1f).padding(8.dp))
                                IconButton(onClick = { keptVideos = keptVideos - path }) { Icon(Icons.Default.Close, "Quitar vídeo ${index + 1}") }
                            }
                        }
                    }
                }
            }
            item { Text("Identificación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { SelectionField("Marca *", make, makes) { if (make != it) { make = it; model = ""; generation = "" } } }
            item { SelectionField("Modelo *", model, models, enabled = make.isNotBlank(), loading = loadingModels) { if (model != it) { model = it; generation = "" } } }
            item { SelectionField("Generación", generation, VehicleCatalog.generationsFor(make, model)) { generation = it } }
            item { Field(version, { version = it }, "Versión") }
            item { SelectionField("Año", year, (java.time.Year.now().value + 1 downTo 1886).map(Int::toString), allowCustom = false) { year = it } }
            item { Field(plate, { plate = it.uppercase() }, "Matrícula") }
            item { Field(vin, { vin = it.uppercase() }, "VIN opcional") }
            item { DecimalField(odometer, { odometer = it }, "Kilometraje actual (${UnitFormatters.distanceLabel(prefs)}) *") }
            item { Field(nickname, { nickname = it }, "Apodo") }
            item { Text("Compra y técnica", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { DateSelectionField("Fecha de compra", purchaseDate) { purchaseDate = it } }
            item { DecimalField(purchasePrice, { purchasePrice = it }, "Precio de compra") }
            item { SelectionField("Combustible", fuel, VehicleCatalog.fuels) { fuel = it } }
            item { NumberField(displacement, { displacement = it }, "Cilindrada (cc)") }
            item { NumberField(powerCv, { powerCv = it }, "Potencia (CV)") }
            item { NumberField(powerKw, { powerKw = it }, "Potencia (kW)") }
            item { SelectionField("Transmisión", transmission, VehicleCatalog.transmissions) { transmission = it } }
            item { SelectionField("Tracción", traction, VehicleCatalog.drivetrains) { traction = it } }
            item { SelectionField("Color", color, listOf("Negro", "Blanco", "Gris", "Plata", "Azul", "Rojo", "Verde", "Amarillo", "Naranja", "Morado", "Beige", "Marrón", "Dorado", "Bicolor", "Vinilado")) { color = it } }
            item {
                ExposedDropdownMenuBox(expanded = statusMenu, onExpandedChange = { statusMenu = it }) {
                    OutlinedTextField(value = status.name.lowercase().replaceFirstChar(Char::uppercase), onValueChange = {}, readOnly = true, label = { Text("Estado") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) { VehicleStatus.entries.forEach { s -> DropdownMenuItem(text = { Text(s.name.lowercase().replaceFirstChar(Char::uppercase)) }, onClick = { status = s; statusMenu = false }) } }
                }
            }
            item { Field(notes, { notes = it }, "Notas", singleLine = false) }
            item {
                Button(onClick = {
                    val odometerInternal = odometer.replace(',', '.').toDoubleOrNull()?.let { UnitFormatters.distanceToKm(it, prefs) }
                    if (make.isBlank() || model.isBlank() || odometerInternal == null) { vm.showMessage("Marca, modelo y kilometraje son obligatorios"); return@Button }
                    val base = (vehicle ?: Vehicle()).copy(
                        make = make.trim(), model = model.trim(), generation = generation.trim(), version = version.trim(), year = year.toIntOrNull(), plate = plate.trim(), vin = vin.trim(), odometerKm = odometerInternal,
                        purchaseDate = purchaseDate.takeIf(String::isNotBlank)?.let { runCatching { EsDateFormat.parse(it) }.getOrNull() }, purchasePrice = purchasePrice.replace(',', '.').toDoubleOrNull(), fuel = fuel.trim(),
                        displacementCc = displacement.toIntOrNull(), powerCv = powerCv.toIntOrNull(), powerKw = powerKw.toIntOrNull(), transmission = transmission.trim(), traction = traction.trim(), color = color.trim(), nickname = nickname.trim(), notes = notes.trim(), status = status
                    )
                    vm.saveVehicleWithMedia(base.copy(galleryLocalPaths = keptPhotos, videoLocalPaths = keptVideos), croppedUri, galleryUris, onSaved)
                }, enabled = !uiState.savingVehicle, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    if (uiState.savingVehicle) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                    Text(if (uiState.savingVehicle) "Guardando archivos y vehículo…" else "Guardar vehículo")
                }
            }
        }
    }
}

@Composable private fun Field(value: String, onValue: (String) -> Unit, label: String, singleLine: Boolean = true) = OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth())
@Composable private fun NumberField(value: String, onValue: (String) -> Unit, label: String) = OutlinedTextField(value, { onValue(it.filter(Char::isDigit)) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
@Composable private fun DecimalField(value: String, onValue: (String) -> Unit, label: String) = OutlinedTextField(value, { onValue(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
