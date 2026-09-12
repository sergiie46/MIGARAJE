package com.noxforgestudios.mygarage.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.noxforgestudios.mygarage.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "migaraje_preferences")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DISTANCE = stringPreferencesKey("distance")
        val VOLUME = stringPreferencesKey("volume")
        val CONSUMPTION = stringPreferencesKey("consumption")
        val CURRENCY = stringPreferencesKey("currency")
        val ONBOARDING = booleanPreferencesKey("onboarding")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val SELECTED_VEHICLE = stringPreferencesKey("selected_vehicle")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { p ->
        AppPreferences(
            themeMode = enumValueOrDefault(p[Keys.THEME], ThemeMode.SYSTEM),
            distanceUnit = enumValueOrDefault(p[Keys.DISTANCE], DistanceUnit.KM),
            volumeUnit = enumValueOrDefault(p[Keys.VOLUME], VolumeUnit.LITERS),
            consumptionUnit = enumValueOrDefault(p[Keys.CONSUMPTION], ConsumptionUnit.L_PER_100_KM),
            currencyCode = p[Keys.CURRENCY] ?: "EUR",
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: true,
            selectedVehicleId = p[Keys.SELECTED_VEHICLE]
        )
    }

    suspend fun setTheme(value: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = value.name }
    suspend fun setDistance(value: DistanceUnit) = context.dataStore.edit { it[Keys.DISTANCE] = value.name }
    suspend fun setVolume(value: VolumeUnit) = context.dataStore.edit { it[Keys.VOLUME] = value.name }
    suspend fun setConsumption(value: ConsumptionUnit) = context.dataStore.edit { it[Keys.CONSUMPTION] = value.name }
    suspend fun setCurrency(value: String) = context.dataStore.edit { it[Keys.CURRENCY] = value.uppercase() }
    suspend fun setOnboardingDone(value: Boolean = true) = context.dataStore.edit { it[Keys.ONBOARDING] = value }
    suspend fun setNotifications(value: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATIONS] = value }
    suspend fun setSelectedVehicle(id: String?) = context.dataStore.edit { prefs -> if (id == null) prefs.remove(Keys.SELECTED_VEHICLE) else prefs[Keys.SELECTED_VEHICLE] = id }

    suspend fun clearForSignOut() = context.dataStore.edit { it.remove(Keys.SELECTED_VEHICLE) }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}
