package ru.nanodev.antigravity.linux

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.nanodev.antigravity.BuildConfig
import java.util.concurrent.TimeUnit

/** Компонент среды из манифеста: адрес, SHA-256, нужно ли распаковывать, куда класть. */
data class RuntimeComponent(
    val id: String,
    val url: String,
    val sha256: String?,
    val archive: Boolean,
    val target: String
)

/**
 * Манифест среды. По умолчанию вшит в приложение: адреса и SHA-256 проверены вручную.
 * Переменная AG_RUNTIME_MANIFEST_URL (если задана при сборке) переопределяет вшитый манифест.
 */
object RuntimeManifest {

    private const val EMBEDDED = """{"components":[
  {"id":"proot","url":"https://skirsten.github.io/proot-portable-android-binaries/aarch64/proot","sha256":"c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38","archive":false,"target":"proot/proot"},
  {"id":"rootfs","url":"https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz","sha256":"a91d5a93010193712d346d761372b7c9db6dfcf093893161c64ca107f05914f2","archive":true,"target":"rootfs"}
]}"""

    val overrideUrl: String get() = BuildConfig.RUNTIME_MANIFEST_URL

    val isOverridden: Boolean get() = overrideUrl.isNotBlank()

    fun load(): List<RuntimeComponent> {
        val json = if (isOverridden) fetch(overrideUrl) else EMBEDDED
        val arr = JSONObject(json).getJSONArray("components")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RuntimeComponent(
                id = o.getString("id"),
                url = o.getString("url"),
                sha256 = o.optString("sha256", "").takeIf { it.isNotBlank() }?.lowercase(),
                archive = o.optBoolean("archive", false),
                target = o.getString("target")
            )
        }
    }

    private fun fetch(url: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) error("Манифест: HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
    }
}
