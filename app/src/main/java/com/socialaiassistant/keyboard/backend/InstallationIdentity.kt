package com.socialaiassistant.keyboard.backend

import android.content.Context
import java.util.UUID

class InstallationIdentity(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("social_ai_installation", Context.MODE_PRIVATE)

    fun getOrCreate(): String {
        val existing = prefs.getString(KEY, null)?.trim().orEmpty()
        if (existing.isNotEmpty()) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, created).apply()
        return created
    }

    companion object {
        private const val KEY = "installation_id_v1"
    }
}
