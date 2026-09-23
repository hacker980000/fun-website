package com.socialaiassistant.keyboard.ime

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent

class VoiceInputActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return

        val languageTag = intent.getStringExtra(EXTRA_LANGUAGE).orEmpty()
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            if (languageTag.isNotBlank()) putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(recognizerIntent, REQUEST_SPEECH)
        } catch (_: ActivityNotFoundException) {
            VoiceResultBus.publish(VoiceResult.Cancelled(readDeferredTarget()))
            finish()
        }
    }

    @Deprecated("Deprecated in Android API but retained for minSdk-compatible speech activity result handling")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SPEECH) return

        val text = if (resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
        } else {
            ""
        }
        val target = readDeferredTarget()
        if (text.isNotEmpty() && target != null) {
            VoiceResultBus.publish(VoiceResult.Text(text, target))
        } else {
            VoiceResultBus.publish(VoiceResult.Cancelled(target))
        }
        finish()
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

    companion object {
        const val EXTRA_LANGUAGE = "voice_language"
        private const val REQUEST_SPEECH = 4107
    }
}
