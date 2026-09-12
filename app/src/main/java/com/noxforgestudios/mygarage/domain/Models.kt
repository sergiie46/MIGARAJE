package com.noxforgestudios.mygarage.domain

import java.util.Date

enum class VehicleStatus { ACTUAL, VENDIDO, PROYECTO, HISTORICO }
enum class SyncState { SYNCED, SYNCING, OFFLINE, ERROR, NOT_CONFIGURED }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class DistanceUnit { KM, MILES }
enum class VolumeUnit { LITERS, GALLONS_US }
enum class ConsumptionUnit { L_PER_100_KM, MPG_US }

enum class RecordKind(val collection: String, val displayName: String) {
    MAINTENANCE("maintenance", "Mantenimiento"),
    REPAIR("repairs", "Reparación"),
    REFUEL("refuels", "Repostaje"),
    EXPENSE("expenses", "Gasto"),
    REMINDER("reminders", "Recordatorio"),
    INSPECTION("inspections", "ITV / Inspección"),
    INSURANCE("insurance", "Seguro"),
    MODIFICATION("modifications", "Mejoras"),
    PART("parts", "Pieza"),
    TYRE("tyres", "Neumático"),
    TAX("taxes", "Impuesto"),
    ODOMETER("odometer", "Kilometraje")
}

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val lastSyncAt: Date? = null
)

data class Vehicle(
    val id: String = "",
    val ownerUid: String = "",
    val make: String = "",
    val model: String = "",
    val generation: String = "",
    val version: String = "",
    val year: Int? = null,
    val plate: String = "",
    val vin: String = "",
    val odometerKm: Long = 0,
    val purchaseDate: Date? = null,
    val purchasePrice: Double? = null,
    val fuel: String = "",
    val displacementCc: Int? = null,
    val powerCv: Int? = null,
    val powerKw: Int? = null,
    val transmission: String = "",
    val traction: String = "",
    val color: String = "",
    val nickname: String = "",
    val notes: String = "",
    val localPhotoPath: String? = null,
    val remotePhotoUrl: String? = null,
    val galleryLocalPaths: List<String> = emptyList(),
    val galleryRemoteUrls: List<String> = emptyList(),
    val videoLocalPaths: List<String> = emptyList(),
    val status: VehicleStatus = VehicleStatus.ACTUAL,
    val archived: Boolean = false,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
) {
    val title: String
        get() = listOf(make, model, generation).filter { it.isNotBlank() }.joinToString(" ").ifBlank { nickname.ifBlank { "Vehículo" } }
}

data class GarageRecord(
    val id: String = "",
    val ownerUid: String = "",
    val vehicleId: String = "",
    val kind: RecordKind = RecordKind.EXPENSE,
    val title: String = "",
    val date: Date = Date(),
    val odometerKm: Long? = null,
    val cost: Double = 0.0,
    val notes: String = "",
    val status: String = "",
    val category: String = "",

    val workshop: String = "",
    val parts: String = "",
    val laborCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val nextDueKm: Long? = null,
    val nextDueDate: Date? = null,

    val fault: String = "",
    val symptoms: String = "",
    val diagnosis: String = "",
    val repairAction: String = "",

    val liters: Double? = null,
    val pricePerLiter: Double? = null,
    val fullTank: Boolean = false,
    val station: String = "",
    val fuelType: String = "",

    val brand: String = "",
    val productModel: String = "",
    val reference: String = "",
    val description: String = "",
    val dimensions: String = "",

    val tyreSize: String = "",
    val dot: String = "",
    val recommendedPressureBar: Double? = null,
    val position: String = "",
    val installedDate: Date? = null,
    val installedKm: Long? = null,

    val result: String = "",
    val nextDate: Date? = null,
    val minorDefects: String = "",
    val majorDefects: String = "",

    val provider: String = "",
    val policy: String = "",
    val coverage: String = "",
    val startDate: Date? = null,
    val endDate: Date? = null,
    val autoRenew: Boolean = false,
    val roadsideAssistance: Boolean = false,

    val taxYear: Int? = null,
    val paid: Boolean = false,

    val reminderByDate: Boolean = false,
    val reminderByKm: Boolean = false,
    val reminderLeadKm: Long? = null,
    val reminderLeadDays: List<Int> = emptyList(),
    val reminderEnabled: Boolean = true,

    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class FuelMetrics(
    val litersPer100Km: Double? = null,
    val kmPerLiter: Double? = null,
    val costPer100Km: Double? = null,
    val costPerKm: Double? = null,
    val distanceKm: Long? = null
)

data class ExpenseStats(
    val total: Double = 0.0,
    val fuel: Double = 0.0,
    val maintenance: Double = 0.0,
    val repairs: Double = 0.0,
    val modifications: Double = 0.0,
    val byCategory: Map<String, Double> = emptyMap(),
    val distanceKm: Long = 0,
    val costPerKm: Double? = null
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val volumeUnit: VolumeUnit = VolumeUnit.LITERS,
    val consumptionUnit: ConsumptionUnit = ConsumptionUnit.L_PER_100_KM,
    val currencyCode: String = "EUR",
    val onboardingDone: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val selectedVehicleId: String? = null
)

data class ProState(
    val isPro: Boolean = false,
    val productAvailable: Boolean = false,
    val priceText: String? = null,
    val purchasePending: Boolean = false,
    val message: String? = null
)
