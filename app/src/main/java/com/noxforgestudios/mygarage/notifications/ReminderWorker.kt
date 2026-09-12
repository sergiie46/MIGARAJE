package com.noxforgestudios.mygarage.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.noxforgestudios.mygarage.R

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        createChannel(applicationContext)
        val title = inputData.getString(KEY_TITLE) ?: "Recordatorio Mi Garaje"
        val text = inputData.getString(KEY_TEXT) ?: "Tienes un recordatorio pendiente"
        val id = inputData.getString(KEY_ID)?.hashCode() ?: System.currentTimeMillis().toInt()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java).notify(id, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "migaraje_reminders"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_TEXT = "text"
        fun createChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Recordatorios Mi Garaje", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Mantenimientos, ITV, seguro y otros recordatorios de tus vehículos"
                }
            )
        }
    }
}
