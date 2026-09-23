package com.socialaiassistant.keyboard.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThemeBackgroundManager(
    private val context: Context
) {
    private val directory = File(context.filesDir, "theme_backgrounds")
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun importPhoto(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        var outputFile: File? = null
        var sourceBitmap: Bitmap? = null
        var normalizedBitmap: Bitmap? = null
        try {
            directory.mkdirs()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open(uri).use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported or unreadable image." }

            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMPORT_WIDTH, MAX_IMPORT_HEIGHT)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val source = open(uri).use { stream -> BitmapFactory.decodeStream(stream, null, options) }
                ?: error("Image decode failed.")
            sourceBitmap = source
            val normalized = scaleInside(source, MAX_IMPORT_WIDTH, MAX_IMPORT_HEIGHT)
            normalizedBitmap = normalized
            if (normalized !== source) {
                source.recycle()
                sourceBitmap = null
            }

            val temp = File(directory, ".incoming-${UUID.randomUUID()}.tmp")
            tempFile = temp
            temp.outputStream().buffered().use { output ->
                check(normalized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "Image encode failed."
                }
            }
            normalized.recycle()
            normalizedBitmap = null

            val fileName = "keyboard-bg-${System.currentTimeMillis()}-${UUID.randomUUID()}.jpg"
            val final = File(directory, fileName)
            outputFile = final
            if (!temp.renameTo(final)) {
                temp.copyTo(final, overwrite = true)
                temp.delete()
            }
            require(final.isFile && final.length() > 0L) { "Image save failed." }
            cache.evictAll()
            Result.success(fileName)
        } catch (error: CancellationException) {
            tempFile?.delete()
            outputFile?.delete()
            throw error
        } catch (error: Throwable) {
            tempFile?.delete()
            outputFile?.delete()
            Result.failure(error)
        } finally {
            normalizedBitmap?.takeUnless { it.isRecycled }?.recycle()
            sourceBitmap?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    suspend fun loadDisplayBitmap(
        config: BackgroundPhotoConfig,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val normalized = config.normalized()
        if (!normalized.enabled || normalized.localFileName.isBlank()) return@withContext null
        val file = privateFile(normalized.localFileName)
        if (!file.isFile) return@withContext null
        val width = targetWidth.coerceIn(160, 1600)
        val height = targetHeight.coerceIn(160, 1000)
        val key = "${normalized.localFileName}|$width|$height|${normalized.blurAmount}"
        cache.get(key)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
            cache.remove(key)
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, width, height)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@withContext null
        val scaled = scaleInside(decoded, width, height)
        if (scaled !== decoded) decoded.recycle()
        val result = if (normalized.blurAmount > 0) {
            val blurred = boxBlur(scaled, normalized.blurAmount)
            if (blurred !== scaled) scaled.recycle()
            blurred
        } else {
            scaled
        }
        cache.put(key, result)
        result
    }

    suspend fun removePhoto(fileName: String?) = withContext(Dispatchers.IO) {
        fileName?.takeIf { it.isNotBlank() }?.let { privateFile(it).delete() }
        cache.evictAll()
    }

    /** Drop decoded background bitmaps under memory pressure; source files stay untouched. */
    fun clearMemoryCache() {
        cache.evictAll()
    }

    fun exists(fileName: String?): Boolean =
        !fileName.isNullOrBlank() && privateFile(fileName).isFile

    private fun privateFile(fileName: String): File {
        val safeName = fileName.substringAfterLast('/').substringAfterLast('\\')
        return File(directory, safeName)
    }

    private fun open(uri: Uri): InputStream {
        return if (uri.scheme == "file") {
            FileInputStream(requireNotNull(uri.path))
        } else {
            context.contentResolver.openInputStream(uri) ?: error("Unable to open selected image.")
        }
    }

    private fun calculateInSampleSize(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var sample = 1
        while (sourceWidth / (sample * 2) >= targetWidth && sourceHeight / (sample * 2) >= targetHeight) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun scaleInside(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) return bitmap
        val ratio = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun boxBlur(source: Bitmap, radiusValue: Int): Bitmap {
        val radius = radiusValue.coerceIn(1, 30)
        val width = source.width
        val height = source.height
        if (width <= 1 || height <= 1) return source.copy(Bitmap.Config.ARGB_8888, false)

        val input = IntArray(width * height)
        val horizontal = IntArray(input.size)
        val output = IntArray(input.size)
        source.getPixels(input, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            val row = y * width
            for (x in -radius..radius) {
                val color = input[row + x.coerceIn(0, width - 1)]
                a += color ushr 24 and 0xFF
                r += color ushr 16 and 0xFF
                g += color ushr 8 and 0xFF
                b += color and 0xFF
            }
            val count = radius * 2 + 1
            for (x in 0 until width) {
                horizontal[row + x] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
                val remove = input[row + (x - radius).coerceIn(0, width - 1)]
                val add = input[row + (x + radius + 1).coerceIn(0, width - 1)]
                a += (add ushr 24 and 0xFF) - (remove ushr 24 and 0xFF)
                r += (add ushr 16 and 0xFF) - (remove ushr 16 and 0xFF)
                g += (add ushr 8 and 0xFF) - (remove ushr 8 and 0xFF)
                b += (add and 0xFF) - (remove and 0xFF)
            }
        }

        for (x in 0 until width) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            for (y in -radius..radius) {
                val color = horizontal[y.coerceIn(0, height - 1) * width + x]
                a += color ushr 24 and 0xFF
                r += color ushr 16 and 0xFF
                g += color ushr 8 and 0xFF
                b += color and 0xFF
            }
            val count = radius * 2 + 1
            for (y in 0 until height) {
                output[y * width + x] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
                val remove = horizontal[(y - radius).coerceIn(0, height - 1) * width + x]
                val add = horizontal[(y + radius + 1).coerceIn(0, height - 1) * width + x]
                a += (add ushr 24 and 0xFF) - (remove ushr 24 and 0xFF)
                r += (add ushr 16 and 0xFF) - (remove ushr 16 and 0xFF)
                g += (add ushr 8 and 0xFF) - (remove ushr 8 and 0xFF)
                b += (add and 0xFF) - (remove and 0xFF)
            }
        }

        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val MAX_IMPORT_WIDTH = 1600
        const val MAX_IMPORT_HEIGHT = 1200
        const val JPEG_QUALITY = 90
    }
}
