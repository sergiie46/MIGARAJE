package com.noxforgestudios.mygarage.data

import com.noxforgestudios.mygarage.domain.GarageRecord
import com.noxforgestudios.mygarage.domain.SyncState
import com.noxforgestudios.mygarage.domain.UserProfile
import com.noxforgestudios.mygarage.domain.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GarageRepository {
    val syncState: StateFlow<SyncState>
    val lastSyncMillis: StateFlow<Long?>
    fun observeVehicles(uid: String): Flow<List<Vehicle>>
    fun observeRecords(uid: String, vehicleId: String): Flow<List<GarageRecord>>
    suspend fun upsertProfile(profile: UserProfile): Result<Unit>
    suspend fun saveVehicle(uid: String, vehicle: Vehicle): Result<String>
    suspend fun duplicateVehicle(uid: String, vehicle: Vehicle): Result<String>
    suspend fun deleteVehicle(uid: String, vehicleId: String): Result<Unit>
    suspend fun saveRecord(uid: String, vehicleId: String, record: GarageRecord): Result<String>
    suspend fun deleteRecord(uid: String, vehicleId: String, record: GarageRecord): Result<Unit>
    suspend fun updateOdometer(uid: String, vehicle: Vehicle, newKm: Long): Result<Unit>
    suspend fun syncNow(): Result<Unit>
    suspend fun deleteAllUserData(uid: String): Result<Unit>
}
