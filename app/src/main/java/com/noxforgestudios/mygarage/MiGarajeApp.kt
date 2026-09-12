package com.noxforgestudios.mygarage

import android.app.Application
import com.noxforgestudios.mygarage.notifications.ReminderWorker

class MiGarajeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderWorker.createChannel(this)
        container.billingManager.start()
    }
}
