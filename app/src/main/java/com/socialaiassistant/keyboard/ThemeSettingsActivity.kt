package com.socialaiassistant.keyboard

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity
import com.socialaiassistant.keyboard.theme.BackgroundFit
import com.socialaiassistant.keyboard.theme.BackgroundPhotoConfig
import com.socialaiassistant.keyboard.theme.BackgroundScope
import com.socialaiassistant.keyboard.theme.KeyboardTheme
import com.socialaiassistant.keyboard.theme.KeyboardThemeSurface
import com.socialaiassistant.keyboard.theme.ThemeBackgroundManager
import com.socialaiassistant.keyboard.theme.ThemeButtonRole
import com.socialaiassistant.keyboard.theme.ThemeCatalog
import com.socialaiassistant.keyboard.theme.ThemePack
import com.socialaiassistant.keyboard.theme.ThemePreset
import com.socialaiassistant.keyboard.theme.ThemePreviewView
import com.socialaiassistant.keyboard.theme.ThemeRenderer
import com.socialaiassistant.keyboard.theme.ThemeRepository
import com.socialaiassistant.keyboard.theme.ThemeResolutionPolicy
import com.socialaiassistant.keyboard.theme.ThemeSelectionState
import com.socialaiassistant.keyboard.theme.ThemeSettingsUiStyler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class ThemeSettingsActivity : AppCompatActivity() {
    private lateinit var repository: ThemeRepository
    private lateinit var backgroundManager: ThemeBackgroundManager
    private lateinit var renderer: ThemeRenderer
    private lateinit var uiStyler: ThemeSettingsUiStyler
    private lateinit var preview: ThemePreviewView
    private var currentTheme: KeyboardTheme = ThemePreset.socialAiNeon
    private var currentState: ThemeSelectionState? = null
    private var syncingUi = false
    private var resetDialog: AlertDialog? = null
    private var resetMutationStarted = false
    private val packButtons = linkedMapOf<ThemePack, Button>()
    private val surfaceButtons = linkedMapOf<KeyboardThemeSurface, Button>()

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(::importBackgroundPhoto)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_settings)
        repository = (application as? SocialAiApplication)?.themeRepository ?: ThemeRepository.create(this)
        backgroundManager = (application as? SocialAiApplication)?.themeBackgroundManager ?: ThemeBackgroundManager(this)
        renderer = (application as? SocialAiApplication)?.themeRenderer ?: ThemeRenderer(this)
        uiStyler = ThemeSettingsUiStyler(renderer)
        preview = findViewById(R.id.theme_preview)

        setupSpinners()
        setupPremiumPackCards()
        setupSurfaceCards()
        setupLegacyPresetButtons()
        setupEditors()
        setupSectionToggles()
        findViewById<Button>(R.id.button_reset_theme).setOnClickListener { showResetConfirmation() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.selectionState.collectLatest { state -> syncUi(state) }
            }
        }
    }

    private fun setupSpinners() {
        findViewById<Spinner>(R.id.background_scope_spinner).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Full Keyboard", "Keys Only", "AI Panel Only")
        )
        findViewById<Spinner>(R.id.background_fit_spinner).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Fill", "Fit", "Center Crop")
        )
    }

    private fun setupPremiumPackCards() {
        val container = findViewById<LinearLayout>(R.id.theme_pack_container)
        container.removeAllViews()
        ThemePack.entries.forEach { pack ->
            val theme = ThemeCatalog.surface(pack, KeyboardThemeSurface.ENGLISH)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(8), dp(8), dp(8))
                background = renderer.panelBackground(ThemeCatalog.globalChrome(pack), theme.primaryNeon)
            }
            val miniPreview = ThemePreviewView(this).apply {
                setPreview(theme, KeyboardThemeSurface.ENGLISH)
            }
            card.addView(
                miniPreview,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(110))
            )

            val boundaryToggle = CheckBox(this).apply {
                text = getString(R.string.theme_key_boundary_toggle)
                setTextColor(theme.textPrimary)
                isChecked = false
                lifecycleScope.launch {
                    isChecked = repository.currentKeyBoundaryEnabled(pack)
                }
                setOnCheckedChangeListener { _, enabled ->
                    lifecycleScope.launch { repository.setKeyBoundaryEnabled(pack, enabled) }
                }
            }
            card.addView(
                boundaryToggle,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                    topMargin = dp(4)
                }
            )

            val bubbleToggle = CheckBox(this).apply {
                text = getString(R.string.theme_bubble_effect_toggle)
                setTextColor(theme.textPrimary)
            }
            card.addView(
                bubbleToggle,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
            )

            val bubbleStyleHeading = TextView(this).apply {
                text = getString(R.string.theme_bubble_style_label)
                setTextColor(theme.textPrimary)
                textSize = 13f
                setPadding(dp(8), dp(2), dp(8), 0)
            }
            card.addView(
                bubbleStyleHeading,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            )

            val bubbleStatus = TextView(this).apply {
                text = getString(R.string.theme_bubble_style_value, BubbleKeyIntensity.NORMAL.label)
                setTextColor(theme.textSecondary)
                textSize = 13f
                setPadding(dp(8), 0, dp(8), dp(3))
            }
            card.addView(
                bubbleStatus,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            )

            val bubbleStyleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            BubbleKeyIntensity.entries.forEach { intensity ->
                val styleButton = Button(this).apply {
                    isAllCaps = false
                    text = intensity.label
                    setOnClickListener {
                        lifecycleScope.launch {
                            repository.setBubbleIntensity(pack, intensity)
                            bubbleStatus.text = getString(R.string.theme_bubble_style_value, intensity.label)
                        }
                    }
                }
                renderer.styleButton(styleButton, ThemeButtonRole.SECONDARY_ACTION, ThemeCatalog.globalChrome(pack))
                bubbleStyleRow.addView(
                    styleButton,
                    LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                        setMargins(dp(2), 0, dp(2), 0)
                    }
                )
            }
            card.addView(
                bubbleStyleRow,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            )

            val bubbleHint = TextView(this).apply {
                text = getString(R.string.theme_bubble_effect_hint)
                setTextColor(theme.textSecondary)
                textSize = 12f
                setPadding(dp(8), dp(3), dp(8), dp(3))
            }
            card.addView(
                bubbleHint,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            )

            lifecycleScope.launch {
                val appearance = repository.currentBubbleAppearance(pack)
                bubbleToggle.isChecked = appearance.enabled
                bubbleStatus.text = getString(R.string.theme_bubble_style_value, appearance.intensity.label)
                bubbleToggle.setOnCheckedChangeListener { _, enabled ->
                    lifecycleScope.launch { repository.setBubbleEnabled(pack, enabled) }
                }
            }

            val button = Button(this).apply {
                isAllCaps = false
                text = pack.displayName
                setOnClickListener {
                    lifecycleScope.launch {
                        repository.applyGlobalPack(pack)
                        toast("${pack.displayName} applied to the complete keyboard package.")
                    }
                }
            }
            renderer.styleButton(button, ThemeButtonRole.SECONDARY_ACTION, ThemeCatalog.globalChrome(pack))
            packButtons[pack] = button
            card.addView(
                button,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                    topMargin = dp(5)
                }
            )
            container.addView(
                card,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(4), 0, dp(6))
                }
            )
        }
    }

    private fun setupSurfaceCards() {
        val container = findViewById<LinearLayout>(R.id.theme_surface_container)
        container.removeAllViews()
        KeyboardThemeSurface.entries.forEach { surface ->
            val button = Button(this).apply {
                isAllCaps = false
                text = surface.displayName
                setOnClickListener { showSurfacePicker(surface) }
            }
            surfaceButtons[surface] = button
            container.addView(
                button,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply {
                    setMargins(0, dp(3), 0, dp(3))
                }
            )
        }
    }

    private fun setupLegacyPresetButtons() {
        val container = findViewById<LinearLayout>(R.id.theme_legacy_preset_container)
        container.removeAllViews()
        ThemePreset.builtIns.forEach { preset ->
            val button = Button(this).apply {
                text = preset.displayName
                isAllCaps = false
                setOnClickListener {
                    lifecycleScope.launch { repository.activatePreset(preset.id) }
                }
            }
            renderer.styleButton(button, ThemeButtonRole.SECONDARY_ACTION, preset)
            container.addView(
                button,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                    setMargins(0, dp(3), 0, dp(3))
                }
            )
        }
    }

    private fun setupSectionToggles() {
        findViewById<Button>(R.id.button_toggle_advanced_theme).setOnClickListener {
            toggleVisibility(findViewById(R.id.theme_advanced_content))
        }
        findViewById<Button>(R.id.button_toggle_legacy_themes).setOnClickListener {
            toggleVisibility(findViewById(R.id.theme_legacy_preset_container))
        }
    }

    private fun toggleVisibility(view: View) {
        view.visibility = if (view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun showSurfacePicker(surface: KeyboardThemeSurface) {
        val state = currentState ?: return
        preview.setPreview(ThemeResolutionPolicy.resolveSurface(state, surface), surface)
        val packs = ThemePack.entries.toTypedArray()
        val labels = buildList {
            add(getString(R.string.theme_use_global_pack))
            packs.forEach { pack -> add(ThemeCatalog.surface(pack, surface).displayName) }
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(surface.displayName)
            .setItems(labels) { _, which ->
                lifecycleScope.launch {
                    if (which == 0) repository.clearSurfaceOverride(surface)
                    else repository.setSurfaceOverride(surface, packs[which - 1])
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { restoreSettingsPreview() }
            .show()
    }

    private fun setupEditors() {
        findViewById<Button>(R.id.button_save_custom_theme).setOnClickListener { saveCustomFromControls() }
        findViewById<Button>(R.id.button_reset_custom_theme).setOnClickListener {
            lifecycleScope.launch {
                repository.resetCustom()
                toast("Custom controls reset.")
            }
        }
        findViewById<Button>(R.id.button_pick_background_photo).setOnClickListener {
            photoPicker.launch(arrayOf("image/*"))
        }
        findViewById<Button>(R.id.button_apply_background_settings).setOnClickListener { saveBackgroundControls() }
        findViewById<Button>(R.id.button_remove_background_photo).setOnClickListener { removeBackgroundPhoto() }

        listOf(
            R.id.theme_glow_seek,
            R.id.theme_glass_seek,
            R.id.theme_radius_seek,
            R.id.theme_gap_seek,
            R.id.theme_font_seek
        ).forEach { id ->
            findViewById<SeekBar>(id).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && !syncingUi) {
                        preview.setPreview(draftThemeFromControls(showErrors = false), KeyboardThemeSurface.SETTINGS)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (!syncingUi) saveCustomFromControls(silent = true)
                }
            })
        }
    }

    private fun showResetConfirmation() {
        resetDialog?.dismiss()
        resetDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.theme_reset_title))
            .setMessage(getString(R.string.theme_reset_message))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.theme_reset_continue) { _, _ -> showResetPackPicker() }
            .create()
            .also { it.show() }
    }

    private fun showResetPackPicker() {
        val packs = ThemePack.entries.toTypedArray()
        resetDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.theme_reset_choose_pack))
            .setItems(packs.map { it.displayName }.toTypedArray()) { _, which ->
                val selected = packs[which]
                resetMutationStarted = true
                lifecycleScope.launch {
                    val result = repository.resetToSelectedPack(selected)
                    result.backgroundFileToDelete?.let { backgroundManager.removePhoto(it) }
                    toast("${selected.displayName} applied.")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { it.show() }
    }

    internal fun resetMutationStartedForTest(): Boolean = resetMutationStarted

    internal fun showResetConfirmationForTest() = showResetConfirmation()

    internal fun cancelResetForTest() {
        resetDialog?.dismiss()
        resetDialog = null
    }

    private fun syncUi(state: ThemeSelectionState) {
        syncingUi = true
        currentState = state
        val globalTheme = ThemeResolutionPolicy.resolveGlobalChrome(state).normalized()
        val settingsTheme = ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.SETTINGS).normalized()
        currentTheme = globalTheme
        preview.setPreview(settingsTheme, KeyboardThemeSurface.SETTINGS)

        findViewById<TextView>(R.id.theme_active_label).text = if (state.globalPack != null) {
            "Global: ${state.globalPack.displayName} • Settings: ${settingsTheme.displayName}"
        } else {
            "Active legacy/custom: ${globalTheme.displayName}"
        }

        packButtons.forEach { (pack, button) ->
            val active = state.globalPack == pack
            button.text = if (active) "${pack.displayName}  ✓" else pack.displayName
            renderer.styleButton(button, ThemeButtonRole.SECONDARY_ACTION, ThemeCatalog.globalChrome(pack), selected = active)
        }
        surfaceButtons.forEach { (surface, button) ->
            val override = state.overrideFor(surface)
            val detail = when {
                override != null -> ThemeCatalog.surface(override, surface).displayName
                state.globalPack != null -> "Global • ${ThemeCatalog.surface(state.globalPack, surface).displayName}"
                else -> "Legacy / Custom"
            }
            button.text = "${surface.displayName}  •  $detail"
            renderer.styleButton(button, ThemeButtonRole.SECONDARY_ACTION, settingsTheme, selected = override != null)
        }

        applySettingsStyling(globalTheme, settingsTheme)
        syncAdvancedControls(globalTheme)
        syncingUi = false
    }

    private fun applySettingsStyling(globalTheme: KeyboardTheme, settingsTheme: KeyboardTheme) {
        val root = findViewById<View>(R.id.theme_settings_root)
        val sections = listOf<View>(
            findViewById(R.id.theme_pack_section),
            findViewById(R.id.theme_surface_section),
            findViewById(R.id.theme_advanced_section),
            findViewById(R.id.theme_legacy_section)
        )
        uiStyler.apply(root, globalTheme, settingsTheme, sections)
        listOf(
            R.id.button_toggle_advanced_theme,
            R.id.button_toggle_legacy_themes,
            R.id.button_save_custom_theme,
            R.id.button_reset_custom_theme,
            R.id.button_pick_background_photo,
            R.id.button_apply_background_settings,
            R.id.button_remove_background_photo,
            R.id.button_reset_theme
        ).forEach { id ->
            findViewById<Button>(id)?.let { button ->
                renderer.styleButton(
                    button,
                    if (id == R.id.button_reset_theme) ThemeButtonRole.ENTER else ThemeButtonRole.SECONDARY_ACTION,
                    settingsTheme
                )
            }
        }
    }

    private fun syncAdvancedControls(theme: KeyboardTheme) {
        setColorField(R.id.theme_root_color, theme.rootBackground)
        setColorField(R.id.theme_panel_surface_color, theme.panelSurface)
        setColorField(R.id.theme_primary_color, theme.primaryNeon)
        setColorField(R.id.theme_secondary_color, theme.secondaryNeon)
        setColorField(R.id.theme_ai_color, theme.aiNeon)
        setColorField(R.id.theme_action_color, theme.actionAccent)
        setColorField(R.id.theme_key_surface_color, theme.keySurface)
        setColorField(R.id.theme_key_label_color, theme.keyLabel)
        findViewById<SeekBar>(R.id.theme_glow_seek).progress = theme.glowStrength
        findViewById<SeekBar>(R.id.theme_glass_seek).progress = theme.glassOpacity
        findViewById<SeekBar>(R.id.theme_radius_seek).progress = (theme.keyCornerRadiusDp - 4f).toInt().coerceIn(0, 18)
        findViewById<SeekBar>(R.id.theme_gap_seek).progress = theme.keyGapDp.toInt().coerceIn(0, 12)
        findViewById<SeekBar>(R.id.theme_font_seek).progress = ((theme.keyLabelScale * 100f).toInt() - 80).coerceIn(0, 55)

        val bg = theme.background
        findViewById<TextView>(R.id.background_photo_status).text =
            if (bg.enabled && bg.localFileName.isNotBlank()) "Background: ${bg.localFileName}" else "No background photo selected"
        findViewById<Spinner>(R.id.background_scope_spinner).setSelection(scopePosition(bg.scope))
        findViewById<Spinner>(R.id.background_fit_spinner).setSelection(fitPosition(bg.fit))
        findViewById<SeekBar>(R.id.background_opacity_seek).progress = bg.opacityPercent
        findViewById<SeekBar>(R.id.background_dim_seek).progress = bg.darkOverlayPercent
        findViewById<SeekBar>(R.id.background_blur_seek).progress = bg.blurAmount
        updateContrastWarning(theme)
    }

    private fun restoreSettingsPreview() {
        currentState?.let { state ->
            preview.setPreview(
                ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.SETTINGS),
                KeyboardThemeSurface.SETTINGS
            )
        }
    }

    private fun saveCustomFromControls(silent: Boolean = false) {
        val draft = draftThemeFromControls(showErrors = !silent)
        preview.setPreview(draft, KeyboardThemeSurface.SETTINGS)
        updateContrastWarning(draft)
        lifecycleScope.launch {
            repository.saveCustom(draft)
            if (!silent) toast("Custom theme saved and applied live.")
        }
    }

    private fun draftThemeFromControls(showErrors: Boolean): KeyboardTheme {
        val base = currentTheme
        return base.copy(
            rootBackground = colorField(R.id.theme_root_color, base.rootBackground, showErrors),
            panelSurface = colorField(R.id.theme_panel_surface_color, base.panelSurface, showErrors),
            primaryNeon = colorField(R.id.theme_primary_color, base.primaryNeon, showErrors),
            secondaryNeon = colorField(R.id.theme_secondary_color, base.secondaryNeon, showErrors),
            aiNeon = colorField(R.id.theme_ai_color, base.aiNeon, showErrors),
            actionAccent = colorField(R.id.theme_action_color, base.actionAccent, showErrors),
            keySurface = colorField(R.id.theme_key_surface_color, base.keySurface, showErrors),
            keyLabel = colorField(R.id.theme_key_label_color, base.keyLabel, showErrors),
            glowStrength = findViewById<SeekBar>(R.id.theme_glow_seek).progress,
            glassOpacity = findViewById<SeekBar>(R.id.theme_glass_seek).progress.coerceAtLeast(25),
            keyCornerRadiusDp = 4f + findViewById<SeekBar>(R.id.theme_radius_seek).progress,
            keyGapDp = findViewById<SeekBar>(R.id.theme_gap_seek).progress.toFloat(),
            keyLabelScale = (80f + findViewById<SeekBar>(R.id.theme_font_seek).progress) / 100f
        ).asCustom().normalized()
    }

    private fun importBackgroundPhoto(uri: Uri) {
        val previous = currentTheme.background.localFileName.takeIf { it.isNotBlank() }
        lifecycleScope.launch {
            val result = backgroundManager.importPhoto(uri)
            val newFile = result.getOrElse {
                toast("Could not use this photo. Your current background was kept.")
                return@launch
            }
            try {
                val config = backgroundFromControls(newFile, enabled = true)
                repository.updateBackground(config)
                if (previous != null && previous != newFile) backgroundManager.removePhoto(previous)
                toast("Background photo applied.")
            } catch (error: CancellationException) {
                withContext(NonCancellable) { backgroundManager.removePhoto(newFile) }
                throw error
            } catch (error: Throwable) {
                backgroundManager.removePhoto(newFile)
                toast("Background could not be saved. Previous background kept.")
            }
        }
    }

    private fun saveBackgroundControls() {
        val fileName = currentTheme.background.localFileName
        if (fileName.isBlank() || !backgroundManager.exists(fileName)) {
            toast("Choose a background photo first.")
            return
        }
        lifecycleScope.launch {
            repository.updateBackground(backgroundFromControls(fileName, enabled = true))
            toast("Background settings applied live.")
        }
    }

    private fun removeBackgroundPhoto() {
        lifecycleScope.launch {
            val oldFile = repository.clearBackground()
            backgroundManager.removePhoto(oldFile)
            toast("Background photo removed.")
        }
    }

    private fun backgroundFromControls(fileName: String, enabled: Boolean): BackgroundPhotoConfig =
        BackgroundPhotoConfig(
            enabled = enabled,
            localFileName = fileName,
            scope = scopeAt(findViewById<Spinner>(R.id.background_scope_spinner).selectedItemPosition),
            fit = fitAt(findViewById<Spinner>(R.id.background_fit_spinner).selectedItemPosition),
            opacityPercent = findViewById<SeekBar>(R.id.background_opacity_seek).progress,
            darkOverlayPercent = findViewById<SeekBar>(R.id.background_dim_seek).progress,
            blurAmount = findViewById<SeekBar>(R.id.background_blur_seek).progress
        ).normalized()

    private fun setColorField(id: Int, color: Int) {
        val input = findViewById<EditText>(id)
        if (!input.hasFocus()) input.setText(String.format("#%08X", color))
    }

    private fun colorField(id: Int, fallback: Int, showErrors: Boolean): Int {
        val value = findViewById<EditText>(id).text?.toString().orEmpty().trim()
        return runCatching { Color.parseColor(value) }.getOrElse {
            if (showErrors) toast("Invalid color: $value — using previous value.")
            fallback
        }
    }

    private fun updateContrastWarning(theme: KeyboardTheme) {
        val difference = kotlin.math.abs(luma(theme.keySurface) - luma(theme.keyLabel))
        findViewById<TextView>(R.id.theme_contrast_warning).visibility =
            if (difference < 90) View.VISIBLE else View.GONE
    }

    private fun luma(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return ((r * 299 + g * 587 + b * 114) / 1000)
    }

    private fun scopeAt(position: Int): BackgroundScope = when (position) {
        1 -> BackgroundScope.KEYS_ONLY
        2 -> BackgroundScope.AI_PANEL_ONLY
        else -> BackgroundScope.FULL_KEYBOARD
    }

    private fun scopePosition(scope: BackgroundScope): Int = when (scope) {
        BackgroundScope.FULL_KEYBOARD -> 0
        BackgroundScope.KEYS_ONLY -> 1
        BackgroundScope.AI_PANEL_ONLY -> 2
    }

    private fun fitAt(position: Int): BackgroundFit = when (position) {
        0 -> BackgroundFit.FILL
        1 -> BackgroundFit.FIT
        else -> BackgroundFit.CENTER_CROP
    }

    private fun fitPosition(fit: BackgroundFit): Int = when (fit) {
        BackgroundFit.FILL -> 0
        BackgroundFit.FIT -> 1
        BackgroundFit.CENTER_CROP -> 2
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
