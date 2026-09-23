package com.socialaiassistant.keyboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialaiassistant.keyboard.theme.KeyboardThemeSurface
import com.socialaiassistant.keyboard.theme.ThemeButtonRole
import com.socialaiassistant.keyboard.theme.ThemeCatalog
import com.socialaiassistant.keyboard.theme.ThemePack
import com.socialaiassistant.keyboard.theme.ThemePreviewView
import com.socialaiassistant.keyboard.theme.ThemeRenderer
import com.socialaiassistant.keyboard.settingsui.SettingsThemeRepository
import com.socialaiassistant.keyboard.theme.ThemeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ThemeOnboardingActivity : AppCompatActivity() {
    private lateinit var repository: ThemeRepository
    private lateinit var renderer: ThemeRenderer
    private lateinit var settingsThemeRepository: SettingsThemeRepository
    private var selectionInProgress = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_onboarding)

        val app = application as? SocialAiApplication
        repository = app?.themeRepository ?: ThemeRepository.create(this)
        renderer = app?.themeRenderer ?: ThemeRenderer(this)
        settingsThemeRepository = SettingsThemeRepository.create(this)

        setupPackCards()

        lifecycleScope.launch {
            if (repository.isInitialThemeSetupComplete()) {
                openNextSetupStep()
            } else {
                selectionInProgress = false
            }
        }
    }

    private fun setupPackCards() {
        val container = findViewById<LinearLayout>(R.id.theme_onboarding_pack_container)
        container.removeAllViews()

        ThemePack.entries.forEach { pack ->
            val chromeTheme = ThemeCatalog.globalChrome(pack)
            val previewTheme = ThemeCatalog.surface(pack, KeyboardThemeSurface.ENGLISH)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = renderer.panelBackground(chromeTheme, previewTheme.primaryNeon)
            }
            val preview = ThemePreviewView(this).apply {
                setPreview(previewTheme, KeyboardThemeSurface.ENGLISH)
            }
            card.addView(
                preview,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(122)
                )
            )

            val selectButton = Button(this).apply {
                isAllCaps = false
                text = getString(R.string.theme_onboarding_use_pack, pack.displayName)
                setOnClickListener { choosePack(pack) }
            }
            renderer.styleButton(selectButton, ThemeButtonRole.ENTER, chromeTheme)
            card.addView(
                selectButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
                ).apply { topMargin = dp(7) }
            )

            container.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, dp(6), 0, dp(8)) }
            )
        }
    }

    private fun choosePack(pack: ThemePack) {
        if (selectionInProgress) return
        selectionInProgress = true
        lifecycleScope.launch {
            try {
                repository.completeInitialThemeSetup(pack)
                openSettingsThemeSetup()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                selectionInProgress = false
                Toast.makeText(
                    this@ThemeOnboardingActivity,
                    error.message ?: getString(R.string.theme_onboarding_save_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun openNextSetupStep() {
        if (settingsThemeRepository.isInitialSetupComplete()) {
            openMainSettings()
        } else {
            openSettingsThemeSetup()
        }
    }

    private fun openSettingsThemeSetup() {
        startActivity(Intent(this, SettingsThemeOnboardingActivity::class.java))
        finish()
    }

    private fun openMainSettings() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
