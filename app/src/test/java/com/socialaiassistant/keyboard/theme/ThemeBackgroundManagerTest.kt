package com.socialaiassistant.keyboard.theme

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeBackgroundManagerTest {
    @Test
    fun importPhoto_reencodesIntoPrivateStorage_andDropsTrailingMetadata() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val input = File(context.cacheDir, "source.jpg")
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        input.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        input.appendBytes("GPS-SECRET-METADATA".toByteArray())

        val manager = ThemeBackgroundManager(context)
        val result = manager.importPhoto(Uri.fromFile(input))
        assertTrue(result.isSuccess)
        val output = File(context.filesDir, "theme_backgrounds/${result.getOrThrow()}")
        assertTrue(output.exists())
        assertFalse(output.readBytes().toString(Charsets.ISO_8859_1).contains("GPS-SECRET-METADATA"))
    }

    @Test
    fun loadDisplayBitmap_returnsNull_whenPrivateFileIsMissing() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = ThemeBackgroundManager(context)
        val value = manager.loadDisplayBitmap(
            BackgroundPhotoConfig(true, "missing.jpg"),
            targetWidth = 800,
            targetHeight = 500
        )
        assertNotNull(Result.success(value))
        assertTrue(value == null)
    }
}
