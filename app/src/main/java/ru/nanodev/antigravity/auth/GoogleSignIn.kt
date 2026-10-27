package ru.nanodev.antigravity.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import ru.nanodev.antigravity.BuildConfig

/** Итог входа через Google. */
sealed interface GoogleResult {
    data class Success(val email: String, val name: String, val photoUrl: String? = null) : GoogleResult
    data class Failed(val reason: String) : GoogleResult
    data object Cancelled : GoogleResult
    data object NotConfigured : GoogleResult
}

/**
 * Вход через Google с Credential Manager. Подтверждает аккаунт пользователя.
 * Доступа к Gemini это не даёт: запросы к модели идут по ключу API.
 */
object GoogleSignIn {

    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun signIn(context: Context): GoogleResult {
        if (!isConfigured) return GoogleResult.NotConfigured

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleResult.Success(
                    email = google.id,
                    name = google.displayName.orEmpty(),
                    photoUrl = google.profilePictureUri?.toString()
                )
            } else {
                GoogleResult.Failed("Неожиданный тип учётных данных")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleResult.Failed("Вход не выполнен: ${e.message ?: e.type}")
        } catch (e: Exception) {
            GoogleResult.Failed("Ошибка входа: ${e.message ?: "неизвестная"}")
        }
    }
}
