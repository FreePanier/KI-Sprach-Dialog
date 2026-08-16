package com.sprachbruecke.translator.data

/**
 * Repräsentiert eine unterstützte Sprache mit Flagge, Name und Locale-Code.
 */
data class LanguageOption(
    val flag: String,        // Emoji-Flagge, z.B. "🇩🇪"
    val displayName: String, // Anzeigename, z.B. "Deutsch"
    val localeCode: String,  // BCP-47 Code, z.B. "de-DE"
)

/**
 * Alle unterstützten Sprachen mit Offline-Sprachpaket-Unterstützung unter Android.
 */
val SUPPORTED_LANGUAGES = listOf(
    // Deutsch Varianten
    LanguageOption("🇩🇪", "Deutsch (DE)",      "de-DE"),
    LanguageOption("🇦🇹", "Deutsch (AT)",      "de-AT"),
    LanguageOption("🇨🇭", "Deutsch (CH)",      "de-CH"),

    // Englisch Varianten
    LanguageOption("🇺🇸", "English (USA)",     "en-US"),
    LanguageOption("🇬🇧", "English (GB)",      "en-GB"),
    LanguageOption("🇦🇺", "English (AU)",      "en-AU"),
    LanguageOption("🇮🇳", "English (IN)",      "en-IN"),

    // Spanisch Varianten
    LanguageOption("🇪🇸", "Español (ES)",      "es-ES"),
    LanguageOption("🇲🇽", "Español (MX)",      "es-MX"),
    LanguageOption("🇺🇸", "Español (US)",      "es-US"),

    // Französisch Varianten
    LanguageOption("🇫🇷", "Français (FR)",     "fr-FR"),
    LanguageOption("🇨🇦", "Français (CA)",     "fr-CA"),

    // Italienisch
    LanguageOption("🇮🇹", "Italiano",          "it-IT"),

    // Portugiesisch Varianten
    LanguageOption("🇵🇹", "Português (PT)",    "pt-PT"),
    LanguageOption("🇧🇷", "Português (BR)",    "pt-BR"),

    // Weitere Sprachen
    LanguageOption("🇳🇱", "Nederlands",        "nl-NL"),
    LanguageOption("🇵🇱", "Polski",            "pl-PL"),
    LanguageOption("🇷🇺", "Русский",           "ru-RU"),
    LanguageOption("🇹🇷", "Türkçe",            "tr-TR"),
    LanguageOption("🇸🇦", "العربية",           "ar-SA"),
    LanguageOption("🇨🇳", "中文 (CN)",         "zh-CN"),
    LanguageOption("🇯🇵", "日本語",             "ja-JP"),
    LanguageOption("🇰🇷", "한국어",             "ko-KR"),
    LanguageOption("🇮🇳", "हिन्दी",            "hi-IN"),
)

val DEFAULT_TOP_LANGUAGE = SUPPORTED_LANGUAGES.first { it.localeCode == "de-DE" }
val DEFAULT_BOTTOM_LANGUAGE = SUPPORTED_LANGUAGES.first { it.localeCode == "it-IT" }
