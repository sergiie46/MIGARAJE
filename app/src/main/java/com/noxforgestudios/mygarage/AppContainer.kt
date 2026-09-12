package com.noxforgestudios.mygarage

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.noxforgestudios.mygarage.ads.AdManager
import com.noxforgestudios.mygarage.ads.ConsentManager
import com.noxforgestudios.mygarage.billing.BillingManager
import com.noxforgestudios.mygarage.data.*
import com.noxforgestudios.mygarage.export.ExportManager
import com.noxforgestudios.mygarage.firebase.AuthRepository
import com.noxforgestudios.mygarage.notifications.ReminderScheduler
import com.noxforgestudios.mygarage.util.NetworkMonitor

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val firebaseConfigured: Boolean
    val firestore: FirebaseFirestore?
    val auth: FirebaseAuth?
    val storage: FirebaseStorage?

    init {
        val app = runCatching { FirebaseApp.initializeApp(appContext) }.getOrNull()
        firebaseConfigured = app != null
        if (app != null) {
            runCatching {
                val appCheck = FirebaseAppCheck.getInstance()
                if (BuildConfig.USE_APP_CHECK_DEBUG) appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                else appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        }
        firestore = if (firebaseConfigured) runCatching { FirebaseFirestore.getInstance() }.getOrNull() else null
        auth = if (firebaseConfigured) runCatching { FirebaseAuth.getInstance() }.getOrNull() else null
        storage = if (firebaseConfigured && BuildConfig.FIREBASE_STORAGE_ENABLED) runCatching { FirebaseStorage.getInstance() }.getOrNull() else null
    }

    val preferences = PreferencesRepository(appContext)
    val garageRepository: GarageRepository = FirebaseGarageRepository(firestore)
    val authRepository = AuthRepository(appContext, auth)
    val vehicleMedia = VehicleMedia(appContext)
    val vehicleCatalog = VehicleCatalogRepository(appContext)
    val photoRepository = PhotoRepository(appContext, storage)
    val billingManager = BillingManager(appContext)
    val consentManager = ConsentManager()
    val adManager = AdManager(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
    val exportManager = ExportManager(appContext)
    val networkMonitor = NetworkMonitor(appContext)
}
