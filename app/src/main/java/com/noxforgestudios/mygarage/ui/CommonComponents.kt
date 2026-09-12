package com.noxforgestudios.mygarage.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.noxforgestudios.mygarage.domain.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.io.File
import java.util.*

val EsDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
val EsMoney: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
val IntFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale("es", "ES"))

@Composable
fun SyncStatusChip(state: SyncState, online: Boolean) {
    val (text, icon) = when {
        !online || state == SyncState.OFFLINE -> "Sin conexión" to Icons.Default.CloudOff
        state == SyncState.SYNCING -> "Sincronizando" to Icons.Default.Sync
        state == SyncState.ERROR -> "Error de sincronización" to Icons.Default.ErrorOutline
        state == SyncState.NOT_CONFIGURED -> "Firebase pendiente" to Icons.Default.CloudOff
        else -> "Sincronizado" to Icons.Default.CloudDone
    }
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, maxLines = 1, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (action != null && onAction != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
fun VehicleSummaryCard(vehicle: Vehicle, preferences: AppPreferences = AppPreferences(), onClick: () -> Unit = {}) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(92.dp)) {
                val local = vehicle.localPhotoPath?.takeIf { runCatching { File(it).exists() }.getOrDefault(false) }
                val model = local ?: vehicle.remotePhotoUrl
                if (!model.isNullOrBlank()) AsyncImage(model = model, contentDescription = "Foto de ${vehicle.title}", modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, Modifier.size(44.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(vehicle.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (vehicle.nickname.isNotBlank()) Text(vehicle.nickname, color = MaterialTheme.colorScheme.primary)
                Text(UnitFormatters.formatDistance(vehicle.odometerKm, preferences), style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(vehicle.year?.toString(), vehicle.plate.takeIf { it.isNotBlank() }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

fun recordIcon(kind: RecordKind): ImageVector = when (kind) {
    RecordKind.MAINTENANCE -> Icons.Default.Build
    RecordKind.REPAIR -> Icons.Default.Handyman
    RecordKind.REFUEL -> Icons.Default.LocalGasStation
    RecordKind.EXPENSE -> Icons.Default.Payments
    RecordKind.REMINDER -> Icons.Default.Notifications
    RecordKind.INSPECTION -> Icons.Default.FactCheck
    RecordKind.INSURANCE -> Icons.Default.Shield
    RecordKind.MODIFICATION -> Icons.Default.Tune
    RecordKind.PART -> Icons.Default.Settings
    RecordKind.TYRE -> Icons.Default.TireRepair
    RecordKind.TAX -> Icons.Default.ReceiptLong
    RecordKind.ODOMETER -> Icons.Default.Speed
}

@Composable
fun RecordRow(record: GarageRecord, preferences: AppPreferences = AppPreferences(), onClick: (() -> Unit)? = null) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        leadingContent = { Icon(recordIcon(record.kind), null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(record.title.ifBlank { record.kind.displayName }) },
        supportingContent = {
            val parts = mutableListOf(EsDateFormat.format(record.date))
            record.odometerKm?.let { parts += UnitFormatters.formatDistance(it, preferences) }
            if (record.category.isNotBlank()) parts += record.category
            Text(parts.joinToString(" · "), maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = { if (record.cost > 0) Text(UnitFormatters.money(record.cost, preferences.currencyCode), fontWeight = FontWeight.SemiBold) }
    )
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SimpleLineChart(values: List<Float>, modifier: Modifier = Modifier.height(170.dp).fillMaxWidth()) {
    if (values.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Aún no hay datos suficientes") }
        return
    }
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.padding(12.dp)) {
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, value ->
            val x = i * stepX
            val y = size.height - ((value - min) / range) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = {
        TextButton(onClick = onConfirm) { Text("Confirmar") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
