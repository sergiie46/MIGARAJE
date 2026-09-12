package com.noxforgestudios.mygarage.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import com.noxforgestudios.mygarage.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

class PhotoRepository(
    private val context: Context,
    private val storage: FirebaseStorage?
) {
    fun createCameraUri(): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "camera_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun persistVehiclePhoto(source: Uri, uid: String, vehicleId: String): Result<Pair<String, String?>> = runCatching {
        val target = withContext(Dispatchers.IO) { compressLocally(source, vehicleId) }
        val remote = if (BuildConfig.FIREBASE_STORAGE_ENABLED && storage != null) {
            val ref = storage.reference.child("users/$uid/vehicles/$vehicleId/${target.name}")
            ref.putFile(Uri.fromFile(target)).await()
            ref.downloadUrl.await().toString()
        } else null
        target.absolutePath to remote
    }


    suspend fun persistVehiclePhotos(sources: List<Uri>, uid: String, vehicleId: String): Result<Pair<List<String>, List<String>>> = runCatching {
        val locals = mutableListOf<String>()
        val remotes = mutableListOf<String>()
        sources.take(6).forEach { source ->
            val target = withContext(Dispatchers.IO) { compressLocally(source, vehicleId) }
            locals += target.absolutePath
            if (BuildConfig.FIREBASE_STORAGE_ENABLED && storage != null) {
                val ref = storage.reference.child("users/$uid/vehicles/$vehicleId/gallery/${target.name}")
                ref.putFile(Uri.fromFile(target)).await()
                remotes += ref.downloadUrl.await().toString()
            }
        }
        locals to remotes
    }

    private fun compressLocally(source: Uri, vehicleId: String): File {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "Imagen no válida" }
        var sample = 1
        while (max(options.outWidth / sample, options.outHeight / sample) > 2400) sample *= 2
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(source).use { input ->
            BitmapFactory.decodeStream(input, null, decode)
        } ?: error("No se pudo decodificar la imagen")
        val maxDim = 1600
        val ratio = minOf(1f, maxDim.toFloat() / max(bitmap.width, bitmap.height).toFloat())
        val scaled = if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true) else bitmap
        val dir = File(context.filesDir, "vehicle_photos").apply { mkdirs() }
        val target = File(dir, "${vehicleId}_${UUID.randomUUID()}.jpg")
        target.outputStream().buffered().use { out -> check(scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)) { "No se pudo comprimir la imagen" } }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return target
    }

    suspend fun deleteUserPhotos(uid: String) {
        withContext(Dispatchers.IO) { File(context.filesDir, "vehicle_photos").deleteRecursively() }
        if (!BuildConfig.FIREBASE_STORAGE_ENABLED) return
        val firebaseStorage = storage ?: return
        runCatching { deleteRecursively(firebaseStorage.reference.child("users/$uid")) }
    }

    private suspend fun deleteRecursively(ref: com.google.firebase.storage.StorageReference) {
        val result = ref.listAll().await()
        result.items.forEach { runCatching { it.delete().await() } }
        result.prefixes.forEach { deleteRecursively(it) }
    }
}
