package com.noxforgestudios.mygarage.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.noxforgestudios.mygarage.domain.GarageRecord
import com.noxforgestudios.mygarage.domain.Vehicle
import java.util.concurrent.TimeUnit

class ReminderScheduler(context: Context) {
    private val wm = WorkManager.getInstance(context)

    fun schedule(record: GarageRecord, vehicle: Vehicle) {
        if (!record.reminderEnabled || record.id.isBlank()) return
        cancel(record)
        if (record.reminderByDate) {
            val due = record.nextDueDate ?: record.nextDate ?: record.endDate
            due?.let { date ->
                val leadDays = record.reminderLeadDays.ifEmpty { listOf(0) }.distinct().filter { it >= 0 }
                leadDays.forEach { lead ->
                    val triggerAt = date.time - lead * 86_400_000L
                    if (triggerAt >= System.currentTimeMillis() - 60_000L) {
                        enqueue(
                            record,
                            vehicle,
                            "date_$lead",
                            (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L),
                            "${record.title.ifBlank { record.kind.displayName }} · ${vehicle.title}",
                            if (lead > 0) "Faltan $lead días" else "Vence hoy"
                        )
                    }
                }
            }
        }
        checkKilometers(record, vehicle)
    }

    fun checkKilometers(record: GarageRecord, vehicle: Vehicle) {
        if (!record.reminderEnabled || !record.reminderByKm || record.id.isBlank()) return
        val due = record.nextDueKm ?: return
        val lead = record.reminderLeadKm ?: 0L
        if (vehicle.odometerKm >= due - lead) {
            val remaining = due - vehicle.odometerKm
            enqueue(
                record,
                vehicle,
                "km",
                0L,
                "${record.title.ifBlank { record.kind.displayName }} · ${vehicle.title}",
                if (remaining >= 0) "Quedan $remaining km" else "Superado por ${-remaining} km"
            )
        } else {
            wm.cancelUniqueWork("migaraje_${record.id}_km")
        }
    }

    fun cancel(record: GarageRecord) {
        if (record.id.isNotBlank()) wm.cancelAllWorkByTag("record_${record.id}")
    }

    fun cancelVehicle(vehicleId: String) {
        if (vehicleId.isNotBlank()) wm.cancelAllWorkByTag("vehicle_$vehicleId")
    }

    fun cancelAll() = wm.cancelAllWorkByTag(GLOBAL_TAG)

    private fun enqueue(record: GarageRecord, vehicle: Vehicle, suffix: String, delayMs: Long, title: String, text: String) {
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_ID, "${record.id}_$suffix")
            .putString(ReminderWorker.KEY_TITLE, title)
            .putString(ReminderWorker.KEY_TEXT, text)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(GLOBAL_TAG)
            .addTag("record_${record.id}")
            .addTag("vehicle_${vehicle.id}")
            .build()
        wm.enqueueUniqueWork("migaraje_${record.id}_$suffix", ExistingWorkPolicy.REPLACE, request)
    }

    private companion object { const val GLOBAL_TAG = "migaraje_reminder" }
}
