package com.noxforgestudios.mygarage.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.noxforgestudios.mygarage.domain.Vehicle
import java.io.File

@Composable
fun VehicleGallery(vehicle: Vehicle) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var enlarged by remember { mutableStateOf<File?>(null) }
    if (vehicle.galleryLocalPaths.isEmpty() && vehicle.videoLocalPaths.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Fotos y vídeos", style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vehicle.galleryLocalPaths) { path ->
                val file = File(path)
                Card(onClick = { if (file.isFile) enlarged = file else error = "Esta foto está en el dispositivo donde la guardaste" }) {
                    AsyncImage(file, "Ampliar foto de ${vehicle.model}", Modifier.size(160.dp, 110.dp), contentScale = ContentScale.Crop)
                }
            }
        }
        vehicle.videoLocalPaths.forEachIndexed { index, path ->
            OutlinedButton(onClick = {
                try {
                    val file = File(path)
                    check(file.isFile) { "Este vídeo está en el dispositivo donde lo guardaste" }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val mime = when (file.extension) { "webm" -> "video/webm"; "3gp" -> "video/3gpp"; else -> "video/mp4" }
                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                } catch (e: android.content.ActivityNotFoundException) { error = "Instala un reproductor de vídeo para abrir este archivo"
                } catch (e: Exception) { error = e.message ?: "No se pudo abrir el vídeo" }
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text("Reproducir vídeo ${index + 1}") }
        }
    }
    enlarged?.let { file -> AlertDialog(onDismissRequest = { enlarged = null }, text = { AsyncImage(file, "Foto del coche", Modifier.fillMaxWidth().height(360.dp), contentScale = ContentScale.Fit) }, confirmButton = { TextButton(onClick = { enlarged = null }) { Text("Cerrar") } }) }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Aceptar") } }) }
}
