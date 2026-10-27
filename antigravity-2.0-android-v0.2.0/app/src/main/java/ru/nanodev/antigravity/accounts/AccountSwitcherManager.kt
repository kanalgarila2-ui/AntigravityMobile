package ru.nanodev.antigravity.accounts

import ru.nanodev.antigravity.sdk.AntigravitySettings
import ru.nanodev.antigravity.sdk.GoogleAccountProfile
import ru.nanodev.antigravity.storage.DownloadsProjectManager

/**
 * Полноценный менеджер и быстрый переключатель аккаунтов (`ru.nanodev.antigravity.accounts`).
 * Поддерживает:
 * - Несколько аккаунтов Google Antigravity 2.0 (OAuth2 / CloudCode токены)
 * - Индивидуальные API-ключи Gemini и профили Vertex AI Enterprise
 * - Мгновенное переключение в 1 клик из верхней панели, сайдбара, настроек или виджета рабочего стола
 * - Автоматическую ротацию аккаунта при исчерпании лимита запросов (Auto-Rotate on Quota Exhausted).
 */
class AccountSwitcherManager(private val projectManager: DownloadsProjectManager) {

    fun switchActiveAccount(current: AntigravitySettings, targetEmail: String): AntigravitySettings {
        val updatedList = current.accounts.map { acc ->
            acc.copy(isActive = acc.email.equals(targetEmail, ignoreCase = true))
        }
        val nextSettings = current.copy(accounts = updatedList)
        projectManager.saveGlobalSettings(nextSettings)
        return nextSettings
    }

    fun cycleToNextAccount(current: AntigravitySettings): AntigravitySettings {
        if (current.accounts.isEmpty()) return current
        val activeIdx = current.accounts.indexOfFirst { it.isActive }.coerceAtLeast(0)
        val nextIdx = (activeIdx + 1) % current.accounts.size
        return switchActiveAccount(current, current.accounts[nextIdx].email)
    }

    fun addAccount(
        current: AntigravitySettings,
        email: String,
        displayName: String,
        tier: String,
        oauthToken: String = "",
        apiKey: String = ""
    ): AntigravitySettings {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) return current
        val deactivated = current.accounts.map { it.copy(isActive = false) }
        val newProfile = GoogleAccountProfile(
            email = cleanEmail,
            displayName = displayName.ifBlank { cleanEmail.substringBefore("@") },
            oauthAccessToken = oauthToken,
            customApiKey = apiKey,
            tier = tier,
            aiCreditsRemaining = 500,
            isActive = true
        )
        val nextSettings = current.copy(accounts = deactivated + newProfile)
        projectManager.saveGlobalSettings(nextSettings)
        return nextSettings
    }

    fun removeAccount(current: AntigravitySettings, email: String): AntigravitySettings {
        val remaining = current.accounts.filterNot { it.email.equals(email, ignoreCase = true) }
        val fixed = if (remaining.isNotEmpty() && remaining.none { it.isActive }) {
            remaining.mapIndexed { idx, acc -> acc.copy(isActive = idx == 0) }
        } else {
            remaining
        }
        val nextSettings = current.copy(accounts = fixed)
        projectManager.saveGlobalSettings(nextSettings)
        return nextSettings
    }
}
