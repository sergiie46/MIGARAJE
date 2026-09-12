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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.Guidelines
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
    val scope = rememberCoroutineScope()
    var make by remember { mutableStateOf(vehicle?.make.orEmpty()) }
    var model by remember { mutableStateOf(vehicle?.model.orEmpty()) }
    var generation by remember { mutableStateOf(vehicle?.generation.orEmpty()) }
    var version by remember { mutableStateOf(vehicle?.version.orEmpty()) }
    var year by remember { mutableStateOf(vehicle?.year?.toString().orEmpty()) }
    var plate by remember { mutableStateOf(vehicle?.plate.orEmpty()) }
    var vin by remember { mutableStateOf(vehicle?.vin.orEmpty()) }
    var odometer by remember(vehicle?.id, prefs.distanceUnit) { mutableStateOf(vehicle?.odometerKm?.let { UnitFormatters.editableDistance(it, prefs) } ?: "") }
    var purchaseDate by remember { mutableStateOf(vehicle?.purchaseDate?.let(EsDateFormat::format).orEmpty()) }
    var purchasePrice by remember { mutableStateOf(vehicle?.purchasePrice?.toString().orEmpty()) }
    var fuel by remember { mutableStateOf(vehicle?.fuel.orEmpty()) }
    var displacement by remember { mutableStateOf(vehicle?.displacementCc?.toString().orEmpty()) }
    var powerCv by remember { mutableStateOf(vehicle?.powerCv?.toString().orEmpty()) }
    var powerKw by remember { mutableStateOf(vehicle?.powerKw?.toString().orEmpty()) }
    var transmission by remember { mutableStateOf(vehicle?.transmission.orEmpty()) }
    var traction by remember { mutableStateOf(vehicle?.traction.orEmpty()) }
    var color by remember { mutableStateOf(vehicle?.color.orEmpty()) }
    var nickname by remember { mutableStateOf(vehicle?.nickname.orEmpty()) }
    var notes by remember { mutableStateOf(vehicle?.notes.orEmpty()) }
    var status by remember { mutableStateOf(vehicle?.status ?: VehicleStatus.ACTUAL) }
    var croppedUri by remember { mutableStateOf<Uri?>(null) }
    var galleryUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var statusMenu by remember { mutableStateOf(false) }

    val crop = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) croppedUri = result.uriContent else result.error?.message?.let(vm::showMessage)
    }
    fun launchCrop(uri: Uri) = crop.launch(CropImageContractOptions(uri = uri, cropImageOptions = CropImageOptions(guidelines = Guidelines.ON, fixAspectRatio = true, aspectRatioX = 16, aspectRatioY = 9)))
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) launchCrop(uri) }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris -> galleryUris = uris.take(6) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let(::launchCrop) }

    Scaffold(topBar = { TopAppBar(title = { Text(if (vehicle == null) "Nuevo vehículo" else "Editar vehículo") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val local = vehicle?.localPhotoPath?.takeIf { runCatching { File(it).exists() }.getOrDefault(false) }
                        val photoModel: Any? = croppedUri ?: local ?: vehicle?.remotePhotoUrl
                        if (photoModel != null) AsyncImage(model = photoModel, contentDescription = "Foto del vehículo", modifier = Modifier.fillMaxWidth().height(190.dp))
                        else Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, Modifier.size(72.dp)) }
                        Row { TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Default.PhotoLibrary, null); Text("Foto principal") }; TextButton(onClick = { cameraUri = container.photoRepository.createCameraUri(); camera.launch(cameraUri!!) }) { Icon(Icons.Default.PhotoCamera, null); Text("Cámara") } }
                        TextButton(onClick = { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Default.Collections, null); Text(if (galleryUris.isEmpty()) "Añadir galería (hasta 6)" else "${galleryUris.size} fotos extra seleccionadas") }
                        if (vehicle?.galleryLocalPaths?.isNotEmpty() == true && galleryUris.isEmpty()) Text("${vehicle.galleryLocalPaths.size} fotos extra guardadas", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Text("Identificación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { Field(make, { make = it }, "Marca *") }
            item { Field(model, { model = it }, "Modelo *") }
            item { Field(generation, { generation = it }, "Generación") }
            item { Field(version, { version = it }, "Versión") }
            item { NumberField(year, { year = it }, "Año") }
            item { Field(plate, { plate = it.uppercase() }, "Matrícula") }
            item { Field(vin, { vin = it.uppercase() }, "VIN opcional") }
            item { DecimalField(odometer, { odometer = it }, "Kilometraje actual (${UnitFormatters.distanceLabel(prefs)}) *") }
            item { Field(nickname, { nickname = it }, "Apodo") }
            item { Text("Compra y técnica", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { Field(purchaseDate, { purchaseDate = it }, "Fecha de compra (DD/MM/AAAA)") }
            item { DecimalField(purchasePrice, { purchasePrice = it }, "Precio de compra") }
            item { Field(fuel, { fuel = it }, "Combustible") }
            item { NumberField(displacement, { displacement = it }, "Cilindrada (cc)") }
            item { NumberField(powerCv, { powerCv = it }, "Potencia (CV)") }
            item { NumberField(powerKw, { powerKw = it }, "Potencia (kW)") }
            item { Field(transmission, { transmission = it }, "Transmisión") }
            item { Field(traction, { traction = it }, "Tracción") }
            item { Field(color, { color = it }, "Color") }
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
                    vm.saveVehicle(base) { id ->
                        if (id == null) return@saveVehicle
                        val mainUri = croppedUri
                        val extraUris = galleryUris
                        if (mainUri == null && extraUris.isEmpty()) onSaved() else scope.launch {
                            var updated = base.copy(id = id)
                            if (mainUri != null) {
                                container.photoRepository.persistVehiclePhoto(mainUri, vm.state.value.user?.uid.orEmpty(), id)
                                    .onSuccess { (local, remote) -> updated = updated.copy(localPhotoPath = local, remotePhotoUrl = remote) }
                                    .onFailure { vm.showMessage("Vehículo guardado, pero la foto principal falló: ${it.message}") }
                            }
                            if (extraUris.isNotEmpty()) {
                                container.photoRepository.persistVehiclePhotos(extraUris, vm.state.value.user?.uid.orEmpty(), id)
                                    .onSuccess { (locals, remotes) -> updated = updated.copy(galleryLocalPaths = locals, galleryRemoteUrls = remotes) }
                                    .onFailure { vm.showMessage("Vehículo guardado, pero la galería falló: ${it.message}") }
                            }
                            vm.saveVehicle(updated) { onSaved() }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Guardar vehículo") }
            }
        }
    }
}

@Composable private fun Field(value: String, onValue: (String) -> Unit, label: String, singleLine: Boolean = true) = OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth())
@Composable private fun NumberField(value: String, onValue: (String) -> Unit, label: String) = OutlinedTextField(value, { onValue(it.filter(Char::isDigit)) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
@Composable private fun DecimalField(value: String, onValue: (String) -> Unit, label: String) = OutlinedTextField(value, { onValue(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
