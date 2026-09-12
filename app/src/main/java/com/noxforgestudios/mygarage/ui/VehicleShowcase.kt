package com.noxforgestudios.mygarage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.noxforgestudios.mygarage.domain.Vehicle
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VehicleShowcase(vehicles: List<Vehicle>, selectedId: String?, onSelect: (String) -> Unit,
                    onOpen: () -> Unit, onShare: () -> Unit) {
    if (vehicles.isEmpty()) return
    val cars = remember(vehicles) { vehicles.filter { !it.archived }.ifEmpty { vehicles }.sortedBy { it.id } }
    val pager = rememberPagerState(initialPage = cars.indexOfFirst { it.id == selectedId }.coerceAtLeast(0), pageCount = { cars.size })
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)
    LaunchedEffect(pager, cars.map { it.id }) {
        snapshotFlow { pager.settledPage }.collect { page -> cars.getOrNull(page)?.let { select(it.id) } }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("MI GARAJE", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${pager.currentPage + 1} / ${cars.size}", style = MaterialTheme.typography.labelLarge)
        }
        HorizontalPager(state = pager, key = { cars[it].id }, pageSpacing = 12.dp) { page ->
            VehicleHeroCard(cars[page])
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(enabled = pager.currentPage > 0, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) { Icon(Icons.Default.ChevronLeft, "Vehículo anterior") }
            Text(if (cars.size > 1) "Desliza para cambiar de coche" else "Tu coche, todos sus detalles", style = MaterialTheme.typography.bodySmall)
            IconButton(enabled = pager.currentPage < cars.lastIndex, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Icon(Icons.Default.ChevronRight, "Siguiente vehículo") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onOpen, enabled = !pager.isScrollInProgress, modifier = Modifier.weight(1f)) { Text("Ver vehículo") }
            OutlinedButton(onClick = onShare, enabled = !pager.isScrollInProgress, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Enseñar ficha") }
        }
    }
}

@Composable
fun VehicleHeroCard(vehicle: Vehicle) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(300.dp).background(Color(0xFF202020))) {
            val photo = vehicle.localPhotoPath?.let(::File)?.takeIf { it.isFile } ?: vehicle.remotePhotoUrl
            if (photo != null) AsyncImage(photo, "${vehicle.title}, foto principal", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.DirectionsCar, null, Modifier.size(144.dp).align(Alignment.Center), tint = Color(0xFF606060))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE101010)))))
            Surface(Modifier.align(Alignment.TopStart).padding(18.dp), color = Color(0xCC161616), shape = RoundedCornerShape(50)) {
                Text(vehicle.status.name.replace('_', ' '), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(vehicle.make.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color(0xFF78B5FF))
                Text(vehicle.model, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text(listOf(vehicle.generation, vehicle.version, vehicle.year?.toString().orEmpty()).filter(String::isNotBlank).joinToString(" · "), color = Color(0xFFDDDDDD))
                if (vehicle.nickname.isNotBlank()) Text(vehicle.nickname, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroSpec("POTENCIA", vehicle.powerCv?.let { "$it CV" } ?: vehicle.powerKw?.let { "$it kW" } ?: "—", Modifier.weight(1f))
                HeroSpec("MOTOR", vehicle.displacementCc?.let { "$it cc" } ?: "—", Modifier.weight(1f))
                HeroSpec("TRACCIÓN", vehicle.traction.ifBlank { "—" }, Modifier.weight(1f))
            }
            HorizontalDivider(color = Color(0xFF333333))
            Text(listOf(vehicle.fuel, vehicle.transmission).filter(String::isNotBlank).joinToString(" · ").ifBlank { "Añade las especificaciones al editar tu coche" }, color = Color(0xFFCCCCCC), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HeroSpec(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Color(0xFF999999), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
