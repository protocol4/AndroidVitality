package io.protocol4.androidvitality.resource

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val modeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun shouldIncludeSensors(): Boolean {
        return prefs.getBoolean("include_sensors", false)
    }

    fun setIncludeSensors(include: Boolean) {
        prefs.edit().putBoolean("include_sensors", include).apply()
    }
}
