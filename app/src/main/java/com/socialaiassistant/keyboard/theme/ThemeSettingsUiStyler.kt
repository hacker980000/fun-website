package com.socialaiassistant.keyboard.theme

import android.view.View
import android.widget.TextView
import com.socialaiassistant.keyboard.R

class ThemeSettingsUiStyler(private val renderer: ThemeRenderer) {
    fun apply(
        root: View,
        globalChromeTheme: KeyboardTheme,
        settingsTheme: KeyboardTheme,
        sectionContainers: List<View>
    ) {
        root.setBackgroundColor(settingsTheme.rootBackground)
        sectionContainers.forEach { container ->
            container.background = renderer.panelBackground(settingsTheme, settingsTheme.primaryNeon)
        }
        renderer.styleTree(root, settingsTheme)
        root.findViewById<TextView>(R.id.theme_settings_title)?.let { title ->
            renderer.styleText(title, globalChromeTheme)
        }
    }
}
