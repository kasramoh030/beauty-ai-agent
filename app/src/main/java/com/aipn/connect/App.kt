package com.aipn.connect

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.aipn.connect.data.KeyVault

class App : Application() {

    /** Encrypted store for the API keys the user owns. */
    val keyVault: KeyVault by lazy { KeyVault(this) }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            when (PreferenceStore(this).theme) {
                ThemeChoice.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeChoice.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeChoice.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                // Fixed palettes are applied by ApnTheme, not by the platform.
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}

/** SYSTEM follows the device; the rest are fixed palettes. */
enum class ThemeChoice { SYSTEM, LIGHT, DARK, MIDNIGHT, OLED, SAND, OCEAN }

/** Small, non-secret preferences: theme, language and generation settings. */
class PreferenceStore(private val app: Application) {

    private val prefs = app.getSharedPreferences("apn_prefs", Application.MODE_PRIVATE)

    var theme: ThemeChoice
        get() = runCatching {
            ThemeChoice.valueOf(prefs.getString("theme", "SYSTEM") ?: "SYSTEM")
        }.getOrDefault(ThemeChoice.SYSTEM)
        set(value) = prefs.edit().putString("theme", value.name).apply()

    /** Empty string means "system". */
    var languageTag: String
        get() = prefs.getString("language", "") ?: ""
        set(value) = prefs.edit().putString("language", value).apply()

    var systemPrompt: String
        get() = prefs.getString("system_prompt", "") ?: ""
        set(value) = prefs.edit().putString("system_prompt", value).apply()

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.7f)
        set(value) = prefs.edit().putFloat("temperature", value).apply()

    var maxTokens: Int
        get() = prefs.getInt("max_tokens", 2048)
        set(value) = prefs.edit().putInt("max_tokens", value).apply()

    var streaming: Boolean
        get() = prefs.getBoolean("streaming", true)
        set(value) = prefs.edit().putBoolean("streaming", value).apply()

    var autoFallback: Boolean
        get() = prefs.getBoolean("auto_fallback", true)
        set(value) = prefs.edit().putBoolean("auto_fallback", value).apply()
}