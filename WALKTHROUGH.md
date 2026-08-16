# SprachBrücke – Projekt-Dokumentation

## ✅ Was wurde erstellt

29 Dateien im vollständigen Android-Kotlin-Projekt unter:
`C:\Users\Panier\Desktop\Urlaub\_Antigravity\Sprache_Uebersetzen\`

---

## 📁 Projektstruktur

```
SprachBrücke/
├── build.gradle.kts              ← Projekt-Konfiguration (AGP 8.5, Kotlin 2.0)
├── settings.gradle.kts           ← Modulkonfiguration
├── local.properties              ← API-Key + SDK-Pfad (NICHT in Git!)
├── local.properties.template     ← Vorlage für neue Entwickler
├── .gitignore                    ← local.properties ist ausgeschlossen
│
├── gradle/wrapper/
│   └── gradle-wrapper.properties ← Gradle 8.9
│
└── app/
    ├── build.gradle.kts          ← App-Dependencies (Compose, Room, Gemini)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml   ← Permissions: RECORD_AUDIO, INTERNET
        ├── res/values/
        │   ├── strings.xml
        │   └── themes.xml
        └── kotlin/com/sprachbruecke/translator/
            │
            ├── MainActivity.kt           ← Einstiegspunkt + Navigation
            │
            ├── data/
            │   ├── LanguageOption.kt     ← 16 Sprachen mit Emoji-Flaggen
            │   ├── ConversationBlock.kt  ← Datenmodell (Room Entity)
            │   ├── ConversationDao.kt    ← Datenbankoperationen
            │   └── AppDatabase.kt        ← Room-Datenbank (Singleton)
            │
            ├── service/
            │   ├── ApiKeyProvider.kt     ← Interface + LocalImpl (→ Server-Impl geplant)
            │   ├── SpeechService.kt      ← Android SpeechRecognizer als Flow
            │   └── TranslationService.kt ← Gemini 2.0 Flash (Streaming)
            │
            ├── viewmodel/
            │   └── TranslatorViewModel.kt ← Zustandsverwaltung + Koordination
            │
            └── ui/
                ├── TranslatorScreen.kt   ← Hauptbildschirm (synchrones Scrollen)
                ├── DividerBar.kt         ← Trennlinie mit Modus-Indikator
                ├── LanguageButton.kt     ← Flagge + Name Button
                ├── LanguagePickerDialog.kt ← Sprachauswahl-Dialog
                ├── SettingsScreen.kt     ← Einstellungen
                └── theme/
                    ├── Color.kt          ← Farben (Aktiv=Gelb, Cloud=Blau, Fehler=Rot)
                    ├── Theme.kt          ← Material3 Light/Dark Theme
                    └── Type.kt           ← Schriften (min. 20sp)
```

---

## 🚀 Setup-Anleitung

### Schritt 1: Android Studio installieren
1. [Android Studio](https://developer.android.com/studio) herunterladen und installieren
2. Beim ersten Start: Android SDK installieren lassen (Standard-Pfad)

### Schritt 2: SDK-Pfad in local.properties eintragen
Die Datei `local.properties` anpassen:
```properties
sdk.dir=C\:\\Users\\Panier\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=DEIN_GEMINI_API_KEY_HIER
```

### Schritt 3: Gemini API-Key holen
1. [Google AI Studio](https://aistudio.google.com) aufrufen
2. „Get API key" → Neuen Key erstellen
3. Key in `local.properties` eintragen (niemals committen!)

### Schritt 4: Projekt in Android Studio öffnen
1. Android Studio starten
2. „Open" → Ordner `Sprache_Uebersetzen` wählen
3. Gradle Sync abwarten (~2 Minuten beim ersten Mal)

### Schritt 5: Offline-Sprachpakete herunterladen
Auf dem Android-Gerät:
> Einstellungen → Allgemeine Verwaltung → Sprache → Sprachpakete für Spracherkennung

### Schritt 6: App auf Gerät starten
1. Android-Gerät per USB verbinden
2. USB-Debugging aktivieren (Entwickleroptionen)
3. In Android Studio: ▶️ Run

---

## 🔑 API-Key Architektur (Zukunftssicher)

```
Entwicklung:               Produktion (geplant):
┌─────────────────┐       ┌─────────────────────────┐
│ local.properties │       │ Auth-Server             │
│ GEMINI_API_KEY= │  →→→  │ POST /auth/login        │
│ BuildConfig     │       │ GET /api/gemini-key     │
└─────────────────┘       └─────────────────────────┘
         ↓                          ↓
  LocalApiKeyProvider       ServerApiKeyProvider
         ↓                          ↓
         └──── ApiKeyProvider ──────┘
                    ↓
            TranslationService
```
Für Produktion: nur `ServerApiKeyProvider` implementieren, kein App-Code ändern.

---

## 🎨 UI-Farb-Konzept

| Element | Farbe | Bedeutung |
|---------|-------|-----------|
| Aktives Feld | 🟡 Creme-Gelb `#FFF9C4` | Mikrofon hört zu |
| Cloud-Modus ☁️ | 🔵 Blau | Gemini Flash (Internet) |
| On-Device 📱 | 🟢 Grün | Gemini Nano (lokal) |
| Fehler 🔴 | 🔴 Rot pulsierend | Kein Netz |
| Inaktives Feld | ⬜ System-Standard | Warten |

---

## 🔄 Synchrones Scrollen – Algorithmus

```
Block N hat:
  topLineCount = 3 (Deutsch: kurzer Satz)
  bottomLineCount = 5 (Chinesisch: mehr Zeilen)

Wenn oberes Feld scrollt:
  offset_unten = offset_oben × (bottomLineCount / topLineCount)
  offset_unten = offset_oben × (5/3) = offset_oben × 1.67

→ Beide Blöcke bleiben proportional ausgerichtet
```

---

## 📋 Nächste Schritte (TODOs im Code)

1. **Gradle Wrapper JAR** – wird von Android Studio beim ersten Sync automatisch erstellt
2. **App-Icon** – aktuell Standard-Icon, eigenes Icon erstellen
3. **Gespräch-Export** – `onSaveConversation()` in MainActivity mit Share-Intent fertigstellen
4. **Bessere Zeilenzahl-Berechnung** – aktuell Schätzung, könnte mit `onTextLayout` präzisiert werden
5. **Server-Auth** – `ServerApiKeyProvider` für Produktion implementieren
6. **Landscape-Layout** – adaptive Layout mit `WindowSizeClass` (Phase 2)

---

## ⚠️ Bekannte Einschränkungen (V1)

- **Zeilenzahl-Schätzung**: ~35 Zeichen/Zeile (ausreichend für V1, präziser mit Layout-Messung)
- **Offline-Übersetzung**: Noch nicht verfügbar (Gemini Nano Integration aufwändig, Cloud als primärer Kanal)
- **Landscape**: Noch nicht angepasst (funktioniert, aber nicht optimiert)
