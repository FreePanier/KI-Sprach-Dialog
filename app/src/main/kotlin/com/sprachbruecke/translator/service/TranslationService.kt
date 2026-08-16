package com.sprachbruecke.translator.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Aktueller Übersetzungsmodus – wird in der UI-Trennlinie angezeigt.
 */
enum class TranslationMode {
    /** Gemini Flash via Cloud API */
    CLOUD,
    /** Gemini Nano lokal auf dem Gerät */
    ON_DEVICE,
    /** Kein Netz und kein On-Device-Modell – Fehler */
    ERROR
}

/**
 * Ergebnis eines Übersetzungsaufrufs
 */
sealed class TranslationResult {
    data class Success(val text: String, val mode: TranslationMode) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
}

/**
 * Übersetzt Text mithilfe der Gemini API.
 * Primär: Gemini 2.0 Flash (Cloud) – schnellstes Modell
 * Fallback: Gemini Nano (On-Device, falls auf Pixel 8+ verfügbar)
 */
class TranslationService(
    private val context: Context,
    private val apiKeyProvider: ApiKeyProvider
) {
    private var cloudModel: GenerativeModel? = null

    /**
     * Initialisiert das Cloud-Modell mit dem API-Key.
     */
    private suspend fun getOrCreateCloudModel(): GenerativeModel {
        return cloudModel ?: run {
            val apiKey = apiKeyProvider.getApiKey()
            val modelName = com.sprachbruecke.translator.BuildConfig.GEMINI_MODEL
            Log.d("TranslationService", "Initialisiere Modell aus Konfiguration: $modelName")
            GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.1f
                    maxOutputTokens = 1024
                }
            ).also { cloudModel = it }
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        if (network == null) {
            Log.w("TranslationService", "Kein aktives Netzwerk gefunden")
            return false
        }
        val caps = cm.getNetworkCapabilities(network)
        if (caps == null) {
            Log.w("TranslationService", "Keine Netzwerk-Capabilities")
            return false
        }
        
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        // Wir lockern den Check: Wenn Internet da ist, versuchen wir es, auch wenn "Validated" noch fehlt
        // Das hilft bei instabilen WiFi-Verbindungen oder während des Debuggings.
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        Log.d("TranslationService", "Netzwerk-Check: Internet=$hasInternet, Validiert=$isValidated")
        
        return hasInternet
    }

    /**
     * Übersetzt Text und gibt das Ergebnis als Flow zurück.
     * Streaming: erste Tokens erscheinen sofort.
     *
     * @param text Der zu übersetzende Text
     * @param fromLanguage Quellsprache (Anzeigename, z.B. "Deutsch")
     * @param toLanguage Zielsprache (Anzeigename, z.B. "Italiano")
     * @return Flow<TranslationResult> – mehrere Partial-Ergebnisse, dann Final
     */
    fun translate(
        text: String,
        fromLanguage: String,
        toLanguage: String
    ): Flow<TranslationResult> = flow {
        if (text.isBlank()) return@flow

        if (!isOnline()) {
            emit(TranslationResult.Error("Keine Internetverbindung für Übersetzung"))
            return@flow
        }

        try {
            Log.d("TranslationService", "Starte Übersetzung von $fromLanguage nach $toLanguage")
            Log.d("TranslationService", "Text: '$text'")
            val model = getOrCreateCloudModel()
            val prompt = buildPrompt(text, fromLanguage, toLanguage)

            val responseFlow = model.generateContentStream(prompt)
            val accumulated = StringBuilder()

            responseFlow.collect { chunk ->
                Log.d("TranslationService", "Empfange Chunk: ${chunk.text?.take(20)}...")
                val chunkText = chunk.text ?: return@collect
                accumulated.append(chunkText)
                // Partial-Ergebnis streamen (sofortige Anzeige)
                emit(TranslationResult.Success(accumulated.toString(), TranslationMode.CLOUD))
            }

        } catch (e: Exception) {
            Log.e("TranslationService", "Fehler bei der Übersetzung", e)
            val errorMsg = when {
                e.message?.contains("API key") == true ->
                    "Ungültiger API-Key. Bitte in local.properties prüfen."
                e.message?.contains("404") == true ->
                    "KI-Modell nicht gefunden (404). Bitte API-Key oder Modell-Verfügbarkeit prüfen.\nDetails: ${e.localizedMessage}"
                e.message?.contains("quota") == true ->
                    "API-Kontingent erschöpft."
                else -> "Übersetzungsfehler: ${e.localizedMessage ?: e.message}"
            }
            emit(TranslationResult.Error(errorMsg))
        }
    }

    /**
     * Baut einen präzisen Übersetzungs-Prompt für Gemini.
     * Optimiert für Konversationstext zwischen Menschen.
     */
    private fun buildPrompt(text: String, from: String, to: String): String = """
        Du bist ein präziser Übersetzer. Übersetze den folgenden Text von $from nach $to.
        
        Regeln:
        - Übersetze NUR den Text, keine Erklärungen
        - Behalte Ton und Stil (förmlich/informell) bei
        - Bei kurzen Sätzen: natürliche Umgangssprache bevorzugen
        - Gib NUR die Übersetzung aus, kein "Übersetzung:" davor
        
        Text: $text
    """.trimIndent()

    /**
     * Setzt das Cloud-Modell zurück (z.B. nach API-Key-Änderung).
     */
    fun resetModel() {
        cloudModel = null
    }
}
