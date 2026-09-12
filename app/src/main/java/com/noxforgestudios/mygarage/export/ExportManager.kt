package com.noxforgestudios.mygarage.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.noxforgestudios.mygarage.domain.GarageRecord
import com.noxforgestudios.mygarage.domain.Vehicle
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ExportManager(private val context: Context) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
    private val money = NumberFormat.getCurrencyInstance(Locale("es", "ES"))

    fun exportCsv(vehicle: Vehicle, records: List<GarageRecord>): Result<File> = runCatching {
        val file = outputFile(vehicle, "csv")
        file.bufferedWriter().use { w ->
            w.appendLine("tipo;fecha;km;titulo;categoria;coste;estado;notas")
            records.sortedByDescending { it.date }.forEach { r ->
                w.appendLine(listOf(r.kind.displayName, dateFormat.format(r.date), r.odometerKm ?: "", r.title, r.category, r.cost, r.status, r.notes)
                    .joinToString(";") { csvEscape(it.toString()) })
            }
        }
        file
    }

    fun exportPdf(vehicle: Vehicle, records: List<GarageRecord>): Result<File> = runCatching {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headingPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10f }
        val mutedPaint = Paint().apply { textSize = 9f; alpha = 180 }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas
        var y = 52f
        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            y = 45f
        }
        fun ensure(height: Float) { if (y + height > 800f) newPage() }
        canvas.drawText("Mi Garaje · Historial del vehículo", 40f, y, titlePaint); y += 30f
        canvas.drawText(vehicle.title, 40f, y, headingPaint); y += 18f
        canvas.drawText("Matrícula: ${vehicle.plate.ifBlank { "—" }}   Año: ${vehicle.year ?: "—"}   Kilómetros: ${vehicle.odometerKm} km", 40f, y, bodyPaint); y += 28f
        val total = records.sumOf { it.cost }
        canvas.drawText("Gasto registrado: ${money.format(total)} · Registros: ${records.size}", 40f, y, bodyPaint); y += 28f
        canvas.drawLine(40f, y, 555f, y, mutedPaint); y += 18f
        records.sortedByDescending { it.date }.forEach { r ->
            ensure(52f)
            canvas.drawText("${dateFormat.format(r.date)} · ${r.kind.displayName} · ${r.odometerKm?.let { "$it km" } ?: "sin km"}", 40f, y, headingPaint); y += 15f
            canvas.drawText(r.title.ifBlank { r.kind.displayName }.take(72), 40f, y, bodyPaint); y += 14f
            val details = buildString {
                if (r.cost > 0) append("${money.format(r.cost)}  ")
                if (r.category.isNotBlank()) append("${r.category}  ")
                if (r.status.isNotBlank()) append(r.status)
            }
            if (details.isNotBlank()) { canvas.drawText(details.take(90), 40f, y, mutedPaint); y += 13f }
            if (r.notes.isNotBlank()) { canvas.drawText(r.notes.replace('\n',' ').take(95), 40f, y, mutedPaint); y += 13f }
            y += 8f
        }
        document.finishPage(page)
        val file = outputFile(vehicle, "pdf")
        file.outputStream().use(document::writeTo)
        document.close()
        file
    }

    fun share(file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir con").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun outputFile(vehicle: Vehicle, ext: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = vehicle.title.replace(Regex("[^A-Za-z0-9_-]+"), "_").take(40)
        return File(dir, "MiGaraje_${safe}_${System.currentTimeMillis()}.$ext")
    }

    private fun csvEscape(value: String): String = if (value.any { it == ';' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
}
