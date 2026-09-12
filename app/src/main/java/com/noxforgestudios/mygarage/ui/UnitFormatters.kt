package com.noxforgestudios.mygarage.ui

import com.noxforgestudios.mygarage.domain.*
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

object UnitFormatters {
    private const val KM_PER_MILE = 1.609344
    private const val LITERS_PER_US_GALLON = 3.785411784
    private const val MPG_US_FACTOR = 235.214583

    fun distanceFromKm(km: Long, prefs: AppPreferences): Double =
        if (prefs.distanceUnit == DistanceUnit.KM) km.toDouble() else km / KM_PER_MILE

    fun distanceToKm(value: Double, prefs: AppPreferences): Long =
        (if (prefs.distanceUnit == DistanceUnit.KM) value else value * KM_PER_MILE).roundToLong()

    fun distanceLabel(prefs: AppPreferences): String = if (prefs.distanceUnit == DistanceUnit.KM) "km" else "mi"

    fun editableDistance(km: Long, prefs: AppPreferences): String = when (prefs.distanceUnit) {
        DistanceUnit.KM -> km.toString()
        DistanceUnit.MILES -> "%.1f".format(Locale.US, distanceFromKm(km, prefs))
    }

    fun editableVolume(liters: Double, prefs: AppPreferences): String = "%.3f".format(Locale.US, volumeFromLiters(liters, prefs)).trimEnd('0').trimEnd('.')

    fun editablePricePerVolume(pricePerLiter: Double, prefs: AppPreferences): String = "%.4f".format(Locale.US, pricePerLiterToDisplayedVolume(pricePerLiter, prefs)).trimEnd('0').trimEnd('.')

    fun formatDistance(km: Long, prefs: AppPreferences): String {
        val value = distanceFromKm(km, prefs)
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = if (prefs.distanceUnit == DistanceUnit.KM) 0 else 1
        }
        return "${nf.format(value)} ${distanceLabel(prefs)}"
    }

    fun volumeFromLiters(liters: Double, prefs: AppPreferences): Double =
        if (prefs.volumeUnit == VolumeUnit.LITERS) liters else liters / LITERS_PER_US_GALLON

    fun volumeToLiters(value: Double, prefs: AppPreferences): Double =
        if (prefs.volumeUnit == VolumeUnit.LITERS) value else value * LITERS_PER_US_GALLON

    fun pricePerDisplayedVolumeToPerLiter(value: Double, prefs: AppPreferences): Double =
        if (prefs.volumeUnit == VolumeUnit.LITERS) value else value / LITERS_PER_US_GALLON

    fun pricePerLiterToDisplayedVolume(value: Double, prefs: AppPreferences): Double =
        if (prefs.volumeUnit == VolumeUnit.LITERS) value else value * LITERS_PER_US_GALLON

    fun volumeLabel(prefs: AppPreferences): String = if (prefs.volumeUnit == VolumeUnit.LITERS) "L" else "gal US"

    fun formatVolume(liters: Double, prefs: AppPreferences): String = "%.2f %s".format(Locale.getDefault(), volumeFromLiters(liters, prefs), volumeLabel(prefs))

    fun consumptionFromL100(l100: Double, prefs: AppPreferences): Double = when (prefs.consumptionUnit) {
        ConsumptionUnit.L_PER_100_KM -> l100
        ConsumptionUnit.MPG_US -> if (l100 > 0.0) MPG_US_FACTOR / l100 else 0.0
    }

    fun consumptionLabel(prefs: AppPreferences): String = if (prefs.consumptionUnit == ConsumptionUnit.L_PER_100_KM) "L/100 km" else "MPG US"

    fun formatConsumption(l100: Double, prefs: AppPreferences): String = "%.2f %s".format(Locale.getDefault(), consumptionFromL100(l100, prefs), consumptionLabel(prefs))

    fun costPerDistanceFromPerKm(costPerKm: Double, prefs: AppPreferences): Double =
        if (prefs.distanceUnit == DistanceUnit.KM) costPerKm else costPerKm * KM_PER_MILE

    fun money(value: Double, currencyCode: String): String = runCatching {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { currency = Currency.getInstance(currencyCode) }.format(value)
    }.getOrElse { "%.2f %s".format(Locale.getDefault(), value, currencyCode) }
}
