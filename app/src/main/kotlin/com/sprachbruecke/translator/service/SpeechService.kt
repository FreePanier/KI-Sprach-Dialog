package com.sprachbruecke.translator.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Ergebnis-Typen der Spracherkennung
 */
sealed class SpeechResult {
    /** Zwischen-Ergebnis während der Erkennung */
    data class Partial(val text: String) : SpeechResult()
    /** Finales Ergebnis nach dem Sprechen */
    data class Final(val text: String) : SpeechResult()
    /** Fehler mit Android-Fehlercode */
    data class Error(val code: Int, val message: String) : SpeechResult()
    /** Mikrofon ist bereit */
    object Ready : SpeechResult()
}

/**
 * Wrapper um Android SpeechRecognizer.
 * Liefert Erkennungsergebnisse als Flow.
 *
 * WICHTIG: Offline-Sprachpakete müssen unter Android-Einstellungen
 * > Allgemeine Verwaltung > Sprachpakete heruntergeladen sein.
 */
class SpeechService(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    /**
     * Startet die Spracherkennung für die angegebene Locale.
     * @param localeCode BCP-47 Code, z.B. "de-DE"
     * @param preferOffline Ob Offline-Erkennung erzwungen werden soll
     * @return Flow mit SpeechResult-Ereignissen
     */
    fun recognize(localeCode: String, preferOffline: Boolean = true): Flow<SpeechResult> = callbackFlow {
        android.util.Log.d("SpeechService", "Starte Erkennung für Locale: $localeCode (Offline-Vorzug: $preferOffline)")
        
        recognizer?.destroy()
        recognizer = null
        
        // Kurze Pause, damit das System den alten Dienst sicher freigibt
        kotlinx.coroutines.delay(100)

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechResult.Ready)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return
                if (text.isNotBlank()) {
                    trySend(SpeechResult.Partial(text))
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                trySend(SpeechResult.Final(text))
                close() // Flow beenden nach finalem Ergebnis
            }

            override fun onError(error: Int) {
                android.util.Log.e("SpeechService", "Recognizer Fehler-Code: $error")
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Keine Übereinstimmung"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Zeitüberschreitung"
                    SpeechRecognizer.ERROR_NETWORK -> "Netzwerkfehler"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Netzwerk-Timeout"
                    SpeechRecognizer.ERROR_AUDIO -> "Audiofehler"
                    SpeechRecognizer.ERROR_SERVER -> "Serverfehler"
                    SpeechRecognizer.ERROR_CLIENT -> "Client-Fehler"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Keine Mikrofon-Erlaubnis"
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Sprache nicht unterstützt – Sprachpaket herunterladen?"
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Sprachpaket nicht verfügbar – bitte herunterladen"
                    11 -> "Dienst-Initialisierungsfehler (11) – Versuche Neustart..."
                    else -> "Unbekannter Fehler ($error)"
                }
                trySend(SpeechResult.Error(error, msg))
                close()
            }

            // Pflicht-Overrides (ungenutzt)
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            
            // Verhindert das Ausweichen auf Englisch, wenn möglich
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
        }

        speechRecognizer.startListening(intent)

        awaitClose {
            recognizer?.destroy()
            recognizer = null
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    /**
     * Versucht den Download eines Sprachpakets anzustoßen.
     * Öffnet den Systemdialog zur Verwaltung der Offlinesprachen.
     */
    fun triggerDownload(localeCode: String) {
        val intents = listOf<Intent>(
            // 1. Spezifischer Google Offline-Sprachen Intent
            Intent("com.google.android.voicesearch.OFFLINE_SETTINGS").apply {
                setPackage("com.google.android.googlequicksearchbox")
            },
            // 2. Standard Android Install Intent
            Intent("android.speech.action.INSTALL_LANGUAGE_PACK").apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeCode)
            },
            // 3. Google Voice Settings Fallback
            Intent("com.google.android.voicesearch.LANGUAGE_SETTINGS"),
            // 4. System Locale Settings als letzter Ausweg
            Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d("SpeechService", "Erfolgreich gestartet: ${intent.action}")
                return
            } catch (e: Exception) {
                Log.w("SpeechService", "Fehlgeschlagen: ${intent.action ?: "unknown"}")
            }
        }
    }

    /**
     * Prüft ob Spracherkennung auf diesem Gerät verfügbar ist.
     */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)
}
