package com.socialaiassistant.keyboard

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        findViewById<Button>(R.id.button_clear_local_history).setOnClickListener {
            confirmClearHistory()
        }
        findViewById<Button>(R.id.button_privacy_done).setOnClickListener {
            finish()
        }
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_local_history_confirm_title)
            .setMessage(R.string.clear_local_history_confirm_body)
            .setNegativeButton(R.string.clear_local_history_cancel, null)
            .setPositiveButton(R.string.clear_local_history_confirm) { _, _ ->
                lifecycleScope.launch {
                    (application as SocialAiApplication).conversationRepository.clearAll()
                    Toast.makeText(
                        this@PrivacyActivity,
                        R.string.local_history_cleared,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }
}
