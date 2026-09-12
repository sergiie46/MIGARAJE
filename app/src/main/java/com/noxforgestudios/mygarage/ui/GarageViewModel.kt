package com.noxforgestudios.mygarage.ui

import android.app.Activity
import android.net.Uri
import java.util.UUID
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.noxforgestudios.mygarage.AppContainer
import com.noxforgestudios.mygarage.domain.*
import com.noxforgestudios.mygarage.firebase.SignInCancelledException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

data class GarageUiState(
    val user: FirebaseUser? = null,
    val preferences: AppPreferences = AppPreferences(),
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val records: List<GarageRecord> = emptyList(),
    val syncState: SyncState = SyncState.SYNCED,
    val lastSyncMillis: Long? = null,
    val online: Boolean = true,
    val pro: ProState = ProState(),
    val loading: Boolean = false,
    val savingVehicle: Boolean = false,
    val message: String? = null,
    val firebaseConfigured: Boolean = true
)

class GarageViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(
        GarageUiState(
            user = container.authRepository.currentUser,
            firebaseConfigured = container.firebaseConfigured
        )
    )
    val state: StateFlow<GarageUiState> = _state.asStateFlow()
    private var vehiclesJob: Job? = null
    private var recordsJob: Job? = null

    init {
        viewModelScope.launch { container.preferences.preferences.collect { prefs -> _state.update { it.copy(preferences = prefs) }; selectVehicleFromPrefs() } }
        viewModelScope.launch { container.garageRepository.syncState.collect { value -> _state.update { it.copy(syncState = value) } } }
        viewModelScope.launch { container.garageRepository.lastSyncMillis.collect { value -> _state.update { it.copy(lastSyncMillis = value) } } }
        viewModelScope.launch { container.networkMonitor.isOnline.collect { online -> _state.update { it.copy(online = online, syncState = if (!online) SyncState.OFFLINE else it.syncState) } } }
        viewModelScope.launch { container.billingManager.state.collect { pro -> _state.update { it.copy(pro = pro) } } }
        _state.value.user?.let { attachUser(it) }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            val result = container.authRepository.signInWithGoogle(activity)
            val user = result.getOrNull()
            if (user != null) {
                container.garageRepository.upsertProfile(container.authRepository.profile(user))
                _state.update { it.copy(user = user, loading = false) }
                attachUser(user)
            } else {
                val e = result.exceptionOrNull()
                _state.update { it.copy(loading = false, message = if (e is SignInCancelledException) null else e?.message ?: "No se pudo iniciar sesión") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            container.authRepository.signOut()
            container.preferences.clearForSignOut()
            vehiclesJob?.cancel(); recordsJob?.cancel()
            _state.update { it.copy(user = null, vehicles = emptyList(), selectedVehicle = null, records = emptyList()) }
        }
    }

    private fun attachUser(user: FirebaseUser) {
        vehiclesJob?.cancel()
        vehiclesJob = viewModelScope.launch {
            container.garageRepository.observeVehicles(user.uid).collect { vehicles ->
                _state.update { s -> s.copy(vehicles = vehicles) }
                selectVehicleFromPrefs()
            }
        }
    }

    private fun selectVehicleFromPrefs() {
        val s = _state.value
        if (s.user == null) return
        val preferred = s.preferences.selectedVehicleId
        val chosen = s.vehicles.firstOrNull { it.id == preferred && !it.archived }
            ?: s.vehicles.firstOrNull { !it.archived }
            ?: s.vehicles.firstOrNull()
        if (chosen?.id == s.selectedVehicle?.id) {
            if (chosen != s.selectedVehicle) _state.update { it.copy(selectedVehicle = chosen) }
        } else {
            recordsJob?.cancel(); recordsJob = null;
            _state.update { it.copy(selectedVehicle = chosen, records = emptyList()) }
            if (chosen != null) {
                recordsJob?.cancel()
                recordsJob = viewModelScope.launch {
                    container.garageRepository.observeRecords(s.user.uid, chosen.id).collect { records ->
                        _state.update { if (it.selectedVehicle?.id == chosen.id) it.copy(records = records) else it }
                        scheduleAllReminders(chosen, records)
                    }
                }
            }
        }
    }

    fun selectVehicle(id: String) {
        if (_state.value.selectedVehicle?.id == id) return
        _state.update { it.copy(preferences = it.preferences.copy(selectedVehicleId = id)) }
        selectVehicleFromPrefs()
        viewModelScope.launch { container.preferences.setSelectedVehicle(id) }
    }

    fun saveVehicleWithMedia(vehicle: Vehicle, main: Uri?, extras: List<Uri>, onDone: () -> Unit) {
        val user = _state.value.user ?: run { showMessage("Inicia sesión para guardar"); return }
        if (_state.value.savingVehicle) return
        val active = _state.value.vehicles.count { !it.archived && it.status != VehicleStatus.VENDIDO }
        if (vehicle.id.isBlank() && VehicleCalculators.activeVehicleLimitReached(active, _state.value.pro.isPro)) {
            showMessage("FREE admite hasta 2 vehículos activos"); return
        }
        _state.update { it.copy(savingVehicle = true) }
        viewModelScope.launch {
            var prepared: com.noxforgestudios.mygarage.data.PreparedMedia? = null
            var saved = false
            try {
                val identified = vehicle.copy(id = vehicle.id.ifBlank { UUID.randomUUID().toString() })
                prepared = container.vehicleMedia.prepare(identified, user.uid, main, extras)
                val id = container.garageRepository.saveVehicle(user.uid, prepared.vehicle).getOrThrow()
                saved = true
                container.preferences.setSelectedVehicle(id)
                onDone()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { showMessage("No se pudo guardar: "+(e.message ?: "vuelve a seleccionar los archivos"))
            } finally {
                if (!saved) prepared?.discard()
                _state.update { it.copy(savingVehicle = false) }
            }
        }
    }

    fun saveVehicle(vehicle: Vehicle, onDone: (String?) -> Unit = {}) {
        val user = _state.value.user ?: return
        val active = _state.value.vehicles.count { !it.archived && it.status != VehicleStatus.VENDIDO }
        if (vehicle.id.isBlank() && VehicleCalculators.activeVehicleLimitReached(active, _state.value.pro.isPro)) {
            _state.update { it.copy(message = "FREE admite hasta 2 vehículos activos. Desbloquea Mi Garaje PRO para añadir más.") }
            onDone(null)
            return
        }
        viewModelScope.launch {
            val result = container.garageRepository.saveVehicle(user.uid, vehicle)
            result.onSuccess { id -> container.preferences.setSelectedVehicle(id); onDone(id) }
                .onFailure { _state.update { s -> s.copy(message = it.message) }; onDone(null) }
        }
    }

    fun duplicateVehicle(vehicle: Vehicle) {
        val user = _state.value.user ?: return
        val active = _state.value.vehicles.count { !it.archived && it.status != VehicleStatus.VENDIDO }
        if (VehicleCalculators.activeVehicleLimitReached(active, _state.value.pro.isPro)) {
            _state.update { it.copy(message = "Necesitas PRO para superar 2 vehículos activos") }
            return
        }
        viewModelScope.launch { container.garageRepository.duplicateVehicle(user.uid, vehicle).onFailure { e -> showMessage(e.message) } }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        val user = _state.value.user ?: return
        container.reminderScheduler.cancelVehicle(vehicle.id)
        viewModelScope.launch { container.garageRepository.deleteVehicle(user.uid, vehicle.id).onFailure { e -> showMessage(e.message) } }
    }

    fun saveRecord(record: GarageRecord, onDone: () -> Unit = {}) {
        val s = _state.value
        val user = s.user ?: return
        val vehicle = s.selectedVehicle ?: return
        viewModelScope.launch {
            val result = container.garageRepository.saveRecord(user.uid, vehicle.id, record)
            result.onSuccess { id ->
                val saved = record.copy(id = id, ownerUid = user.uid, vehicleId = vehicle.id)
                if (saved.kind == RecordKind.REMINDER || saved.nextDueDate != null || saved.nextDueKm != null) container.reminderScheduler.schedule(saved, vehicle)
                onDone()
            }.onFailure { showMessage(it.message) }
        }
    }

    fun deleteRecord(record: GarageRecord) {
        val uid = _state.value.user?.uid ?: return
        val vehicleId = _state.value.selectedVehicle?.id ?: return
        container.reminderScheduler.cancel(record)
        viewModelScope.launch { container.garageRepository.deleteRecord(uid, vehicleId, record).onFailure { showMessage(it.message) } }
    }

    fun rotateTyres() {
        val s = _state.value
        val uid = s.user?.uid ?: return
        val vehicle = s.selectedVehicle ?: return
        val copies = TyreRotation.rotationCopies(s.records.filter { it.kind == RecordKind.TYRE }, vehicle.odometerKm)
        if (copies.isEmpty()) { showMessage("Añade al menos un neumático con posición antes de rotarlos"); return }
        viewModelScope.launch {
            var failure: Throwable? = null
            copies.forEach { copy ->
                val result = container.garageRepository.saveRecord(uid, vehicle.id, copy)
                if (result.isFailure && failure == null) failure = result.exceptionOrNull()
            }
            if (failure != null) showMessage("No se pudo completar la rotación: ${failure?.message}")
            else showMessage("Rotación registrada en el historial")
        }
    }

    fun updateOdometer(newKm: Long, allowDecrease: Boolean, onDone: (Boolean) -> Unit = {}) {
        val s = _state.value
        val vehicle = s.selectedVehicle ?: return
        val uid = s.user?.uid ?: return
        when (val validation = VehicleCalculators.validateOdometer(vehicle.odometerKm, newKm)) {
            is OdometerValidation.Invalid -> { showMessage(validation.message); onDone(false); return }
            is OdometerValidation.Warning -> if (!allowDecrease) { showMessage(validation.message); onDone(false); return }
            else -> Unit
        }
        viewModelScope.launch {
            container.garageRepository.updateOdometer(uid, vehicle, newKm)
                .onSuccess {
                    val updated = vehicle.copy(odometerKm = newKm)
                    _state.value.records.forEach { container.reminderScheduler.checkKilometers(it, updated) }
                    onDone(true)
                }
                .onFailure { showMessage(it.message); onDone(false) }
        }
    }

    fun syncNow() = viewModelScope.launch { container.garageRepository.syncNow().onFailure { showMessage(it.message) } }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { container.preferences.setTheme(mode) }
    fun setDistanceUnit(unit: DistanceUnit) = viewModelScope.launch { container.preferences.setDistance(unit) }
    fun setVolumeUnit(unit: VolumeUnit) = viewModelScope.launch { container.preferences.setVolume(unit) }
    fun setConsumptionUnit(unit: ConsumptionUnit) = viewModelScope.launch { container.preferences.setConsumption(unit) }
    fun setCurrency(code: String) = viewModelScope.launch { container.preferences.setCurrency(code) }
    fun setOnboardingDone() = viewModelScope.launch { container.preferences.setOnboardingDone() }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch {
        container.preferences.setNotifications(enabled)
        if (!enabled) container.reminderScheduler.cancelAll()
        else _state.value.selectedVehicle?.let { scheduleAllReminders(it, _state.value.records) }
    }

    fun exportCsv(onReady: (File) -> Unit) {
        val v = _state.value.selectedVehicle ?: return
        if (!_state.value.pro.isPro) { showMessage("La exportación CSV es una función PRO"); return }
        container.exportManager.exportCsv(v, _state.value.records).onSuccess(onReady).onFailure { showMessage(it.message) }
    }
    fun exportPdf(onReady: (File) -> Unit) {
        val v = _state.value.selectedVehicle ?: return
        if (!_state.value.pro.isPro) { showMessage("La exportación PDF es una función PRO"); return }
        container.exportManager.exportPdf(v, _state.value.records).onSuccess(onReady).onFailure { showMessage(it.message) }
    }
    fun share(file: File, mime: String) = container.exportManager.share(file, mime)

    fun purchasePro(activity: Activity) { container.billingManager.launchPurchase(activity) }
    fun restorePro() { container.billingManager.restorePurchases() }

    fun deleteAccount(onDone: () -> Unit = {}) {
        val uid = _state.value.user?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val dataResult = container.garageRepository.deleteAllUserData(uid)
            if (dataResult.isFailure) { _state.update { it.copy(loading = false, message = dataResult.exceptionOrNull()?.message) }; return@launch }
            container.photoRepository.deleteUserPhotos(uid)
            container.reminderScheduler.cancelAll()
            val authResult = container.authRepository.deleteCurrentUser()
            if (authResult.isSuccess) {
                container.preferences.clearForSignOut()
                vehiclesJob?.cancel(); recordsJob?.cancel()
                _state.value = GarageUiState(firebaseConfigured = container.firebaseConfigured, pro = _state.value.pro, preferences = _state.value.preferences)
                onDone()
            } else {
                _state.update { it.copy(loading = false, message = "Los datos se eliminaron, pero Firebase exige volver a iniciar sesión recientemente para borrar la identidad. Cierra sesión, entra de nuevo y repite: ${authResult.exceptionOrNull()?.message}") }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
    fun showMessage(message: String?) { if (!message.isNullOrBlank()) _state.update { it.copy(message = message) } }

    private fun scheduleAllReminders(vehicle: Vehicle, records: List<GarageRecord>) {
        if (!_state.value.preferences.notificationsEnabled) return
        records.filter { it.reminderEnabled && (it.kind == RecordKind.REMINDER || it.nextDueDate != null || it.nextDueKm != null || it.nextDate != null || it.endDate != null) }
            .forEach { container.reminderScheduler.schedule(it, vehicle) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GarageViewModel(container) as T
    }
}
