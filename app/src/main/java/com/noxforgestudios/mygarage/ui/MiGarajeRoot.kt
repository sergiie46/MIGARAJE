package com.noxforgestudios.mygarage.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.noxforgestudios.mygarage.AppContainer
import com.noxforgestudios.mygarage.ads.BannerAd
import com.noxforgestudios.mygarage.R
import com.noxforgestudios.mygarage.domain.RecordKind
import com.noxforgestudios.mygarage.ui.screens.*

private data class BottomDest(val route: String, val labelRes: Int, val icon: ImageVector)
private val bottom = listOf(
    BottomDest("home", R.string.home, Icons.Default.Home),
    BottomDest("garage", R.string.garage, Icons.Default.DirectionsCar),
    BottomDest("history", R.string.history, Icons.Default.History),
    BottomDest("stats", R.string.statistics, Icons.Default.BarChart),
    BottomDest("settings", R.string.settings, Icons.Default.Settings)
)

@Composable
fun MiGarajeRoot(vm: GarageViewModel, container: AppContainer, activity: Activity) {
    val state by vm.state.collectAsStateWithLifecycle()
    var legalTitle by remember { mutableStateOf<String?>(null) }
    var legalText by remember { mutableStateOf<String?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) vm.setNotifications(false)
    }
    LaunchedEffect(state.user?.uid, state.preferences.onboardingDone, state.preferences.notificationsEnabled) {
        if (state.user != null && state.preferences.onboardingDone && state.preferences.notificationsEnabled && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (state.message != null) {
        AlertDialog(onDismissRequest = vm::clearMessage, confirmButton = { TextButton(onClick = vm::clearMessage) { Text("Aceptar") } }, text = { Text(state.message.orEmpty()) })
    }
    if (legalTitle != null) {
        AlertDialog(onDismissRequest = { legalTitle = null }, title = { Text(legalTitle.orEmpty()) }, text = { Text(legalText.orEmpty()) }, confirmButton = { TextButton(onClick = { legalTitle = null }) { Text("Cerrar") } })
    }

    when {
        state.user == null -> LoginScreen(
            activity = activity,
            firebaseConfigured = state.firebaseConfigured,
            loading = state.loading,
            onLogin = { vm.signIn(activity) },
            onPrivacy = { legalTitle = "Política de privacidad"; legalText = LegalText.privacy },
            onTerms = { legalTitle = "Términos"; legalText = LegalText.terms },
            onConsent = { container.consentManager.showPrivacyOptions(activity) { it?.let(vm::showMessage) } }
        )
        !state.preferences.onboardingDone -> OnboardingScreen(onDone = vm::setOnboardingDone)
        else -> MainNavigation(vm, container, activity)
    }
}

@Composable
private fun MainNavigation(vm: GarageViewModel, container: AppContainer, activity: Activity) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottom = bottom.any { current == it.route }
    val canRequestAds by container.adManager.canRequestAds.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (showBottom) Column {
                if (!state.pro.isPro && canRequestAds && current != "settings") BannerAd()
                NavigationBar {
                    bottom.forEach { dest ->
                        NavigationBarItem(
                            selected = current == dest.route,
                            onClick = { nav.navigate(dest.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(dest.icon, null) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (current == "home" && state.selectedVehicle != null) FloatingActionButton(onClick = { nav.navigate("quickAdd") }) { Icon(Icons.Default.Add, "Añadir registro") }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(state, onAddVehicle = { nav.navigate("vehicle/new") }, onSelectVehicle = vm::selectVehicle, onShareVehicle = { nav.navigate("vehicleCard") }, onVehicle = { nav.navigate("vehicleDetail") }, onUpdateKm = { km, force -> vm.updateOdometer(km, force) }, onSearch = { nav.navigate("search") }, onQuick = { nav.navigate("quickAdd") }, onRecord = { r -> nav.navigate("record/${r.kind.name}/${r.id}") }) }
            composable("garage") { GarageScreen(state, onAdd = { if (!state.pro.isPro && state.vehicles.count { !it.archived } >= 2) nav.navigate("pro") else nav.navigate("vehicle/new") }, onSelect = { v -> vm.selectVehicle(v.id); nav.navigate("vehicleDetail") }, onEdit = { nav.navigate("vehicle/${it.id}") }, onDuplicate = vm::duplicateVehicle, onArchive = { vm.saveVehicle(it) }, onDelete = vm::deleteVehicle) }
            composable("history") { HistoryScreen(state, onRecord = { r -> nav.navigate("record/${r.kind.name}/${r.id}") }, onAddKind = { k -> nav.navigate("record/${k.name}/new") }) }
            composable("stats") { StatsScreen(state) }
            composable("settings") { SettingsScreen(state, vm, container, activity, onPro = { nav.navigate("pro") }, onPrivacy = { container.consentManager.showPrivacyOptions(activity) { it?.let(vm::showMessage) } }) }
            composable("vehicle/new") { VehicleEditorScreen(null, vm, container, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() }) }
            composable("vehicle/{id}") { entry -> VehicleEditorScreen(state.vehicles.firstOrNull { it.id == entry.arguments?.getString("id") }, vm, container, { nav.popBackStack() }, { nav.popBackStack() }) }
            composable("vehicleDetail") { VehicleDetailScreen(state, onBack = { nav.popBackStack() }, onEditVehicle = { state.selectedVehicle?.let { nav.navigate("vehicle/${it.id}") } }, onGenerateCard = { nav.navigate("vehicleCard") }, onKind = { nav.navigate("records/${it.name}") }) }
            composable("vehicleCard") { VehicleCardScreen(state, onBack = { nav.popBackStack() }) }
            composable("records/{kind}") { entry -> val kind = runCatching { RecordKind.valueOf(entry.arguments?.getString("kind").orEmpty()) }.getOrDefault(RecordKind.EXPENSE); RecordListScreen(state, kind, onBack = { nav.popBackStack() }, onAdd = { nav.navigate("record/${kind.name}/new") }, onRecord = { nav.navigate("record/${kind.name}/${it.id}") }, onRotateTyres = vm::rotateTyres) }
            composable("record/{kind}/{id}") { entry ->
                val kind = runCatching { RecordKind.valueOf(entry.arguments?.getString("kind").orEmpty()) }.getOrDefault(RecordKind.EXPENSE)
                val id = entry.arguments?.getString("id").orEmpty()
                RecordEditorScreen(kind, state.records.firstOrNull { it.id == id && it.kind == kind }, state.selectedVehicle, vm, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable("quickAdd") { QuickAddScreen(onBack = { nav.popBackStack() }, onKind = { nav.navigate("record/${it.name}/new") }) }
            composable("search") { SearchScreen(state, onBack = { nav.popBackStack() }, onRecord = { nav.navigate("record/${it.kind.name}/${it.id}") }) }
            composable("pro") { ProScreen(state.pro, onBack = { nav.popBackStack() }, onBuy = { vm.purchasePro(activity) }, onRestore = vm::restorePro) }
        }
    }
}

object LegalText {
    const val privacy = "Mi Garaje usa Google Authentication para iniciar sesión y Firebase Firestore para sincronizar los datos del vehículo vinculados a tu UID. La versión FREE puede solicitar anuncios mediante Google AdMob y usa Google UMP para gestionar consentimiento en regiones aplicables. Google Play Billing procesa la compra opcional Mi Garaje PRO. Las fotos se guardan localmente y solo se suben a Firebase Storage si el desarrollador activa esa opción. Puedes eliminar tu cuenta y sus datos desde Ajustes. Contacto del desarrollador configurable en la aplicación."
    const val terms = "Mi Garaje es una herramienta de registro personal. Los datos introducidos por el usuario, avisos de mantenimiento y cálculos de consumo son informativos y no sustituyen las instrucciones del fabricante, inspecciones oficiales ni asesoramiento profesional. El usuario es responsable de la exactitud de los datos que introduce y del cumplimiento de sus obligaciones legales y de mantenimiento."
}
