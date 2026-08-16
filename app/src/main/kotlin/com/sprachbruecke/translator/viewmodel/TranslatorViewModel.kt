package com.sprachbruecke.translator.viewmodel

import android.app.Application
import android.speech.SpeechRecognizer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sprachbruecke.translator.R
import com.sprachbruecke.translator.data.*
import com.sprachbruecke.translator.service.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

// DataStore für App-Einstellungen
val android.content.Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val KEY_TOP_LOCALE = stringPreferencesKey("top_locale")
private val KEY_BOTTOM_LOCALE = stringPreferencesKey("bottom_locale")
private val KEY_TOP_ROTATION = intPreferencesKey("top_rotation")
private val KEY_BOTTOM_ROTATION = intPreferencesKey("bottom_rotation")
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
private val KEY_TOP_FONT_SIZE = floatPreferencesKey("top_font_size")
private val KEY_BOTTOM_FONT_SIZE = floatPreferencesKey("bottom_font_size")
private val KEY_TOP_FONT_COLOR = longPreferencesKey("top_font_color")
private val KEY_BOTTOM_FONT_COLOR = longPreferencesKey("bottom_font_color")
private val KEY_APP_LOCALE = stringPreferencesKey("app_locale")

/**
 * Welches Textfeld ist gerade aktiv (Mikrofon hört zu)
 */
enum class ActiveField { TOP, BOTTOM, NONE }

/**
 * UI-Zustand des Übersetzers
 */
data class TranslatorUiState(
    val blocks: List<ConversationBlock> = emptyList(),
    val activeField: ActiveField = ActiveField.NONE,
    val topLanguage: LanguageOption = DEFAULT_TOP_LANGUAGE,
    val bottomLanguage: LanguageOption = DEFAULT_BOTTOM_LANGUAGE,
    val partialTopText: String = "",      // Zwischen-Ergebnis ASR oben
    val partialBottomText: String = "",   // Zwischen-Ergebnis ASR unten
    val partialTranslation: String = "",  // Zwischen-Ergebnis Übersetzung
    val translationMode: TranslationMode = TranslationMode.CLOUD,
    val errorMessage: String? = null,
    val errorLocale: String? = null,      // Welche Sprache hat den Fehler verursacht
    val isSpeaking: Boolean = false,      // TTS läuft
    val ttsBlockId: Long? = null,         // Welcher Block wird vorgelesen
    val keepScreenOn: Boolean = true,     // Steuerung des Always-On
    val kiTest: Boolean = true,           // Variable ki_test auf true gesetzt
    
    // Neue Layout-Einstellungen
    val topRotation: Int = 0,             // 0 oder 180
    val bottomRotation: Int = 0,          // 0 oder 180
    val themeMode: String = "System",     // "System", "Dark", "Light"
    val topFontSize: Float = 22f,
    val bottomFontSize: Float = 22f,
    val topFontColor: Long = 0xFF000000,  // Standard Schwarz (wird im UI-State angepasst)
    val bottomFontColor: Long = 0xFF000000,
    val appLocale: String = "de",
    val latestVersion: String? = null,    // Gelesene Version von der Webseite
)

/**
 * Haupt-ViewModel: Koordiniert Spracherkennung, Übersetzung und UI-Zustand.
 */
