package com.sprachbruecke.translator.service

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*

class TranslationServiceTest {

    @Test
    fun testManualTranslation() = runBlocking {
        // Wir simulieren den ApiKeyProvider mit dem Key aus der local.properties (manuell für den Test)
        // HINWEIS: Da Unit-Tests keinen Zugriff auf BuildConfig/Context haben wie die App,
        // nutzen wir hier eine einfache Mock-Implementierung.
        
        val properties = Properties()
        val propFile = java.io.File("../local.properties")
        if (propFile.exists()) {
            properties.load(propFile.inputStream())
        }
        val apiKey = properties.getProperty("GEMINI_API_KEY", "")
        
        println("Nutze API Key: ${if (apiKey.length > 5) apiKey.take(5) + "..." else "NICHT GEFUNDEN"}")

        val mockProvider = object : ApiKeyProvider {
            override suspend fun getApiKey(): String = apiKey
        }

        // Wir brauchen einen minimalen Mock Context für TranslationService (nur für ConnectivityManager)
        // Da Mocking von Context in Unit-Tests ohne Mockito schwierig ist, 
        // passen wir den Service kurz für den Test an oder nutzen ein Interface.
        
        println("Starte Test-Übersetzung...")
        
        // Hier rufen wir die Logik auf. 
        // Da der echte TranslationService den ConnectivityManager nutzt, 
        // wird er im reinen JUnit-Test ohne Robolectric abstürzen.
        // Deshalb analysieren wir stattdessen die Logcat-Fehler direkt vom Handy.
    }
}
