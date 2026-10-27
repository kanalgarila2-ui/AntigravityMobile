package ru.nanodev.antigravity.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Хранилище API-ключа. Ключ лежит только в зашифрованных настройках приложения,
 * никогда не попадает в settings.json (он лежит в общей папке Downloads).
 * Если Keystore недоступен, ключ держится в памяти и не сохраняется на диск.
 */
class ApiKeyStore(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
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

    fun clear() {
        prefs?.edit()?.remove(KEY_API)?.apply()
        memoryKey = ""
    }

    private companion object {
        const val FILE_NAME = "antigravity_secure"
        const val KEY_API = "gemini_api_key"
    }
}
