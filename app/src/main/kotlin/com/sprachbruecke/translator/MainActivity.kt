package com.sprachbruecke.translator

import android.Manifest
import android.app.LocaleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sprachbruecke.translator.data.LanguageOption
import com.sprachbruecke.translator.ui.*
import com.sprachbruecke.translator.ui.theme.SprachBrueckeTheme
import com.sprachbruecke.translator.viewmodel.TranslatorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()

    // Mikrofon-Permission anfragen
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Mikrofon-Berechtigung wird für die Spracherkennung benötigt.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Mikrofon-Berechtigung prüfen/anfragen
        checkMicrophonePermission()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            
            // Theme-Logik
            val isDark = when (uiState.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            SprachBrueckeTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Berechtigung vorhanden
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

/**
 * App-Navigation: Haupt-Screen und Einstellungen.
 */
@Composable
fun AppNavigation(viewModel: TranslatorViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Always-On Steuerung ──────────────────────────────────────────────────
    val context = LocalContext.current
    SideEffect {
        val window = (context as? android.app.Activity)?.window
        if (uiState.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    // ────────────────────────────────────────────────────────────────────────

    // ── App-Sprache anwenden ────────────────────────────────────────────────
    LaunchedEffect(uiState.appLocale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (localeManager.applicationLocales.toLanguageTags() != uiState.appLocale) {
                localeManager.applicationLocales = LocaleList.forLanguageTags(uiState.appLocale)
            }
        } else {
            // Für ältere Versionen ist es komplizierter, wir nutzen hier primär Tiramisu+ für Pixel 9
            // Eine einfachere Lösung für Compose ist oft die Neukonfiguration des Contexts, 
            // aber wir vertrauen auf das Pixel 9.
        }
    }
    // ────────────────────────────────────────────────────────────────────────

    var showSettings by remember { mutableStateOf(false) }
    var showTopLanguagePicker by remember { mutableStateOf(false) }
    var showBottomLanguagePicker by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            uiState = uiState,
            onBack = { showSettings = false },
            onSetRotation = viewModel::setRotation,
            onSetThemeMode = viewModel::setThemeMode,
            onSetFontSize = viewModel::setFontSize,
            onSetFontColor = viewModel::setFontColor,
            onSetAppLocale = viewModel::setAppLocale,
            onCheckUpdate = viewModel::checkForUpdates
        )
    } else {
        TranslatorScreen(
            uiState = uiState,
            onFieldTapped = viewModel::onFieldTapped,
            onTopLanguageClick = { showTopLanguagePicker = true },
            onBottomLanguageClick = { showBottomLanguagePicker = true },
            onSettingsClick = { showSettings = true },
            onSpeakBlock = viewModel::speakBlock,
            onSaveConversation = {
                val text = viewModel.saveConversation()
                // Text als Share-Intent teilen
                // TODO: Könnte auch als Datei gespeichert werden
            },
            onClearConversation = viewModel::clearConversation,
            onDismissError = viewModel::dismissError,
            onTriggerDownload = viewModel::triggerDownload,
        )
    }

    // Sprach-Auswahl Dialoge
    if (showTopLanguagePicker) {
        LanguagePickerDialog(
            title = "Sprache oben wählen",
            currentLanguage = uiState.topLanguage,
            onLanguageSelected = { lang: LanguageOption ->
                viewModel.setTopLanguage(lang)
            },
            onDismiss = { showTopLanguagePicker = false }
        )
    }

    if (showBottomLanguagePicker) {
        LanguagePickerDialog(
            title = "Sprache unten wählen",
            currentLanguage = uiState.bottomLanguage,
            onLanguageSelected = { lang: LanguageOption ->
                viewModel.setBottomLanguage(lang)
            },
            onDismiss = { showBottomLanguagePicker = false }
        )
    }
}
