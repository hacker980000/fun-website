package com.socialaiassistant.keyboard

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialaiassistant.keyboard.settingsui.SettingsThemeCatalog
import com.socialaiassistant.keyboard.settingsui.SettingsThemePack
import com.socialaiassistant.keyboard.settingsui.SettingsThemePreviewView
import com.socialaiassistant.keyboard.settingsui.SettingsThemeRepository
import com.socialaiassistant.keyboard.settingsui.SettingsThemeStyler
import com.socialaiassistant.keyboard.theme.ThemeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SettingsThemeOnboardingActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_MANUAL_CHANGE = "settings_theme_manual_change"
    }

    private lateinit var repository: SettingsThemeRepository
    private lateinit var keyboardThemeRepository: ThemeRepository
    private lateinit var styler: SettingsThemeStyler
    private var selectionInProgress = true
    private var manualChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_theme_onboarding)

        repository = SettingsThemeRepository.create(this)
        keyboardThemeRepository = ThemeRepository.create(this)
        styler = SettingsThemeStyler(this)
        manualChange = intent.getBooleanExtra(EXTRA_MANUAL_CHANGE, false)

        lifecycleScope.launch {
            val keyboardPack = keyboardThemeRepository.currentSelection().globalPack
            val recommended = SettingsThemePack.recommendedFor(keyboardPack)
            val currentState = repository.currentState()

            if (!manualChange && currentState.initialSetupComplete) {
                openMainSettings()
                return@launch
            }

            findViewById<TextView>(R.id.settings_theme_onboarding_title).text =
                if (manualChange) "Change Settings Theme" else "Choose Settings Theme"
            findViewById<TextView>(R.id.settings_theme_onboarding_subtitle).text =
                if (manualChange) {
                    "Switch the Settings panel design without changing your keyboard layout theme."
                } else {
                    "Step 2 of setup • Pick one premium Settings design. All Settings features remain available in every pack."
                }
            findViewById<TextView>(R.id.settings_theme_recommendation).text = buildString {
                append("Recommended match: ${recommended.displayName}")
                keyboardPack?.let { append(" for ${it.displayName}") }
                append(" • You can still choose any pack.")
            }

            setupPackCards(recommended, currentState.selectedPack)
            selectionInProgress = false
        }
    }

    private fun setupPackCards(recommended: SettingsThemePack, current: SettingsThemePack?) {
        val container = findViewById<LinearLayout>(R.id.settings_theme_onboarding_pack_container)
        container.removeAllViews()

        SettingsThemePack.entries.forEach { pack ->
            val spec = SettingsThemeCatalog.spec(pack)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = styler.dashboardCard(spec, if (pack == recommended) spec.accent2 else spec.border, stronger = true)
            }

            val titleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            titleRow.addView(TextView(this).apply {
                text = pack.displayName
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(spec.textPrimary)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            if (pack == recommended) {
                titleRow.addView(TextView(this).apply {
                    text = "RECOMMENDED"
                    textSize = 9f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    setPadding(dp(8), dp(5), dp(8), dp(5))
                    background = styler.accentTile(spec.accent, 999f)
                })
            }
            card.addView(titleRow)

            card.addView(TextView(this).apply {
                text = pack.subtitle
                textSize = 12f
                setTextColor(spec.textSecondary)
                setPadding(0, dp(4), 0, dp(8))
            })

            val preview = SettingsThemePreviewView(this).apply { setPack(pack) }
            card.addView(
                preview,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(148))
            )

            val button = Button(this).apply {
                isAllCaps = false
                text = when {
                    manualChange && current == pack -> "Current • ${pack.displayName}"
                    else -> "Use ${pack.displayName}"
                }
                setTextColor(spec.textPrimary)
                background = styler.buttonBackground(spec)
                stateListAnimator = null
                setOnClickListener { choosePack(pack) }
            }
            card.addView(
                button,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply {
                    topMargin = dp(9)
                }
            )

            container.addView(
                card,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(7), 0, dp(7))
                }
            )
        }
    }

    private fun choosePack(pack: SettingsThemePack) {
        if (selectionInProgress) return
        selectionInProgress = true
        lifecycleScope.launch {
            try {
                if (manualChange) repository.setPack(pack) else repository.completeInitialSetup(pack)
                if (manualChange) {
                    Toast.makeText(this@SettingsThemeOnboardingActivity, "${pack.displayName} Settings theme applied.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    openMainSettings()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                selectionInProgress = false
                Toast.makeText(
                    this@SettingsThemeOnboardingActivity,
                    error.message ?: "Could not save Settings theme.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openMainSettings() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
