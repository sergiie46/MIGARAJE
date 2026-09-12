package com.noxforgestudios.mygarage.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.noxforgestudios.mygarage.domain.GarageRecord
import com.noxforgestudios.mygarage.domain.RecordKind
import com.noxforgestudios.mygarage.domain.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

object VehicleCardGenerator {
    enum class Style { MODERNO, DEPORTIVO, MINIMAL }

    data class Options(
        val style: Style = Style.MODERNO,
        val includeSpecs: Boolean = true,
        val includeMods: Boolean = true
    )

    suspend fun generate(
        context: Context,
        vehicle: Vehicle,
        records: List<GarageRecord>,
        options: Options
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bg = when (options.style) {
                Style.MODERNO -> Color.rgb(15, 18, 22)
                Style.DEPORTIVO -> Color.rgb(10, 10, 12)
                Style.MINIMAL -> Color.rgb(24, 25, 27)
            }
            val accent = when (options.style) {
                Style.MODERNO -> Color.rgb(238, 47, 51)
                Style.DEPORTIVO -> Color.rgb(255, 38, 45)
                Style.MINIMAL -> Color.rgb(210, 213, 218)
            }
            canvas.drawColor(bg)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = accent
            canvas.drawRect(0f, 0f, 28f, height.toFloat(), paint)
            canvas.drawRect(58f, 50f, 260f, 62f, paint)

            drawText(canvas, paint, "MI GARAJE", 58f, 115f, 52f, Color.WHITE, true)
            drawText(canvas, paint, "NOXFORGE STUDIOS", 60f, 150f, 20f, Color.LTGRAY, false)

            val title = vehicle.title.uppercase(Locale.getDefault())
            drawText(canvas, paint, title, 58f, 225f, 56f, Color.WHITE, true, 940f)
            val subtitle = listOfNotNull(
                vehicle.generation.takeIf { it.isNotBlank() },
                vehicle.year?.toString(),
                vehicle.nickname.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) drawText(canvas, paint, subtitle, 60f, 265f, 25f, accent, true, 940f)

            val photos = buildList {
                vehicle.localPhotoPath?.let(::add)
                addAll(vehicle.galleryLocalPaths)
            }.distinct().filter { File(it).exists() }

            val heroTop = 300f
            val heroBottom = 720f
            if (photos.isNotEmpty()) {
                decodeBitmap(photos.first())?.let { photo ->
                    drawCenterCrop(canvas, photo, RectF(58f, heroTop, 1022f, heroBottom))
                    photo.recycle()
                }
            } else {
                paint.color = Color.rgb(35, 38, 43)
                canvas.drawRoundRect(RectF(58f, heroTop, 1022f, heroBottom), 24f, 24f, paint)
                drawText(canvas, paint, "AÑADE UNA FOTO DEL COCHE", 250f, 520f, 30f, Color.LTGRAY, true, 600f)
            }

            val thumbs = photos.drop(1).take(3)
            if (thumbs.isNotEmpty()) {
                val gap = 14f
                val thumbW = (964f - gap * 2f) / 3f
                thumbs.forEachIndexed { i, path ->
                    decodeBitmap(path)?.let { photo ->
                        val left = 58f + i * (thumbW + gap)
                        drawCenterCrop(canvas, photo, RectF(left, 735f, left + thumbW, 875f))
                        photo.recycle()
                    }
                }
            }

            val sectionTop = if (thumbs.isNotEmpty()) 920f else 770f
            val leftX = 58f
            val rightX = 570f
            if (options.includeSpecs) {
                drawText(canvas, paint, "ESPECIFICACIONES", leftX, sectionTop, 28f, Color.WHITE, true)
                canvas.drawRect(leftX, sectionTop + 12f, leftX + 380f, sectionTop + 18f, paint.apply { color = accent })
                val specs = specs(vehicle)
                specs.take(7).forEachIndexed { index, pair ->
                    val y = sectionTop + 60f + index * 43f
                    drawText(canvas, paint, pair.first, leftX, y, 22f, Color.LTGRAY, false, 190f)
                    drawText(canvas, paint, pair.second, leftX + 205f, y, 22f, Color.WHITE, true, 270f)
                }
            }

            if (options.includeMods) {
                drawText(canvas, paint, "MODIFICACIONES", rightX, sectionTop, 28f, Color.WHITE, true)
                canvas.drawRect(rightX, sectionTop + 12f, rightX + 360f, sectionTop + 18f, paint.apply { color = accent })
                val mods = records.filter { it.kind == RecordKind.MODIFICATION }.sortedByDescending { it.date }.take(7)
                if (mods.isEmpty()) {
                    drawText(canvas, paint, "Sin modificaciones registradas", rightX, sectionTop + 70f, 21f, Color.LTGRAY, false, 430f)
                } else {
                    mods.forEachIndexed { index, mod ->
                        val y = sectionTop + 60f + index * 43f
                        val prefix = mod.category.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()
                        drawText(canvas, paint, "• ${prefix}${mod.title.ifBlank { mod.description.ifBlank { "Modificación" } }}", rightX, y, 20f, Color.WHITE, false, 445f)
                    }
                }
            }

            drawText(canvas, paint, "Más que un coche, una historia sobre ruedas", 58f, 1302f, 23f, Color.LTGRAY, false, 700f)
            drawText(canvas, paint, "BUILD · DRIVE · REPEAT", 745f, 1302f, 19f, accent, true, 280f)

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val out = File(dir, "MiGaraje_${vehicle.make}_${vehicle.model}_${System.currentTimeMillis()}.png".replace(" ", "_"))
            FileOutputStream(out).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "No se pudo guardar la imagen" }
            }
            bitmap.recycle()
            out
        }
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir ficha del vehículo"))
    }

    suspend fun saveToGallery(context: Context, source: File): Result<Uri?> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, source.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiGaraje")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("No se pudo crear la imagen en la galería")
                context.contentResolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
                    ?: error("No se pudo escribir la imagen")
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            } else {
                val dir = context.getExternalFilesDir("Pictures/MiGaraje") ?: context.filesDir
                dir.mkdirs()
                val target = File(dir, source.name)
                source.copyTo(target, overwrite = true)
                Uri.fromFile(target)
            }
        }
    }

    private fun specs(v: Vehicle): List<Pair<String, String>> = buildList {
        val motor = listOfNotNull(v.displacementCc?.let { "${it} cc" }, v.fuel.takeIf { it.isNotBlank() }).joinToString(" · ")
        if (motor.isNotBlank()) add("Motor" to motor)
        v.powerCv?.let { add("Potencia" to "$it CV") }
        if (v.transmission.isNotBlank()) add("Cambio" to v.transmission)
        if (v.traction.isNotBlank()) add("Tracción" to v.traction)
        if (v.color.isNotBlank()) add("Color" to v.color)
        add("Kilometraje" to NumberFormat.getIntegerInstance(Locale("es", "ES")).format(v.odometerKm) + " km")
        if (v.plate.isNotBlank()) add("Matrícula" to v.plate)
    }

    private fun decodeBitmap(path: String): Bitmap? = runCatching {
        BitmapFactory.Options().let { options -> BitmapFactory.decodeFile(path, options) }
    }.getOrNull()

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, dst: RectF) {
        val scale = max(dst.width() / bitmap.width, dst.height() / bitmap.height)
        val srcW = dst.width() / scale
        val srcH = dst.height() / scale
        val left = (bitmap.width - srcW) / 2f
        val top = (bitmap.height - srcH) / 2f
        canvas.drawBitmap(bitmap, RectF(left, top, left + srcW, top + srcH), dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawText(canvas: Canvas, paint: Paint, text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean, maxWidth: Float = Float.MAX_VALUE) {
        paint.textSize = size
        paint.color = color
        paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var value = text
        if (paint.measureText(value) > maxWidth) {
            while (value.length > 1 && paint.measureText("$value…") > maxWidth) value = value.dropLast(1)
            value += "…"
        }
        canvas.drawText(value, x, y, paint)
    }
}
