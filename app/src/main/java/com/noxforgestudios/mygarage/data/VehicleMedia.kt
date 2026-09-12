package com.noxforgestudios.mygarage.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.noxforgestudios.mygarage.domain.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.max

/** Imports into app-owned storage before a single vehicle write. Never treats video as a bitmap. */
class VehicleMedia(private val context: Context) {
    suspend fun prepare(vehicle: Vehicle, uid: String, main: Uri?, extras: List<Uri>): PreparedMedia = withContext(Dispatchers.IO) {
        val created = mutableListOf<File>()
        val dir = File(context.filesDir, "vehicle_photos/$uid").apply { check(mkdirs() || isDirectory) }
        fun target(extension: String) = File(dir, "${UUID.randomUUID()}.$extension").also { created += it }
        try {
            var updated = vehicle
            if (main != null) updated = updated.copy(localPhotoPath = image(main, target("jpg")).path, remotePhotoUrl = null)
            val photos = vehicle.galleryLocalPaths.toMutableList()
            val videos = vehicle.videoLocalPaths.toMutableList()
            for (uri in extras.distinct()) {
                coroutineContext.ensureActive()
                val mime = context.contentResolver.getType(uri).orEmpty()
                if (mime.startsWith("video/")) {
                    require(videos.size < 2) { "Puedes guardar hasta 2 vídeos por coche" }
                    val extension = when (mime) { "video/webm" -> "webm"; "video/3gpp" -> "3gp"; else -> "mp4" }
                    val file = target(extension)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().buffered().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var total = 0L
                            while (true) {
                                coroutineContext.ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= 100L * 1024 * 1024) { "El vídeo supera 100 MB. Selecciona uno más corto" }
                                output.write(buffer, 0, count)
                            }
                            require(total > 0) { "El vídeo está vacío" }
                        }
                    } ?: error("No se puede leer el vídeo. Vuelve a seleccionarlo")
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.path)
                        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        require(duration != null && duration in 1..120_000) { "El vídeo debe durar como máximo 2 minutos" }
                        require(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") { "El archivo no contiene vídeo" }
                    } finally { retriever.release() }
                    videos += file.path
                } else {
                    require(photos.size < 6) { "Puedes guardar hasta 6 fotos de galería por coche" }
                    photos += image(uri, target("jpg")).path
                }
            }
            PreparedMedia(updated.copy(galleryLocalPaths = photos, videoLocalPaths = videos), created)
        } catch (e: Throwable) {
            created.forEach { it.delete() }
            throw e
        }
    }

    private fun image(uri: Uri, target: File): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagen no válida. Selecciona una foto JPG, PNG, WEBP o HEIC compatible" }
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("No se pudo abrir la foto")
        var oriented = bitmap
        try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
            val matrix = Matrix().apply {
                if (orientation?.isFlipped == true) postScale(-1f, 1f)
                postRotate(orientation?.rotationDegrees?.toFloat() ?: 0f)
            }
            if (!matrix.isIdentity) oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            target.outputStream().buffered().use { check(oriented.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
        } finally {
            if (oriented !== bitmap) oriented.recycle()
            bitmap.recycle()
        }
        return target
    }
}

data class PreparedMedia(val vehicle: Vehicle, val createdFiles: List<File>) {
    fun discard() { createdFiles.forEach { it.delete() } }
}
