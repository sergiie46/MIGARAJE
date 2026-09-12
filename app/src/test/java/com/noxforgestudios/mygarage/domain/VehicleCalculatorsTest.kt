package com.noxforgestudios.mygarage.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class VehicleCalculatorsTest {
    @Test fun `consumption uses full to full refuels`() {
        val a = GarageRecord(kind = RecordKind.REFUEL, odometerKm = 100_000, liters = 50.0, fullTank = true, cost = 80.0)
        val b = GarageRecord(kind = RecordKind.REFUEL, odometerKm = 100_600, liters = 42.0, fullTank = true, cost = 70.0)
        val m = VehicleCalculators.fuelMetrics(a, b)
        assertEquals(7.0, m.litersPer100Km!!, 0.0001)
        assertEquals(600L, m.distanceKm)
        assertEquals(70.0 / 600.0, m.costPerKm!!, 0.0001)
    }

    @Test fun `consumption is absent when tank is not full`() {
        val a = GarageRecord(kind = RecordKind.REFUEL, odometerKm = 1000, liters = 20.0, fullTank = false)
        val b = GarageRecord(kind = RecordKind.REFUEL, odometerKm = 1200, liters = 20.0, fullTank = true)
        assertNull(VehicleCalculators.fuelMetrics(a, b).litersPer100Km)
    }

    @Test fun `next maintenance adds interval`() {
        assertEquals(372_000L, VehicleCalculators.nextMaintenanceKm(362_000, 10_000))
        assertEquals(5_150L, VehicleCalculators.remainingKm(366_850, 372_000))
    }

    @Test fun `odometer decrease warns`() {
        assertTrue(VehicleCalculators.validateOdometer(50_000, 49_000) is OdometerValidation.Warning)
        assertTrue(VehicleCalculators.validateOdometer(50_000, 51_000) is OdometerValidation.Valid)
    }

    @Test fun `free tier stops third active vehicle`() {
        assertTrue(VehicleCalculators.activeVehicleLimitReached(2, false))
        assertFalse(VehicleCalculators.activeVehicleLimitReached(2, true))
    }

    @Test fun `expense totals are grouped correctly`() {
        val records = listOf(
            GarageRecord(kind = RecordKind.REFUEL, cost = 70.0, category = "Combustible"),
            GarageRecord(kind = RecordKind.MAINTENANCE, cost = 120.0, category = "Mantenimiento"),
            GarageRecord(kind = RecordKind.REPAIR, cost = 230.0, category = "Reparación")
        )
        val s = VehicleCalculators.expenseStats(records, 1000, 2000)
        assertEquals(420.0, s.total, 0.001)
        assertEquals(0.42, s.costPerKm!!, 0.001)
        assertEquals(120.0, s.maintenance, 0.001)
    }

    @Test fun `remaining days uses calendar distance`() {
        val now = Date(1_000_000L)
        val due = Date(now.time + 10L * 86_400_000L)
        assertEquals(10L, VehicleCalculators.remainingDays(now, due))
    }
}
