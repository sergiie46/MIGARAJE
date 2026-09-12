package com.noxforgestudios.mygarage.domain

import java.util.Date
import kotlin.math.max

object VehicleCalculators {
    fun fuelMetrics(previous: GarageRecord?, current: GarageRecord): FuelMetrics {
        if (previous == null || previous.kind != RecordKind.REFUEL || current.kind != RecordKind.REFUEL) return FuelMetrics()
        val previousKm = previous.odometerKm ?: return FuelMetrics()
        val currentKm = current.odometerKm ?: return FuelMetrics()
        val liters = current.liters ?: return FuelMetrics()
        val distance = currentKm - previousKm
        if (!current.fullTank || !previous.fullTank || distance <= 0L || liters <= 0.0) return FuelMetrics()
        val l100 = liters / distance * 100.0
        val kmL = distance / liters
        val total = current.cost.takeIf { it > 0 } ?: ((current.pricePerLiter ?: 0.0) * liters)
        return FuelMetrics(
            litersPer100Km = l100,
            kmPerLiter = kmL,
            costPer100Km = if (total > 0) total / distance * 100.0 else null,
            costPerKm = if (total > 0) total / distance else null,
            distanceKm = distance
        )
    }

    fun remainingKm(currentOdometer: Long, dueKm: Long?): Long? = dueKm?.minus(currentOdometer)

    fun remainingDays(now: Date, dueDate: Date?): Long? = dueDate?.let {
        ((it.time - now.time) / 86_400_000L)
    }

    fun nextMaintenanceKm(performedKm: Long?, intervalKm: Long?): Long? {
        if (performedKm == null || intervalKm == null || intervalKm <= 0) return null
        return performedKm + intervalKm
    }

    fun validateOdometer(current: Long, proposed: Long): OdometerValidation = when {
        proposed < 0 -> OdometerValidation.Invalid("El kilometraje no puede ser negativo")
        proposed < current -> OdometerValidation.Warning("El nuevo kilometraje es menor que el actual")
        proposed == current -> OdometerValidation.Warning("El kilometraje no ha cambiado")
        else -> OdometerValidation.Valid
    }

    fun activeVehicleLimitReached(activeVehicles: Int, isPro: Boolean): Boolean = !isPro && activeVehicles >= 2

    fun expenseStats(records: List<GarageRecord>, startOdometer: Long? = null, endOdometer: Long? = null): ExpenseStats {
        val costs = records.filter { it.cost > 0.0 }
        val byCategory = costs.groupBy { if (it.category.isBlank()) it.kind.displayName else it.category }
            .mapValues { (_, list) -> list.sumOf { it.cost } }
        val distance = if (startOdometer != null && endOdometer != null) max(0, endOdometer - startOdometer) else 0
        return ExpenseStats(
            total = costs.sumOf { it.cost },
            fuel = costs.filter { it.kind == RecordKind.REFUEL || it.category.equals("combustible", true) }.sumOf { it.cost },
            maintenance = costs.filter { it.kind == RecordKind.MAINTENANCE }.sumOf { it.cost },
            repairs = costs.filter { it.kind == RecordKind.REPAIR }.sumOf { it.cost },
            modifications = costs.filter { it.kind == RecordKind.MODIFICATION }.sumOf { it.cost },
            byCategory = byCategory,
            distanceKm = distance,
            costPerKm = if (distance > 0) costs.sumOf { it.cost } / distance else null
        )
    }
}

sealed interface OdometerValidation {
    data object Valid : OdometerValidation
    data class Warning(val message: String) : OdometerValidation
    data class Invalid(val message: String) : OdometerValidation
}
