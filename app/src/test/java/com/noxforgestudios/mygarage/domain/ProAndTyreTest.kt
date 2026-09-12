package com.noxforgestudios.mygarage.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class ProAndTyreTest {
    @Test fun `pro requires purchased acknowledged exact product`() {
        assertTrue(ProEntitlementPolicy.hasEntitlement(setOf("migaraje_pro"), "migaraje_pro", true, true))
        assertFalse(ProEntitlementPolicy.hasEntitlement(setOf("migaraje_pro"), "migaraje_pro", true, false))
        assertFalse(ProEntitlementPolicy.hasEntitlement(setOf("other"), "migaraje_pro", true, true))
    }

    @Test fun `tyre rotation keeps history and swaps same side`() {
        val t = GarageRecord(id = "old", kind = RecordKind.TYRE, brand = "Michelin", productModel = "Pilot", position = "Delantero izquierdo", date = Date(1000))
        val rotated = TyreRotation.rotationCopies(listOf(t), 55_000).single()
        assertEquals("", rotated.id)
        assertEquals("Trasero izquierdo", rotated.position)
        assertEquals(55_000L, rotated.odometerKm)
        assertEquals(0.0, rotated.cost, 0.0)
    }
}
