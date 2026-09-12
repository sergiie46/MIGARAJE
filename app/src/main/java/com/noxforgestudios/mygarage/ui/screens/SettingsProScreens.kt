package com.noxforgestudios.mygarage.ui.screens

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noxforgestudios.mygarage.AppContainer
import com.noxforgestudios.mygarage.BuildConfig
import com.noxforgestudios.mygarage.domain.*
import com.noxforgestudios.mygarage.ui.ConfirmDialog
import com.noxforgestudios.mygarage.ui.GarageUiState
import com.noxforgestudios.mygarage.ui.GarageViewModel
import java.text.DateFormat

@Composable
fun SettingsScreen(state: GarageUiState, vm: GarageViewModel, container: AppContainer, activity: Activity, onPro: () -> Unit, onPrivacy: () -> Unit) {
    var deleteConfirm by remember { mutableStateOf(false) }
    var themeMenu by remember { mutableStateOf(false) }
    var unitsDialog by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item { Section("Cuenta") }
        item { SettingsRow(Icons.Default.AccountCircle, state.user?.displayName ?: "Google", state.user?.email.orEmpty()) {} }
        item { SettingsRow(Icons.Default.CloudSync, "Estado de sincronización", state.lastSyncMillis?.let { "Última: ${DateFormat.getDateTimeInstance().format(java.util.Date(it))}" } ?: state.syncState.name) { vm.syncNow() } }
        item { SettingsRow(Icons.Default.WorkspacePremium, "Mi Garaje PRO", if (state.pro.isPro) "Activado · sin anuncios" else "FREE · máximo 2 vehículos activos") { onPro() } }
        item { Section("Preferencias") }
        item {
            Box { SettingsRow(Icons.Default.Palette, "Tema", state.preferences.themeMode.name.lowercase()) { themeMenu = true }; DropdownMenu(themeMenu, { themeMenu = false }) { ThemeMode.entries.forEach { mode -> DropdownMenuItem(text = { Text(mode.label()) }, onClick = { vm.setTheme(mode); themeMenu = false }) } } }
        }
        item {
            val p = state.preferences
            SettingsRow(Icons.Default.Straighten, "Unidades", "${p.distanceUnit.label()} · ${p.volumeUnit.label()} · ${p.consumptionUnit.label()} · ${p.currencyCode}") { unitsDialog = true }
        }
        item { ListItem(leadingContent = { Icon(Icons.Default.Notifications, null) }, headlineContent = { Text("Notificaciones") }, supportingContent = { Text("Recordatorios Mi Garaje") }, trailingContent = { Switch(state.preferences.notificationsEnabled, vm::setNotifications) }) }
        item { SettingsRow(Icons.Default.PrivacyTip, "Opciones de privacidad", "Consentimiento UMP") { onPrivacy() } }
        item { Section("Datos") }
        item { SettingsRow(Icons.Default.PictureAsPdf, "Exportar PDF", if (state.pro.isPro) "Compartir historial" else "Requiere PRO") { vm.exportPdf { vm.share(it, "application/pdf") } } }
        item { SettingsRow(Icons.Default.TableChart, "Exportar CSV", if (state.pro.isPro) "Compartir datos" else "Requiere PRO") { vm.exportCsv { vm.share(it, "text/csv") } } }
        item { SettingsRow(Icons.Default.Sync, "Sincronizar ahora", "Forzar envío de cambios pendientes") { vm.syncNow() } }
        item { Section("Sesión y privacidad") }
        item { SettingsRow(Icons.Default.Logout, "Cerrar sesión", state.user?.email.orEmpty()) { vm.signOut() } }
        item { SettingsRow(Icons.Default.DeleteForever, "Eliminar cuenta", "Borra vehículos, registros e identidad") { deleteConfirm = true } }
        item { Section("Acerca de") }
        item { SettingsRow(Icons.Default.Info, "Mi Garaje", "Versión ${BuildConfig.VERSION_NAME}") {} }
        item { Text("Contacto: ${BuildConfig.DEVELOPER_CONTACT}", style = MaterialTheme.typography.bodySmall) }
    }
    if (unitsDialog) UnitsDialog(state.preferences, vm, { unitsDialog = false })
    if (deleteConfirm) ConfirmDialog("Eliminar cuenta", "Se eliminarán tus vehículos, registros y perfil. Firebase puede pedir inicio de sesión reciente para borrar la identidad. Esta acción no se puede deshacer.", { deleteConfirm = false; vm.deleteAccount() }, { deleteConfirm = false })
}

@Composable
private fun UnitsDialog(prefs: AppPreferences, vm: GarageViewModel, onDismiss: () -> Unit) {
    var currency by remember(prefs.currencyCode) { mutableStateOf(prefs.currencyCode) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unidades y moneda") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OptionChips("Distancia", DistanceUnit.entries, prefs.distanceUnit, { it.label() }, vm::setDistanceUnit)
                OptionChips("Volumen", VolumeUnit.entries, prefs.volumeUnit, { it.label() }, vm::setVolumeUnit)
                OptionChips("Consumo", ConsumptionUnit.entries, prefs.consumptionUnit, { it.label() }, vm::setConsumptionUnit)
                Text("Moneda", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EUR", "USD", "GBP", "CHF").forEach { code ->
                        FilterChip(selected = currency == code, onClick = { currency = code; vm.setCurrency(code) }, label = { Text(code) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}

@Composable
private fun <T> OptionChips(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option -> FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) }) }
        }
    }
}

private fun ThemeMode.label() = when (this) { ThemeMode.SYSTEM -> "Sistema"; ThemeMode.LIGHT -> "Claro"; ThemeMode.DARK -> "Oscuro" }
private fun DistanceUnit.label() = if (this == DistanceUnit.KM) "km" else "millas"
private fun VolumeUnit.label() = if (this == VolumeUnit.LITERS) "litros" else "galones US"
private fun ConsumptionUnit.label() = if (this == ConsumptionUnit.L_PER_100_KM) "L/100 km" else "MPG US"

@Composable private fun Section(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)) }
@Composable private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = { if (subtitle.isNotBlank()) Text(subtitle) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
    )
}

@Composable
fun ProScreen(pro: ProState, onBack: () -> Unit, onBuy: () -> Unit, onRestore: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Mi Garaje PRO") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Default.WorkspacePremium, null, Modifier.size(78.dp), tint = MaterialTheme.colorScheme.primary)
            Text(if (pro.isPro) "PRO activado" else "Desbloquea todo tu garaje", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            listOf("Vehículos ilimitados", "Cero anuncios", "Estadísticas completas", "Exportación PDF profesional", "Exportación CSV", "Funciones avanzadas").forEach { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Text(it) } }
            if (!pro.isPro) Button(onClick = onBuy, enabled = pro.productAvailable && !pro.purchasePending, modifier = Modifier.fillMaxWidth()) { Text(if (pro.purchasePending) "Compra pendiente" else "Desbloquear PRO ${pro.priceText?.let { "· $it" }.orEmpty()}") }
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restaurar compra") }
            pro.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
