package com.noxforgestudios.mygarage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.noxforgestudios.mygarage.export.VehicleCardGenerator
import com.noxforgestudios.mygarage.ui.GarageUiState
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VehicleCardScreen(state: GarageUiState, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vehicle = state.selectedVehicle
    var style by remember { mutableStateOf(VehicleCardGenerator.Style.MODERNO) }
    var includeSpecs by remember { mutableStateOf(true) }
    var includeMods by remember { mutableStateOf(true) }
    var output by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun generate() {
        val v = vehicle ?: return
        busy = true
        scope.launch {
            VehicleCardGenerator.generate(context, v, state.records, VehicleCardGenerator.Options(style, includeSpecs, includeMods))
                .onSuccess { output = it }
                .onFailure { message = it.message ?: "No se pudo generar la ficha" }
            busy = false
        }
    }

    if (message != null) AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("Aceptar") } }, text = { Text(message.orEmpty()) })

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Generar ficha del coche") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } }
        )
    }) { pad ->
        if (vehicle == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text("Selecciona un vehículo") }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(vehicle.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Crea una imagen lista para WhatsApp, Instagram o enseñarla rápidamente con las especificaciones y modificaciones de este vehículo.")
                    }
                }
            }
            item {
                Text("Estilo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleCardGenerator.Style.entries.forEach { item ->
                        FilterChip(selected = style == item, onClick = { style = item }, label = { Text(item.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Incluir especificaciones", Modifier.weight(1f)); Switch(includeSpecs, { includeSpecs = it }) }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Incluir modificaciones", Modifier.weight(1f)); Switch(includeMods, { includeMods = it }) }
                        Text("Fotos disponibles: ${listOfNotNull(vehicle.localPhotoPath).size + vehicle.galleryLocalPaths.size}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Button(onClick = ::generate, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp)); Text(if (busy) "Generando…" else "Generar imagen")
                }
            }
            output?.let { file ->
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        AsyncImage(model = file, contentDescription = "Ficha generada", modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                VehicleCardGenerator.saveToGallery(context, file)
                                    .onSuccess { message = "Imagen guardada en tu dispositivo" }
                                    .onFailure { message = it.message ?: "No se pudo guardar" }
                            }
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Guardar") }
                        Button(onClick = { VehicleCardGenerator.share(context, file) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Compartir") }
                    }
                }
            }
        }
    }
}
