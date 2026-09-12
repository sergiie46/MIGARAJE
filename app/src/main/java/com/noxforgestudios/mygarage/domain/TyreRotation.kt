package com.noxforgestudios.mygarage.domain

object TyreRotation {
    private val sameSide = mapOf(
        "Delantero izquierdo" to "Trasero izquierdo",
        "Trasero izquierdo" to "Delantero izquierdo",
        "Delantero derecho" to "Trasero derecho",
        "Trasero derecho" to "Delantero derecho"
    )

    fun rotatedPosition(position: String): String = sameSide[position] ?: position

    fun rotationCopies(currentTyres: List<GarageRecord>, odometerKm: Long): List<GarageRecord> {
        val latestByPosition = currentTyres
            .filter { it.kind == RecordKind.TYRE && it.position in sameSide.keys }
            .groupBy { it.position }
            .mapValues { (_, records) -> records.maxByOrNull { it.date.time }!! }
            .values
        return latestByPosition.map { tyre ->
            tyre.copy(
                id = "",
                title = "Rotación · ${listOf(tyre.brand, tyre.productModel).filter(String::isNotBlank).joinToString(" ").ifBlank { "Neumático" }}",
                date = java.util.Date(),
                odometerKm = odometerKm,
                installedDate = java.util.Date(),
                installedKm = odometerKm,
                position = rotatedPosition(tyre.position),
                cost = 0.0,
                notes = listOf(tyre.notes, "Rotación de neumáticos").filter(String::isNotBlank).joinToString(" · ")
            )
        }
    }
}