class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(context)
    private val dao = db.conversationDao()
    private val apiKeyProvider = LocalApiKeyProvider()
    private val speechService = SpeechService(context)
    private val translationService = TranslationService(context, apiKeyProvider)
    private val tts = android.speech.tts.TextToSpeech(context, null)

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    /** Aktuelle Sitzungs-ID */
    private var currentSessionId = System.currentTimeMillis()

    private var speechJob: Job? = null
    private var translationJob: Job? = null
    private var inactivityJob: Job? = null

    init {
        // Gespeicherte Spracheinstellungen laden
        viewModelScope.launch {
            context.dataStore.data.first().let { prefs ->
                _uiState.update { state ->
                    state.copy(
                        topLanguage = SUPPORTED_LANGUAGES.find { it.localeCode == prefs[KEY_TOP_LOCALE] }
                            ?: DEFAULT_TOP_LANGUAGE,
                        bottomLanguage = SUPPORTED_LANGUAGES.find { it.localeCode == prefs[KEY_BOTTOM_LOCALE] }
                            ?: DEFAULT_BOTTOM_LANGUAGE,
                        topRotation = prefs[KEY_TOP_ROTATION] ?: 0,
                        bottomRotation = prefs[KEY_BOTTOM_ROTATION] ?: 0,
                        themeMode = prefs[KEY_THEME_MODE] ?: "System",
                        topFontSize = prefs[KEY_TOP_FONT_SIZE] ?: 22f,
                        bottomFontSize = prefs[KEY_BOTTOM_FONT_SIZE] ?: 22f,
                        topFontColor = prefs[KEY_TOP_FONT_COLOR] ?: 0xFF000000,
                        bottomFontColor = prefs[KEY_BOTTOM_FONT_COLOR] ?: 0xFF000000,
                        appLocale = prefs[KEY_APP_LOCALE] ?: Locale.getDefault().language
                    )
                }
            }
        }

        // Netzwerkstatus initial prüfen
        checkTranslationMode()
        
        // Inaktivitäts-Timer starten
        resetInactivityTimer()

        // KI-Test Begrüßung
        if (_uiState.value.kiTest) {
            triggerGreetingTest()
        }
    }

    /**
     * Führt eine Test-Übersetzung des Begrüßungstextes durch.
     */
    private fun triggerGreetingTest() {
        // Sicherstellen, dass wir die aktuelle App-Sprache für den Begrüßungstext nutzen
        val greeting = context.getString(R.string.greeting_text)
        
        viewModelScope.launch {
            _uiState.update { it.copy(partialTopText = greeting) }
            android.util.Log.d("TranslatorViewModel", "KI-Test gestartet mit: $greeting")
            processSpokenText(greeting, ActiveField.TOP)
            _uiState.update { it.copy(partialTopText = "") }
        }
    }

    /**
     * Startet oder setzt den 5-Minuten Timer für das Display zurück.
     */
    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        _uiState.update { it.copy(keepScreenOn = true) }
        
        inactivityJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000L) // 5 Minuten
            android.util.Log.d("TranslatorViewModel", "5 Minuten Inaktivität erreicht - Display Always-On aus")
            _uiState.update { it.copy(keepScreenOn = false) }
        }
    }

    // ─────────────────────────────────────────────
    // Spracherkennung
    // ─────────────────────────────────────────────

    /**
     * Textfeld angetippt – Mikrofon für die entsprechende Sprache aktivieren.
     */
    fun onFieldTapped(field: ActiveField) {
        resetInactivityTimer()
        val currentActive = _uiState.value.activeField

        // Zweites Tippen stoppt die Erkennung
        if (currentActive == field) {
            stopListening()
            return
        }

        // Feld wechseln: alte Erkennung stoppen
        if (currentActive != ActiveField.NONE) {
            stopListening()
        }

        _uiState.update { it.copy(activeField = field, errorMessage = null) }
        startListening(field)
    }

    private fun startListening(field: ActiveField) {
        val localeCode = when (field) {
            ActiveField.TOP -> _uiState.value.topLanguage.localeCode
            ActiveField.BOTTOM -> _uiState.value.bottomLanguage.localeCode
            ActiveField.NONE -> return
        }

        speechJob = viewModelScope.launch {
            speechService.recognize(localeCode, preferOffline = true).collect { result ->
                when (result) {
                    is SpeechResult.Ready -> { /* Mikrofon bereit */ }

                    is SpeechResult.Partial -> {
                        if (field == ActiveField.TOP) {
                            _uiState.update { it.copy(partialTopText = result.text) }
                        } else {
                            _uiState.update { it.copy(partialBottomText = result.text) }
                        }
                    }

                    is SpeechResult.Final -> {
                        _uiState.update { it.copy(partialTopText = "", partialBottomText = "", activeField = ActiveField.NONE) }
                        if (result.text.isNotBlank()) {
                            processSpokenText(result.text, field)
                        }
                    }

                    is SpeechResult.Error -> {
                        // AUTOMATISCHER FALLBACK:
                        // Wenn Fehler 13 (Sprache nicht offline verfügbar) oder Fehler 11 (Dienst hakt)
                        // und wir online sind, versuchen wir es sofort nochmal OHNE Offline-Zwang.
                        if ((result.code == 13 || result.code == 11) && translationService.isOnline()) {
                            android.util.Log.w("TranslatorViewModel", "Sprachdienst hakt (Fehler ${result.code}), versuche Online-Fallback...")
                            startListeningOnline(field, localeCode)
                            return@collect
                        }

                        // Dialog nur zeigen, wenn wir wirklich offline sind oder ein anderer Fehler vorliegt
                        _uiState.update { it.copy(
                            activeField = ActiveField.NONE,
                            partialTopText = "",
                            partialBottomText = "",
                            errorMessage = if (translationService.isOnline()) null else result.message,
                            errorLocale = if (translationService.isOnline()) null else localeCode
                        )}
                    }
                }
            }
        }
    }

    /**
     * Startet die Erkennung ohne Offline-Zwang als schneller Fallback.
     */
    private fun startListeningOnline(field: ActiveField, localeCode: String) {
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            speechService.recognize(localeCode, preferOffline = false).collect { result ->
                when (result) {
                    is SpeechResult.Ready -> {}
                    is SpeechResult.Partial -> {
                        if (field == ActiveField.TOP) _uiState.update { it.copy(partialTopText = result.text) }
                        else _uiState.update { it.copy(partialBottomText = result.text) }
                    }
                    is SpeechResult.Final -> {
                        _uiState.update { it.copy(partialTopText = "", partialBottomText = "", activeField = ActiveField.NONE) }
                        if (result.text.isNotBlank()) processSpokenText(result.text, field)
                    }
                    is SpeechResult.Error -> {
                        _uiState.update { it.copy(activeField = ActiveField.NONE, errorMessage = result.message) }
                    }
                }
            }
        }
    }

    fun stopListening() {
        speechJob?.cancel()
        speechService.stopListening()
        _uiState.update { it.copy(
            activeField = ActiveField.NONE,
            partialTopText = "",
            partialBottomText = ""
        )}
    }

    // ─────────────────────────────────────────────
    // Übersetzung
    // ─────────────────────────────────────────────

    private fun processSpokenText(spokenText: String, fromField: ActiveField) {
        val state = _uiState.value
        val fromLang = if (fromField == ActiveField.TOP) state.topLanguage else state.bottomLanguage
        val toLang = if (fromField == ActiveField.TOP) state.bottomLanguage else state.topLanguage

        // Regionalkürzel entfernen, damit die KI nicht verwirrt wird (z.B. "Deutsch (DE)" -> "Deutsch")
        val cleanFrom = fromLang.displayName.substringBefore(" (")
        val cleanTo = toLang.displayName.substringBefore(" (")

        android.util.Log.d("TranslatorViewModel", "Übersetze: '$spokenText' ($cleanFrom -> $cleanTo)")
        checkTranslationMode()

        translationJob = viewModelScope.launch {
            var finalTranslation = ""

            translationService.translate(
                text = spokenText,
                fromLanguage = cleanFrom,
                toLanguage = cleanTo
            ).collect { result ->
                when (result) {
                    is TranslationResult.Success -> {
                        finalTranslation = result.text
                        // Während des Tests zeigen wir das Ergebnis im partialTranslation Feld
                        _uiState.update { it.copy(
                            partialTranslation = result.text,
                            translationMode = result.mode
                        )}
                    }
                    is TranslationResult.Error -> {
                        _uiState.update { it.copy(
                            errorMessage = result.message,
                            partialTranslation = "",
                            translationMode = TranslationMode.ERROR
                        )}
                        // Block auch bei Fehler mit Original-Text speichern (für ki_test wichtig!)
                        if (spokenText.isNotBlank()) {
                            saveBlock(spokenText, "⚠️ Übersetzung fehlgeschlagen", fromField)
                        }
                        return@collect
                    }
                }
            }

            // Finalen Block speichern
            if (finalTranslation.isNotBlank()) {
                _uiState.update { it.copy(partialTranslation = "") }
                saveBlock(spokenText, finalTranslation, fromField)
            }
        }
    }

    private suspend fun saveBlock(originalText: String, translation: String, fromField: ActiveField) {
        val block = ConversationBlock(
            topText = if (fromField == ActiveField.TOP) originalText else translation,
            bottomText = if (fromField == ActiveField.TOP) translation else originalText,
            topLineCount = estimateLineCount(
                if (fromField == ActiveField.TOP) originalText else translation
            ),
            bottomLineCount = estimateLineCount(
                if (fromField == ActiveField.TOP) translation else originalText
            ),
            isFromTop = fromField == ActiveField.TOP,
            sessionId = currentSessionId
        )

        val id = dao.insertBlock(block)
        val savedBlock = block.copy(id = id)

        _uiState.update { state ->
            state.copy(blocks = state.blocks + savedBlock)
        }
    }

    /** Schätzt die Zeilenzahl bei ~35 Zeichen pro Zeile */
    private fun estimateLineCount(text: String): Int {
        val charsPerLine = 35
        return maxOf(1, text.length / charsPerLine + text.count { it == '\n' } + 1)
    }

    // ─────────────────────────────────────────────
    // Text-to-Speech
    // ─────────────────────────────────────────────

    /**
     * Liest einen Textblock vor.
     * @param blockId ID des Blocks
     * @param text Der vorzulesende Text
     * @param localeCode Sprache für TTS
     */
    fun speakBlock(blockId: Long, text: String, localeCode: String) {
        resetInactivityTimer()
        if (_uiState.value.ttsBlockId == blockId) {
            // Gleiches Feld nochmal → stoppen
            tts.stop()
            _uiState.update { it.copy(isSpeaking = false, ttsBlockId = null) }
            return
        }

        val locale = java.util.Locale.forLanguageTag(localeCode)
        tts.language = locale
        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "tts_$blockId")
        _uiState.update { it.copy(isSpeaking = true, ttsBlockId = blockId) }

        viewModelScope.launch {
            kotlinx.coroutines.delay(text.length * 80L) // Grobe Schätzung Lesezeit
            _uiState.update { it.copy(isSpeaking = false, ttsBlockId = null) }
        }
    }

    // ─────────────────────────────────────────────
    // Sprache wechseln
    // ─────────────────────────────────────────────

    fun setTopLanguage(lang: LanguageOption) {
        resetInactivityTimer()
        _uiState.update { it.copy(topLanguage = lang) }
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_TOP_LOCALE] = lang.localeCode
            }
        }
        if (_uiState.value.kiTest) {
            triggerGreetingTest()
        }
    }

    fun setBottomLanguage(lang: LanguageOption) {
        resetInactivityTimer()
        _uiState.update { it.copy(bottomLanguage = lang) }
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_BOTTOM_LOCALE] = lang.localeCode
            }
        }
        if (_uiState.value.kiTest) {
            triggerGreetingTest()
        }
    }

    // ─────────────────────────────────────────────
    // Gespräch speichern / löschen
    // ─────────────────────────────────────────────

    fun clearConversation() {
        resetInactivityTimer()
        viewModelScope.launch {
            dao.deleteSession(currentSessionId)
            currentSessionId = System.currentTimeMillis()
            _uiState.update { it.copy(blocks = emptyList(), errorMessage = null) }
        }
    }

    fun saveConversation(): String {
        val state = _uiState.value
        val sb = StringBuilder()
        sb.appendLine("SprachBrücke – Gesprächsprotokoll")
        sb.appendLine("${state.topLanguage.flag} ${state.topLanguage.displayName}  |  " +
                      "${state.bottomLanguage.flag} ${state.bottomLanguage.displayName}")
        sb.appendLine("═".repeat(60))
        state.blocks.forEach { block ->
            sb.appendLine()
            sb.appendLine("[${state.topLanguage.flag}] ${block.topText}")
            sb.appendLine("[${state.bottomLanguage.flag}] ${block.bottomText}")
            sb.appendLine("─".repeat(40))
        }
        return sb.toString()
    }

    fun triggerDownload() {
        val locale = _uiState.value.errorLocale ?: return
        speechService.triggerDownload(locale)
        dismissError()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null, errorLocale = null) }
    }

    // ─────────────────────────────────────────────
    // Einstellungen & Layout
    // ─────────────────────────────────────────────

    fun setRotation(field: ActiveField, rotation: Int) {
        resetInactivityTimer()
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                if (field == ActiveField.TOP) {
                    prefs[KEY_TOP_ROTATION] = rotation
                    _uiState.update { it.copy(topRotation = rotation) }
                } else {
                    prefs[KEY_BOTTOM_ROTATION] = rotation
                    _uiState.update { it.copy(bottomRotation = rotation) }
                }
            }
        }
    }

    fun setThemeMode(mode: String) {
        resetInactivityTimer()
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_THEME_MODE] = mode
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun setFontSize(field: ActiveField, size: Float) {
        resetInactivityTimer()
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                if (field == ActiveField.TOP) {
                    prefs[KEY_TOP_FONT_SIZE] = size
                    _uiState.update { it.copy(topFontSize = size) }
                } else {
                    prefs[KEY_BOTTOM_FONT_SIZE] = size
                    _uiState.update { it.copy(bottomFontSize = size) }
                }
            }
        }
    }

    fun setFontColor(field: ActiveField, color: Long) {
        resetInactivityTimer()
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                if (field == ActiveField.TOP) {
                    prefs[KEY_TOP_FONT_COLOR] = color
                    _uiState.update { it.copy(topFontColor = color) }
                } else {
                    prefs[KEY_BOTTOM_FONT_COLOR] = color
                    _uiState.update { it.copy(bottomFontColor = color) }
                }
            }
        }
    }

    fun setAppLocale(languageCode: String) {
        resetInactivityTimer()
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_APP_LOCALE] = languageCode
                _uiState.update { it.copy(appLocale = languageCode) }
            }
        }
    }

    fun checkForUpdates() {
        resetInactivityTimer()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://qr2go.org/Sprachen/index.html")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                
                // Regex für <meta name="app-version" content="0.1">
                val regex = """<meta\s+name="app-version"\s+content="([^"]+)"""".toRegex()
                val match = regex.find(html)
                val version = match?.groupValues?.get(1)
                
                _uiState.update { it.copy(latestVersion = version ?: "N/A") }
            } catch (e: Exception) {
                android.util.Log.e("TranslatorViewModel", "Update-Check fehlgeschlagen", e)
                _uiState.update { it.copy(latestVersion = "Error") }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Hilfsfunktionen
    // ─────────────────────────────────────────────

    private fun checkTranslationMode() {
        val mode = if (translationService.isOnline()) {
            TranslationMode.CLOUD
        } else {
            TranslationMode.ERROR
        }
        _uiState.update { it.copy(translationMode = mode) }
    }

    override fun onCleared() {
        super.onCleared()
        speechService.stopListening()
        tts.shutdown()
    }
}
