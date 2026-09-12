package com.noxforgestudios.mygarage.data

import android.content.Context
import com.noxforgestudios.mygarage.domain.VehicleCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

/** Public vPIC catalogue complements the European offline catalogue; no account data is sent. */
class VehicleCatalogRepository(context: Context) {
    private val cacheDir = File(context.cacheDir, "vehicle_catalog")

    suspend fun makes(): List<String> = withContext(Dispatchers.IO) {
        merge(VehicleCatalog.models.keys.toList(), fetch("GetMakesForVehicleType/car", "MakeName"))
    }

    suspend fun models(make: String): List<String> = withContext(Dispatchers.IO) {
        if (make.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(make, "UTF-8").replace("+", "%20")
        merge(VehicleCatalog.modelsFor(make), fetch("GetModelsForMake/$encoded", "Model_Name"))
    }

    private fun merge(local: List<String>, remote: List<String>) = (local + remote)
        .filter(String::isNotBlank).distinctBy(VehicleCatalog::searchKey).sortedBy(VehicleCatalog::searchKey)

    private fun fetch(endpoint: String, field: String): List<String> {
        val cache = File(cacheDir, endpoint.hashCode().toUInt().toString() + ".json")
        fun parse(text: String): List<String> {
            val rows = JSONObject(text).getJSONArray("Results")
            return (0 until rows.length()).map { rows.getJSONObject(it).optString(field).trim() }
        }
        val cached = runCatching { parse(cache.readText()) }.getOrDefault(emptyList())
        if (cached.isNotEmpty() && System.currentTimeMillis() - cache.lastModified() < 30L * 24 * 60 * 60 * 1000) return cached
        return runCatching {
            val connection = URI("https://vpic.nhtsa.dot.gov/api/vehicles/$endpoint?format=json").toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                check(connection.responseCode == 200)
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val values = parse(text)
                runCatching { cacheDir.mkdirs(); cache.writeText(text) }
                values
            } finally { connection.disconnect() }
        }.getOrDefault(cached)
    }
}
