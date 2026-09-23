package com.socialaiassistant.keyboard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialaiassistant.keyboard.settingsui.SettingsCategoryId
import com.socialaiassistant.keyboard.settingsui.SettingsThemeCatalog
import com.socialaiassistant.keyboard.settingsui.SettingsThemeDashboardRenderer
import com.socialaiassistant.keyboard.settingsui.SettingsThemeRepository
import com.socialaiassistant.keyboard.settingsui.SettingsThemeStyler
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OPEN_SECTION = "social_ai_open_section"
        const val SECTION_AI_PRIVACY = "ai_privacy"
    }

    private lateinit var settingsThemeRepository: SettingsThemeRepository
    private lateinit var settingsThemeStyler: SettingsThemeStyler
    private lateinit var settingsThemeDashboardRenderer: SettingsThemeDashboardRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settingsThemeRepository = SettingsThemeRepository.create(this)
        settingsThemeStyler = SettingsThemeStyler(this)
        settingsThemeDashboardRenderer = SettingsThemeDashboardRenderer(this)

        val requested = SettingsCategoryId.fromLegacySection(intent.getStringExtra(EXTRA_OPEN_SECTION))
        if (requested != null) {
            startActivity(SettingsCategoryActivity.createIntent(this, requested))
            finish()
            return
        }

        lifecycleScope.launch {
            if (!settingsThemeRepository.isInitialSetupComplete()) {
                startActivity(Intent(this@MainActivity, SettingsThemeOnboardingActivity::class.java))
                finish()
            } else {
                applySettingsTheme()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::settingsThemeRepository.isInitialized) return
        lifecycleScope.launch {
            if (settingsThemeRepository.isInitialSetupComplete()) {
                applySettingsTheme()
            }
        }
    }

    private suspend fun applySettingsTheme() {
        val pack = settingsThemeRepository.currentPack()
        val spec = SettingsThemeCatalog.spec(pack)

        window.statusBarColor = spec.rootTop
        window.navigationBarColor = spec.rootBottom

        settingsThemeDashboardRenderer.render(
            findViewById(R.id.settings_theme_dashboard_container),
            pack,
            ::openSettingsCategory
        )

        settingsThemeStyler.apply(
            root = findViewById(R.id.settings_scroll),
            sections = emptyList(),
            headerTitle = findViewById(R.id.settings_header_title),
            headerEyebrow = findViewById(R.id.settings_header_eyebrow),
            headerSubtitle = findViewById(R.id.settings_header_subtitle),
            status = findViewById(R.id.status_settings_theme),
            pack = pack
        )

        findViewById<TextView>(R.id.status_settings_theme).text =
            "Settings theme: ${pack.displayName} • choose a category below"
    }

    private fun openSettingsCategory(category: SettingsCategoryId) {
        startActivity(SettingsCategoryActivity.createIntent(this, category))
    }
}
