package com.noxforgestudios.mygarage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.exifinterface.media.ExifInterface
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import com.noxforgestudios.mygarage.data.VehicleMedia
import com.noxforgestudios.mygarage.domain.Vehicle
import com.noxforgestudios.mygarage.domain.VehicleCatalog
import com.noxforgestudios.mygarage.domain.ThemeMode
import com.noxforgestudios.mygarage.ui.SelectionField
import com.noxforgestudios.mygarage.ui.VehicleShowcase
import com.noxforgestudios.mygarage.ui.theme.MiGarajeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class MediaAndGarageTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun photoImportDownsamplesAndAppliesCameraRotation() = runBlocking {
        val source = File(context.cacheDir, "test-${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(3200, 1600, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }; bitmap.recycle()
        ExifInterface(source.path).apply { setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); saveAttributes() }
        val result = VehicleMedia(context).prepare(Vehicle(id = "test"), "test-user", Uri.fromFile(source), emptyList())
        try {
            val decoded = BitmapFactory.decodeFile(result.vehicle.localPhotoPath)
            assertTrue(decoded.width <= 1600 && decoded.height <= 1600)
            assertTrue(decoded.height > decoded.width)
            decoded.recycle()
        } finally { result.discard(); source.delete() }
    }

    @Test fun corruptAttachmentCleansUpNewFilesAndKeepsExistingGallery() = runBlocking {
        val uid = "test-${UUID.randomUUID()}"
        val source = File(context.cacheDir, "$uid.jpg").apply { writeText("not an image") }
        val existing = Vehicle(id = "test", galleryLocalPaths = listOf("existing.jpg"))
        try {
            val result = runCatching { VehicleMedia(context).prepare(existing, uid, null, listOf(Uri.fromFile(source))) }
            assertTrue(result.isFailure)
            assertEquals(listOf("existing.jpg"), existing.galleryLocalPaths)
            assertTrue(File(context.filesDir, "vehicle_photos/$uid").listFiles().orEmpty().isEmpty())
        } finally { source.delete(); File(context.filesDir, "vehicle_photos/$uid").delete() }
    }

    @Test fun searchableFuelSelectorAcceptsRacingFuelAndCustomEntries() {
        var selected = ""
        compose.setContent { MiGarajeTheme(ThemeMode.DARK) { SelectionField("Combustible", selected, VehicleCatalog.fuels) { selected = it } } }
        compose.onNodeWithContentDescription("Abrir Combustible").performClick()
        compose.onNodeWithText("Buscar").performTextInput("E85")
        compose.onNodeWithText(VehicleCatalog.fuels.first { it.contains("E85") }).performClick()
        compose.runOnIdle { assertTrue(selected.contains("E85")) }
        compose.onNodeWithContentDescription("Abrir Combustible").performClick()
        compose.onNodeWithText("Buscar").performTextInput("Mezcla de circuito personal")
        compose.onNodeWithText("Usar «Mezcla de circuito personal»").performClick()
        compose.runOnIdle { assertEquals("Mezcla de circuito personal", selected) }
    }

    @Test fun videoIsCopiedPlayableAndAppendedWithoutReplacingPhotos() = runBlocking {
        val source = File(context.cacheDir, "camera/test-${UUID.randomUUID()}.mp4")
        source.parentFile!!.mkdirs()
        InstrumentationRegistry.getInstrumentation().context.assets.open("sample.mp4").use { input -> source.outputStream().use { input.copyTo(it) } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", source)
        val original = Vehicle(id = "test", galleryLocalPaths = listOf("old-photo.jpg"))
        val result = VehicleMedia(context).prepare(original, "test-user", null, listOf(uri))
        try {
            assertEquals(original.galleryLocalPaths, result.vehicle.galleryLocalPaths)
            assertEquals(1, result.vehicle.videoLocalPaths.size)
            assertArrayEquals(source.readBytes(), File(result.vehicle.videoLocalPaths.single()).readBytes())
        } finally { result.discard(); source.delete() }
    }

    @Test fun corruptVideoProducesAnErrorWithoutLeavingPartialFiles() = runBlocking {
        val uid = "video-${UUID.randomUUID()}"
        val source = File(context.cacheDir, "camera/$uid.mp4")
        source.parentFile!!.mkdirs(); source.writeText("invalid video")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", source)
        try {
            assertTrue(runCatching { VehicleMedia(context).prepare(Vehicle(id = "test"), uid, null, listOf(uri)) }.isFailure)
            assertTrue(File(context.filesDir, "vehicle_photos/$uid").listFiles().orEmpty().isEmpty())
        } finally { source.delete(); File(context.filesDir, "vehicle_photos/$uid").delete() }
    }

    @Test fun changingCardSelectsTheVehicleWhoseDetailsWillOpen() {
        var selected = "a"
        compose.setContent {
            MiGarajeTheme(ThemeMode.DARK) {
                VehicleShowcase(listOf(Vehicle(id = "a", make = "BMW", model = "Serie 3"), Vehicle(id = "b", make = "SEAT", model = "León")), selected,
                    onSelect = { selected = it }, onOpen = {}, onShare = {})
            }
        }
        compose.onNodeWithContentDescription("Siguiente vehículo").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("b", selected) }
        compose.onNodeWithText("León").assertIsDisplayed()
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.filesDir, "garage-preview.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
