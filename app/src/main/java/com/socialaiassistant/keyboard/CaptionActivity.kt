package com.socialaiassistant.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialaiassistant.keyboard.ai.ExtensionLanguageLogic
import com.socialaiassistant.keyboard.ai.LanguageMode
import com.socialaiassistant.keyboard.backend.BackendErrorMessages
import com.socialaiassistant.keyboard.backend.BackendException
import com.socialaiassistant.keyboard.backend.ManagedAiPayload
import com.socialaiassistant.keyboard.ime.CaptionDraftBus
import com.socialaiassistant.keyboard.ime.DeferredInsertionExtras
import com.socialaiassistant.keyboard.ime.DeferredInsertionTarget
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CaptionActivity : AppCompatActivity() {
    private val selectedUris = mutableListOf<Uri>()
    private lateinit var modeSpinner: Spinner
    private lateinit var contextInput: EditText
    private lateinit var imageStatus: TextView
    private lateinit var output: EditText
    private lateinit var generateButton: Button
    private lateinit var insertButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(24))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "Write Caption"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Social AI Assistant Pro v35.3.1 caption engine. Draft only — posting/sending is always manual."
            setPadding(0, dp(8), 0, dp(12))
        })

        modeSpinner = Spinner(this)
        val modes = listOf("Romantic Caption", "Funny Caption", "Emotional Caption", "Photo Caption")
        modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        root.addView(modeSpinner)

        contextInput = EditText(this).apply {
            hint = "Optional composer context / words to include"
            minLines = 2
            maxLines = 5
            gravity = Gravity.TOP or Gravity.START
        }
        root.addView(contextInput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        root.addView(Button(this).apply {
            text = "Select photos (max 4)"
            setOnClickListener { openPhotoPicker() }
        })
        imageStatus = TextView(this).apply { text = "No photos selected" }
        root.addView(imageStatus)

        generateButton = Button(this).apply {
            text = "Generate caption"
            setOnClickListener { generateCaption() }
        }
        root.addView(generateButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        output = EditText(this).apply {
            hint = "Generated caption appears here"
            minLines = 3
            maxLines = 7
            gravity = Gravity.TOP or Gravity.START
        }
        root.addView(output)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Copy"
            setOnClickListener {
                val value = output.text?.toString().orEmpty().trim()
                if (value.isNotEmpty()) {
                    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("AI caption", value))
                    Toast.makeText(this@CaptionActivity, "Caption copied.", Toast.LENGTH_SHORT).show()
                }
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        insertButton = Button(this).apply {
            text = "Insert into current field"
            setOnClickListener {
                val value = output.text?.toString().orEmpty().trim()
                if (value.isNotEmpty()) {
                    val target = readDeferredTarget()
                    if (target == null) {
                        Toast.makeText(
                            this@CaptionActivity,
                            "Original keyboard field is no longer available. Use Copy instead.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    CaptionDraftBus.publish(value, target)
                    Toast.makeText(this@CaptionActivity, "Caption ready for the original field.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        actions.addView(insertButton, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions)
        return scroll
    }

    private fun openPhotoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_IMAGES)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_IMAGES || resultCode != RESULT_OK || data == null) return
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) uris += clip.getItemAt(i).uri
        }
        data.data?.let { uris += it }
        selectedUris.clear()
        selectedUris += uris.distinct().take(4)
        imageStatus.text = if (selectedUris.isEmpty()) "No photos selected" else "${selectedUris.size} photo(s) selected"
    }

    private fun generateCaption() {
        val app = application as SocialAiApplication
        if (!app.managedSessionStore.isLoggedIn()) {
            Toast.makeText(this, "Photo/Write Caption ব্যবহার করতে Managed AI Login করুন।", Toast.LENGTH_LONG).show()
            return
        }
        val mode = when (modeSpinner.selectedItemPosition) {
            0 -> "ROMANTIC"
            1 -> "FUNNY"
            2 -> "EMOTIONAL"
            else -> "PHOTO"
        }
        if (mode == "PHOTO" && selectedUris.isEmpty()) {
            Toast.makeText(this, "Photo Caption-এর জন্য অন্তত ১টি ছবি নির্বাচন করুন।", Toast.LENGTH_LONG).show()
            return
        }

        generateButton.isEnabled = false
        generateButton.text = "Preparing…"
        lifecycleScope.launch {
            try {
                val settings = app.settingsRepository.current()
                if (!settings.aiPrivacyConsent) {
                    Toast.makeText(
                        this@CaptionActivity,
                        "Photo/Write Caption ব্যবহার করতে আগে Settings-এ Privacy/Data consent দিন।",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                val imageData = if (mode == "PHOTO") {
                    withContext(Dispatchers.IO) { selectedUris.take(4).map { encodeSafeJpeg(it) } }
                } else emptyList()
                generateButton.text = "Generating…"
                val contextText = contextInput.text?.toString().orEmpty().trim()
                val personalTraining = listOf(app.currentManagedPersonalTraining(), settings.customInstruction)
                    .filter { it.isNotBlank() }.joinToString("\n").take(1800)
                val payload = ManagedAiPayload.caption(
                    mode = mode,
                    contextText = contextText,
                    captionLanguageMode = captionLanguageMode(contextText),
                    imageDataUrls = imageData,
                    personalTraining = personalTraining
                )
                val result = app.backendClient.generate(payload)
                output.setText(result.reply)
            } catch (error: CancellationException) {
                throw error
            } catch (error: BackendException) {
                Toast.makeText(this@CaptionActivity, BackendErrorMessages.userMessage(error), Toast.LENGTH_LONG).show()
            } catch (error: Throwable) {
                Toast.makeText(this@CaptionActivity, error.message ?: "Caption generation failed.", Toast.LENGTH_LONG).show()
            } finally {
                generateButton.isEnabled = true
                generateButton.text = "Generate caption"
            }
        }
    }

    private fun captionLanguageMode(text: String): String = when (ExtensionLanguageLogic.detectLanguageMode(text)) {
        LanguageMode.BANGLISH -> "BANGLISH"
        LanguageMode.LATIN_INFER -> "LATIN_INFER"
        LanguageMode.SOURCE_LANGUAGE -> "SOURCE_LANGUAGE"
        LanguageMode.BENGALI, LanguageMode.BENGALI_DEFAULT, null -> "BENGALI_DEFAULT"
    }

    private fun encodeSafeJpeg(uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1800 || bounds.outHeight / sample > 1800) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        val decoded = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Could not read ${displayName(uri)}")
        var bitmap = scaleToMax(decoded, 1280)
        if (bitmap !== decoded) decoded.recycle()

        var quality = 88
        var bytes = compress(bitmap, quality)
        while (bytes.size > MAX_JPEG_BYTES && quality > 50) {
            quality -= 7
            bytes = compress(bitmap, quality)
        }
        while (bytes.size > MAX_JPEG_BYTES && bitmap.width > 640 && bitmap.height > 640) {
            val smaller = Bitmap.createScaledBitmap(bitmap, (bitmap.width * 0.82f).toInt(), (bitmap.height * 0.82f).toInt(), true)
            if (smaller !== bitmap) bitmap.recycle()
            bitmap = smaller
            quality = 72
            bytes = compress(bitmap, quality)
        }
        bitmap.recycle()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    private fun scaleToMax(source: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxSide) return source
        val scale = maxSide.toFloat() / longest.toFloat()
        return Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
    }

    private fun compress(bitmap: Bitmap, quality: Int): ByteArray = ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0) ?: "image"
        }
        return "image"
    }

    private fun readDeferredTarget(): DeferredInsertionTarget? {
        val packageName = intent.getStringExtra(DeferredInsertionExtras.PACKAGE) ?: return null
        val createdAt = intent.getLongExtra(DeferredInsertionExtras.CREATED_AT_NANOS, -1L)
        if (createdAt <= 0L) return null
        return DeferredInsertionTarget(
            packageName = packageName,
            inputType = intent.getIntExtra(DeferredInsertionExtras.INPUT_TYPE, 0),
            fieldId = intent.getIntExtra(DeferredInsertionExtras.FIELD_ID, 0),
            hintText = intent.getStringExtra(DeferredInsertionExtras.HINT),
            createdAtNanos = createdAt
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_IMAGES = 35031
        private const val MAX_JPEG_BYTES = 700 * 1024
    }
}
