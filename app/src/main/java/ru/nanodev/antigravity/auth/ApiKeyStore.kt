package ru.nanodev.antigravity.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Зашифрованное хранилище: API-ключ Gemini и данные аккаунта Google.
 * Ничего из этого не попадает в settings.json (он лежит в общей папке Downloads).
 * Если Keystore недоступен, ключ держится в памяти и не сохраняется на диск.
 */
class ApiKeyStore(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        EncryptedSharedPreferences.create(
            FILE_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    private var memoryKey: String = ""

    var apiKey: String
        get() = prefs?.getString(KEY_API, null) ?: memoryKey
        set(value) {
            val clean = value.trim()
            if (prefs != null) prefs.edit().putString(KEY_API, clean).apply()
            else memoryKey = clean
        }

    val hasKey: Boolean get() = apiKey.isNotBlank()

    /** Выход из ключа. Аккаунт Google сохраняется. */
    fun clear() {
        prefs?.edit()?.remove(KEY_API)?.apply()
        memoryKey = ""
    }

    val googleEmail: String? get() = prefs?.getString(KEY_G_EMAIL, null)
    val googleName: String? get() = prefs?.getString(KEY_G_NAME, null)

    fun saveGoogleAccount(email: String, name: String) {
        prefs?.edit()?.putString(KEY_G_EMAIL, email)?.putString(KEY_G_NAME, name)?.apply()
    }

    fun forgetGoogleAccount() {
        prefs?.edit()?.remove(KEY_G_EMAIL)?.remove(KEY_G_NAME)?.apply()
    }

    private companion object {
        const val FILE_NAME = "antigravity_secure"
        const val KEY_API = "gemini_api_key"
        const val KEY_G_EMAIL = "google_email"
        const val KEY_G_NAME = "google_name"
    }
}
