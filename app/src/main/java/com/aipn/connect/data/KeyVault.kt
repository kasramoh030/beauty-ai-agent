package com.aipn.connect.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's own API keys encrypted with an AES-GCM key that lives inside the
 * Android hardware-backed keystore and never leaves it. The ciphertext sits in
 * ordinary SharedPreferences, so the raw key text is never written to disk.
 *
 * Notes:
 *  - Keys belong to the user, are entered by the user, and are only ever sent to the
 *    provider they belong to.
 *  - If the keystore entry is lost (e.g. after a factory reset) the encrypted values
 *    simply become unreadable and the user re-enters the keys.
 */
class KeyVault(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS = "apn_secure_prefs"
        private const val KEY_ALIAS = "apn_key_vault_aes"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128

        /** Key/value pair per provider id. */
        private const val K_API = "api_"
        private const val K_ENABLED = "enabled_"
        private const val K_MODEL = "model_"
        private const val K_CUSTOM = "custom_models_"
        private const val K_ACCOUNT = "account_"
        private const val K_STATUS = "status_"
        private const val K_MODEL_COUNT = "model_count_"
        private const val K_REQUESTS = "requests_"
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? = try {
        val payload = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, payload, 0, IV_LENGTH)
        )
        String(cipher.doFinal(payload, IV_LENGTH, payload.size - IV_LENGTH), Charsets.UTF_8)
    } catch (t: Throwable) {
        null
    }

    // ---- API keys -----------------------------------------------------------

    fun apiKey(providerId: String): String? {
        val stored = prefs.getString(K_API + providerId, null) ?: return null
        return decrypt(stored)
    }

    fun setApiKey(providerId: String, value: String) {
        prefs.edit().putString(K_API + providerId, encrypt(value)).apply()
    }

    fun clearApiKey(providerId: String) {
        prefs.edit().remove(K_API + providerId).apply()
    }

    /** The last four characters, for showing which key is stored without revealing it. */
    fun keyTail(providerId: String): String? {
        val key = apiKey(providerId) ?: return null
        return if (key.length <= 4) "••••" else "••••" + key.takeLast(4)
    }

    fun hasKey(providerId: String): Boolean = !apiKey(providerId).isNullOrBlank()

    /** True when the provider can be used right now: either it needs no key, or a key is set. */
    fun isUsable(provider: Provider): Boolean =
        provider.keyOptional || hasKey(provider.id)

    // ---- account id (Cloudflare and friends) ---------------------------------

    fun accountId(providerId: String): String = prefs.getString(K_ACCOUNT + providerId, "") ?: ""

    fun setAccountId(providerId: String, value: String) {
        prefs.edit().putString(K_ACCOUNT + providerId, value.trim()).apply()
    }

    // ---- enable / disable ---------------------------------------------------

    fun isEnabled(providerId: String): Boolean =
        prefs.getBoolean(K_ENABLED + providerId, true)

    fun setEnabled(providerId: String, enabled: Boolean) {
        prefs.edit().putBoolean(K_ENABLED + providerId, enabled).apply()
    }

    fun enabledIds(): Set<String> = Providers.ALL
        .filter { isEnabled(it.id) && isUsable(it) }
        .map { it.id }
        .toSet()

    // ---- per-provider model choice ------------------------------------------

    /** Empty string means "use the provider default". */
    fun modelFor(providerId: String): String = prefs.getString(K_MODEL + providerId, "") ?: ""

    fun setModelFor(providerId: String, model: String) {
        prefs.edit().putString(K_MODEL + providerId, model).apply()
    }

    fun resolvedModel(provider: Provider): String =
        modelFor(provider.id).ifBlank { provider.defaultModel }

    // ---- last known test status ---------------------------------------------

    /** Model ids the user typed by hand for a provider, stored comma separated. */
    fun setCustomModels(providerId: String, value: String) {
        val normalised = value.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(",")
        prefs.edit().putString(K_CUSTOM + providerId, normalised).apply()
    }

    /** All custom-model entries, for a quick lookup at startup. */
    fun customModels(): Map<String, String> {
        val result = HashMap<String, String>()
        for (provider in Providers.ALL) {
            val value = prefs.getString(K_CUSTOM + provider.id, "") ?: continue
            if (value.isNotBlank()) result[provider.id] = value
        }
        return result
    }

    fun status(providerId: String): String = prefs.getString(K_STATUS + providerId, "") ?: ""

    fun modelCount(providerId: String): Int = prefs.getInt(K_MODEL_COUNT + providerId, 0)

    fun setStatus(providerId: String, status: String, models: Int = 0) {
        prefs.edit()
            .putString(K_STATUS + providerId, status)
            .putInt(K_MODEL_COUNT + providerId, models)
            .apply()
    }

    fun requestCount(providerId: String): Int = prefs.getInt(K_REQUESTS + providerId, 0)

    fun bumpRequests(providerId: String) {
        prefs.edit().putInt(K_REQUESTS + providerId, requestCount(providerId) + 1).apply()
    }

    fun totalRequests(): Int = Providers.ALL.sumOf { requestCount(it.id) }

    fun resetStats() {
        val editor = prefs.edit()
        for (provider in Providers.ALL) {
            editor.putInt(K_REQUESTS + provider.id, 0)
            editor.remove(K_STATUS + provider.id)
            editor.remove(K_MODEL_COUNT + provider.id)
        }
        editor.apply()
    }

    /** Wipes every stored secret but keeps the user's choices. */
    fun clearAllKeys() {
        val editor = prefs.edit()
        for (provider in Providers.ALL) editor.remove(K_API + provider.id)
        editor.apply()
    }
}