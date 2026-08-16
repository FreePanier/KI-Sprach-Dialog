package com.sprachbruecke.translator.service

import com.sprachbruecke.translator.BuildConfig

/**
 * Interface für API-Key-Beschaffung.
 * Entwicklung: BuildConfig (local.properties)
 * Produktion (geplant): Server-Authentifizierung
 */
interface ApiKeyProvider {
    suspend fun getApiKey(): String
}

/**
 * Entwicklungs-Implementierung: Key aus local.properties über BuildConfig.
 * In Produktion durch ServerApiKeyProvider ersetzen.
 */
class LocalApiKeyProvider : ApiKeyProvider {
    override suspend fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) {
            throw IllegalStateException(
                "GEMINI_API_KEY nicht gesetzt! " +
                "Bitte GEMINI_API_KEY in local.properties eintragen."
            )
        }
        return key
    }
}

// Zukunft: ServerApiKeyProvider
// class ServerApiKeyProvider(
//     private val authService: AuthService,
//     private val serverUrl: String
// ) : ApiKeyProvider {
//     override suspend fun getApiKey(): String {
//         val token = authService.getAuthToken()
//         return httpClient.get("$serverUrl/api/key") {
//             bearerAuth(token)
//         }.body()
//     }
// }
