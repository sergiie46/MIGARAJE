package com.noxforgestudios.mygarage.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.noxforgestudios.mygarage.domain.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseGarageRepository(
    private val firestore: FirebaseFirestore?
) : GarageRepository {
    private val _syncState = MutableStateFlow(if (firestore == null) SyncState.NOT_CONFIGURED else SyncState.SYNCED)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    private val _lastSyncMillis = MutableStateFlow<Long?>(null)
    override val lastSyncMillis: StateFlow<Long?> = _lastSyncMillis.asStateFlow()

    override fun observeVehicles(uid: String): Flow<List<Vehicle>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        _syncState.value = SyncState.SYNCING
        val registration = db.collection("users").document(uid).collection("vehicles")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _syncState.value = SyncState.ERROR
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty().mapNotNull(::vehicleFromDoc)
                val metadata = snapshot?.metadata
                _syncState.value = when {
                    metadata?.hasPendingWrites() == true -> SyncState.SYNCING
                    metadata?.isFromCache == true -> SyncState.OFFLINE
                    else -> SyncState.SYNCED
                }
                if (metadata?.isFromCache == false) _lastSyncMillis.value = System.currentTimeMillis()
                trySend(docs)
            }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override fun observeRecords(uid: String, vehicleId: String): Flow<List<GarageRecord>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val byKind = mutableMapOf<RecordKind, List<GarageRecord>>()
        val registrations = mutableListOf<ListenerRegistration>()
        fun emitMerged() {
            trySend(byKind.values.flatten().sortedByDescending { it.date.time })
        }
        RecordKind.entries.forEach { kind ->
            val reg = vehicleDoc(db, uid, vehicleId).collection(kind.collection)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(500)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _syncState.value = SyncState.ERROR
                        return@addSnapshotListener
                    }
                    byKind[kind] = snapshot?.documents.orEmpty().map { recordFromDoc(it, kind, vehicleId) }
                    val metadata = snapshot?.metadata
                    _syncState.value = when {
                        metadata?.hasPendingWrites() == true -> SyncState.SYNCING
                        metadata?.isFromCache == true -> SyncState.OFFLINE
                        else -> SyncState.SYNCED
                    }
                    if (metadata?.isFromCache == false) _lastSyncMillis.value = System.currentTimeMillis()
                    emitMerged()
                }
            registrations += reg
        }
        awaitClose { registrations.forEach { it.remove() } }
    }.distinctUntilChanged()

    override suspend fun upsertProfile(profile: UserProfile): Result<Unit> = runCatching {
        val db = requireDb()
        val ref = db.collection("users").document(profile.uid)
        val data = mutableMapOf<String, Any?>(
            "uid" to profile.uid,
            "displayName" to profile.displayName,
            "email" to profile.email,
            "photoUrl" to profile.photoUrl,
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastSyncAt" to FieldValue.serverTimestamp()
        )
        ref.set(data, SetOptions.merge()).await()
        val snap = ref.get().await()
        if (!snap.contains("createdAt")) ref.update("createdAt", FieldValue.serverTimestamp()).await()
    }

    override suspend fun saveVehicle(uid: String, vehicle: Vehicle): Result<String> = runCatching {
        val db = requireDb()
        val col = db.collection("users").document(uid).collection("vehicles")
        val ref = if (vehicle.id.isBlank()) col.document() else col.document(vehicle.id)
        _syncState.value = SyncState.SYNCING
        val data = vehicleToMap(vehicle.copy(ownerUid = uid)).toMutableMap().apply {
            this["ownerUid"] = uid
            this["updatedAt"] = FieldValue.serverTimestamp()
            if (vehicle.id.isBlank()) this["createdAt"] = FieldValue.serverTimestamp()
        }
        ref.set(data, SetOptions.merge()).await()
        ref.id
    }.onFailure { _syncState.value = SyncState.ERROR }

    override suspend fun duplicateVehicle(uid: String, vehicle: Vehicle): Result<String> {
        return saveVehicle(uid, vehicle.copy(id = "", nickname = vehicle.nickname.ifBlank { vehicle.title } + " copia", plate = "", vin = ""))
    }

    override suspend fun deleteVehicle(uid: String, vehicleId: String): Result<Unit> = runCatching {
        val db = requireDb()
        val vehicleRef = vehicleDoc(db, uid, vehicleId)
        RecordKind.entries.forEach { kind -> deleteCollection(vehicleRef.collection(kind.collection)) }
        vehicleRef.delete().await()
    }

    override suspend fun saveRecord(uid: String, vehicleId: String, record: GarageRecord): Result<String> = runCatching {
        val db = requireDb()
        val col = vehicleDoc(db, uid, vehicleId).collection(record.kind.collection)
        val ref = if (record.id.isBlank()) col.document() else col.document(record.id)
        _syncState.value = SyncState.SYNCING
        val data = recordToMap(record.copy(ownerUid = uid, vehicleId = vehicleId)).toMutableMap().apply {
            this["ownerUid"] = uid
            this["vehicleId"] = vehicleId
            this["updatedAt"] = FieldValue.serverTimestamp()
            if (record.id.isBlank()) this["createdAt"] = FieldValue.serverTimestamp()
        }
        ref.set(data, SetOptions.merge()).await()
        ref.id
    }.onFailure { _syncState.value = SyncState.ERROR }

    override suspend fun deleteRecord(uid: String, vehicleId: String, record: GarageRecord): Result<Unit> = runCatching {
        requireDb().collection("users").document(uid).collection("vehicles").document(vehicleId)
            .collection(record.kind.collection).document(record.id).delete().await()
    }

    override suspend fun updateOdometer(uid: String, vehicle: Vehicle, newKm: Long): Result<Unit> = runCatching {
        val db = requireDb()
        val vehicleRef = vehicleDoc(db, uid, vehicle.id)
        val odoRef = vehicleRef.collection(RecordKind.ODOMETER.collection).document()
        val batch = db.batch()
        batch.update(vehicleRef, mapOf("odometerKm" to newKm, "updatedAt" to FieldValue.serverTimestamp()))
        batch.set(odoRef, mapOf(
            "ownerUid" to uid,
            "vehicleId" to vehicle.id,
            "title" to "Kilometraje actualizado",
            "date" to Date(),
            "odometerKm" to newKm,
            "cost" to 0.0,
            "notes" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ))
        batch.commit().await()
    }

    override suspend fun syncNow(): Result<Unit> = runCatching {
        val db = requireDb()
        _syncState.value = SyncState.SYNCING
        db.enableNetwork().await()
        db.waitForPendingWrites().await()
        _syncState.value = SyncState.SYNCED
        _lastSyncMillis.value = System.currentTimeMillis()
    }.onFailure { _syncState.value = SyncState.ERROR }

    override suspend fun deleteAllUserData(uid: String): Result<Unit> = runCatching {
        val db = requireDb()
        val userRef = db.collection("users").document(uid)
        val vehicles = userRef.collection("vehicles").get().await().documents
        vehicles.forEach { v ->
            RecordKind.entries.forEach { kind -> deleteCollection(v.reference.collection(kind.collection)) }
            v.reference.delete().await()
        }
        userRef.delete().await()
    }

    private suspend fun deleteCollection(collection: com.google.firebase.firestore.CollectionReference) {
        while (true) {
            val docs = collection.limit(400).get().await().documents
            if (docs.isEmpty()) break
            val batch = requireDb().batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
            if (docs.size < 400) break
        }
    }

    private fun requireDb(): FirebaseFirestore = firestore ?: error("Firebase no está configurado. Añade app/google-services.json")
    private fun vehicleDoc(db: FirebaseFirestore, uid: String, vehicleId: String) = db.collection("users").document(uid).collection("vehicles").document(vehicleId)

    private fun vehicleFromDoc(d: DocumentSnapshot): Vehicle? = runCatching {
        Vehicle(
            id = d.id,
            ownerUid = d.getString("ownerUid").orEmpty(),
            make = d.getString("make").orEmpty(), model = d.getString("model").orEmpty(), generation = d.getString("generation").orEmpty(),
            version = d.getString("version").orEmpty(), year = d.getLong("year")?.toInt(), plate = d.getString("plate").orEmpty(), vin = d.getString("vin").orEmpty(),
            odometerKm = d.getLong("odometerKm") ?: 0, purchaseDate = d.date("purchaseDate"), purchasePrice = d.getDouble("purchasePrice"), fuel = d.getString("fuel").orEmpty(),
            displacementCc = d.getLong("displacementCc")?.toInt(), powerCv = d.getLong("powerCv")?.toInt(), powerKw = d.getLong("powerKw")?.toInt(),
            transmission = d.getString("transmission").orEmpty(), traction = d.getString("traction").orEmpty(), color = d.getString("color").orEmpty(),
            nickname = d.getString("nickname").orEmpty(), notes = d.getString("notes").orEmpty(), localPhotoPath = d.getString("localPhotoPath"), remotePhotoUrl = d.getString("remotePhotoUrl"), galleryLocalPaths = (d.get("galleryLocalPaths") as? List<*>)?.filterIsInstance<String>().orEmpty(), galleryRemoteUrls = (d.get("galleryRemoteUrls") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            status = runCatching { VehicleStatus.valueOf(d.getString("status") ?: "ACTUAL") }.getOrDefault(VehicleStatus.ACTUAL), archived = d.getBoolean("archived") ?: false,
            createdAt = d.date("createdAt"), updatedAt = d.date("updatedAt")
        )
    }.getOrNull()

    private fun vehicleToMap(v: Vehicle): Map<String, Any?> = mapOf(
        "ownerUid" to v.ownerUid, "make" to v.make, "model" to v.model, "generation" to v.generation, "version" to v.version,
        "year" to v.year, "plate" to v.plate, "vin" to v.vin, "odometerKm" to v.odometerKm, "purchaseDate" to v.purchaseDate,
        "purchasePrice" to v.purchasePrice, "fuel" to v.fuel, "displacementCc" to v.displacementCc, "powerCv" to v.powerCv, "powerKw" to v.powerKw,
        "transmission" to v.transmission, "traction" to v.traction, "color" to v.color, "nickname" to v.nickname, "notes" to v.notes,
        "localPhotoPath" to v.localPhotoPath, "remotePhotoUrl" to v.remotePhotoUrl, "galleryLocalPaths" to v.galleryLocalPaths, "galleryRemoteUrls" to v.galleryRemoteUrls, "status" to v.status.name, "archived" to v.archived
    )

    private fun recordFromDoc(d: DocumentSnapshot, kind: RecordKind, vehicleId: String): GarageRecord = GarageRecord(
        id = d.id, ownerUid = d.getString("ownerUid").orEmpty(), vehicleId = vehicleId, kind = kind,
        title = d.getString("title").orEmpty(), date = d.date("date") ?: Date(), odometerKm = d.getLong("odometerKm"), cost = d.getDouble("cost") ?: 0.0,
        notes = d.getString("notes").orEmpty(), status = d.getString("status").orEmpty(), category = d.getString("category").orEmpty(),
        workshop = d.getString("workshop").orEmpty(), parts = d.getString("parts").orEmpty(), laborCost = d.getDouble("laborCost") ?: 0.0, partsCost = d.getDouble("partsCost") ?: 0.0,
        nextDueKm = d.getLong("nextDueKm"), nextDueDate = d.date("nextDueDate"), fault = d.getString("fault").orEmpty(), symptoms = d.getString("symptoms").orEmpty(),
        diagnosis = d.getString("diagnosis").orEmpty(), repairAction = d.getString("repairAction").orEmpty(), liters = d.getDouble("liters"), pricePerLiter = d.getDouble("pricePerLiter"),
        fullTank = d.getBoolean("fullTank") ?: false, station = d.getString("station").orEmpty(), fuelType = d.getString("fuelType").orEmpty(), brand = d.getString("brand").orEmpty(),
        productModel = d.getString("productModel").orEmpty(), reference = d.getString("reference").orEmpty(), description = d.getString("description").orEmpty(),
        tyreSize = d.getString("tyreSize").orEmpty(), dot = d.getString("dot").orEmpty(), recommendedPressureBar = d.getDouble("recommendedPressureBar"), position = d.getString("position").orEmpty(),
        installedDate = d.date("installedDate"), installedKm = d.getLong("installedKm"), result = d.getString("result").orEmpty(), nextDate = d.date("nextDate"),
        minorDefects = d.getString("minorDefects").orEmpty(), majorDefects = d.getString("majorDefects").orEmpty(), provider = d.getString("provider").orEmpty(), policy = d.getString("policy").orEmpty(),
        coverage = d.getString("coverage").orEmpty(), startDate = d.date("startDate"), endDate = d.date("endDate"), autoRenew = d.getBoolean("autoRenew") ?: false,
        roadsideAssistance = d.getBoolean("roadsideAssistance") ?: false, taxYear = d.getLong("taxYear")?.toInt(), paid = d.getBoolean("paid") ?: false,
        reminderByDate = d.getBoolean("reminderByDate") ?: false, reminderByKm = d.getBoolean("reminderByKm") ?: false, reminderLeadKm = d.getLong("reminderLeadKm"),
        reminderLeadDays = (d.get("reminderLeadDays") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }.orEmpty(), reminderEnabled = d.getBoolean("reminderEnabled") ?: true,
        createdAt = d.date("createdAt"), updatedAt = d.date("updatedAt")
    )

    private fun recordToMap(r: GarageRecord): Map<String, Any?> = mapOf(
        "ownerUid" to r.ownerUid, "vehicleId" to r.vehicleId, "title" to r.title, "date" to r.date, "odometerKm" to r.odometerKm, "cost" to r.cost, "notes" to r.notes,
        "status" to r.status, "category" to r.category, "workshop" to r.workshop, "parts" to r.parts, "laborCost" to r.laborCost, "partsCost" to r.partsCost,
        "nextDueKm" to r.nextDueKm, "nextDueDate" to r.nextDueDate, "fault" to r.fault, "symptoms" to r.symptoms, "diagnosis" to r.diagnosis, "repairAction" to r.repairAction,
        "liters" to r.liters, "pricePerLiter" to r.pricePerLiter, "fullTank" to r.fullTank, "station" to r.station, "fuelType" to r.fuelType,
        "brand" to r.brand, "productModel" to r.productModel, "reference" to r.reference, "description" to r.description,
        "tyreSize" to r.tyreSize, "dot" to r.dot, "recommendedPressureBar" to r.recommendedPressureBar, "position" to r.position, "installedDate" to r.installedDate, "installedKm" to r.installedKm,
        "result" to r.result, "nextDate" to r.nextDate, "minorDefects" to r.minorDefects, "majorDefects" to r.majorDefects,
        "provider" to r.provider, "policy" to r.policy, "coverage" to r.coverage, "startDate" to r.startDate, "endDate" to r.endDate, "autoRenew" to r.autoRenew,
        "roadsideAssistance" to r.roadsideAssistance, "taxYear" to r.taxYear, "paid" to r.paid,
        "reminderByDate" to r.reminderByDate, "reminderByKm" to r.reminderByKm, "reminderLeadKm" to r.reminderLeadKm,
        "reminderLeadDays" to r.reminderLeadDays, "reminderEnabled" to r.reminderEnabled
    )

    private fun DocumentSnapshot.date(field: String): Date? = when (val value = get(field)) {
        is Timestamp -> value.toDate()
        is Date -> value
        else -> null
    }
}
