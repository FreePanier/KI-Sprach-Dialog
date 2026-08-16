package com.sprachbruecke.translator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprachbruecke.translator.R
import com.sprachbruecke.translator.viewmodel.ActiveField
import com.sprachbruecke.translator.viewmodel.TranslatorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: TranslatorUiState,
    onBack: () -> Unit,
    onSetRotation: (ActiveField, Int) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onSetFontSize: (ActiveField, Float) -> Unit,
    onSetFontColor: (ActiveField, Long) -> Unit,
    onSetAppLocale: (String) -> Unit,
    onCheckUpdate: () -> Unit
) {
    var activeMenu by remember { mutableStateOf<SettingsMenu?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (activeMenu == null) stringResource(R.string.settings) 
                               else stringResource(activeMenu!!.titleRes), 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (activeMenu == null) onBack() else activeMenu = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (activeMenu == null) {
                // Hauptmenü
                SettingsMenuItem(
                    icon = Icons.Default.Dashboard,
                    title = stringResource(R.string.layout),
                    onClick = { activeMenu = SettingsMenu.LAYOUT }
                )
                SettingsMenuItem(
                    icon = Icons.Default.Translate,
                    title = stringResource(R.string.app_language),
                    onClick = { activeMenu = SettingsMenu.LANGUAGE }
                )
                SettingsMenuItem(
                    icon = Icons.Default.SystemUpdate,
                    title = stringResource(R.string.update),
                    onClick = { 
                        activeMenu = SettingsMenu.UPDATE
                        onCheckUpdate()
                    }
                )
                SettingsMenuItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.about_app),
                    onClick = { activeMenu = SettingsMenu.ABOUT }
                )
            } else {
                // Untermenüs
                when (activeMenu) {
                    SettingsMenu.LAYOUT -> LayoutSettings(uiState, onSetRotation, onSetThemeMode, onSetFontSize, onSetFontColor)
                    SettingsMenu.LANGUAGE -> LanguageSettings(uiState, onSetAppLocale)
                    SettingsMenu.UPDATE -> UpdateSettings(uiState, onCheckUpdate, onShowInfo = { showUpdateDialog = true })
                    SettingsMenu.ABOUT -> AboutAppContent()
                    else -> {}
                }
            }
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            confirmButton = { TextButton(onClick = { showUpdateDialog = false }) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(R.string.update_later_msg)) }
        )
    }
}

enum class SettingsMenu(val titleRes: Int) {
    LAYOUT(R.string.layout),
    LANGUAGE(R.string.app_language),
    UPDATE(R.string.update),
    ABOUT(R.string.about_app)
}

@Composable
private fun SettingsMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
private fun LayoutSettings(
    uiState: TranslatorUiState,
    onSetRotation: (ActiveField, Int) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onSetFontSize: (ActiveField, Float) -> Unit,
    onSetFontColor: (ActiveField, Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        
        // Theme
        Text(stringResource(R.string.background_color), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uiState.themeMode == "Light", onClick = { onSetThemeMode("Light") }, label = { Text(stringResource(R.string.light_mode)) })
            FilterChip(selected = uiState.themeMode == "Dark", onClick = { onSetThemeMode("Dark") }, label = { Text(stringResource(R.string.dark_mode)) })
            FilterChip(selected = uiState.themeMode == "System", onClick = { onSetThemeMode("System") }, label = { Text("System") })
        }

        HorizontalDivider()

        // OBERES FELD
        Text(stringResource(R.string.language_top), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        
        // Rotation Top
        Text(stringResource(R.string.text_rotation), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uiState.topRotation == 0, onClick = { onSetRotation(ActiveField.TOP, 0) }, label = { Text(stringResource(R.string.rotation_normal)) })
            FilterChip(selected = uiState.topRotation == 180, onClick = { onSetRotation(ActiveField.TOP, 180) }, label = { Text(stringResource(R.string.rotation_flipped)) })
        }

        // Font Size Top
        Text("${stringResource(R.string.font_size)}: ${uiState.topFontSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium)
        Slider(value = uiState.topFontSize, onValueChange = { onSetFontSize(ActiveField.TOP, it) }, valueRange = 16f..48f)

        // Color Top (Einfache Auswahl)
        Text(stringResource(R.string.font_color), style = MaterialTheme.typography.bodyMedium)
        ColorRow(selectedColor = uiState.topFontColor) { onSetFontColor(ActiveField.TOP, it) }

        HorizontalDivider()

        // UNTERES FELD
        Text(stringResource(R.string.language_bottom), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        
        // Rotation Bottom
        Text(stringResource(R.string.text_rotation), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uiState.bottomRotation == 0, onClick = { onSetRotation(ActiveField.BOTTOM, 0) }, label = { Text(stringResource(R.string.rotation_normal)) })
            FilterChip(selected = uiState.bottomRotation == 180, onClick = { onSetRotation(ActiveField.BOTTOM, 180) }, label = { Text(stringResource(R.string.rotation_flipped)) })
        }

        // Font Size Bottom
        Text("${stringResource(R.string.font_size)}: ${uiState.bottomFontSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium)
        Slider(value = uiState.bottomFontSize, onValueChange = { onSetFontSize(ActiveField.BOTTOM, it) }, valueRange = 16f..48f)

        // Color Bottom
        Text(stringResource(R.string.font_color), style = MaterialTheme.typography.bodyMedium)
        ColorRow(selectedColor = uiState.bottomFontColor) { onSetFontColor(ActiveField.BOTTOM, it) }
    }
}

@Composable
private fun ColorRow(selectedColor: Long, onColorSelected: (Long) -> Unit) {
    val colors = listOf(0xFF000000, 0xFFFFFFFF, 0xFF1976D2, 0xFF388E3C, 0xFFD32F2F, 0xFFFFD600)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { colorVal ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onColorSelected(colorVal) }
                    .padding(2.dp)
                    .graphicsLayer {
                        if (selectedColor == colorVal) {
                            scaleX = 1.2f
                            scaleY = 1.2f
                        }
                    }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(colorVal),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                ) {}
            }
        }
    }
}

@Composable
private fun LanguageSettings(uiState: TranslatorUiState, onSetAppLocale: (String) -> Unit) {
    val languages = listOf("de" to "Deutsch", "en" to "English", "fr" to "Français", "es" to "Español")
    Column(modifier = Modifier.padding(16.dp)) {
        languages.forEach { (code, name) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetAppLocale(code) }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                RadioButton(selected = uiState.appLocale == code, onClick = { onSetAppLocale(code) })
            }
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

@Composable
private fun UpdateSettings(uiState: TranslatorUiState, onCheckUpdate: () -> Unit, onShowInfo: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.check_for_updates), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(stringResource(R.string.current_version), "1.1.0")
                InfoRow(stringResource(R.string.latest_version), uiState.latestVersion ?: "...")
            }
        }
        
        Button(
            onClick = onShowInfo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.update))
        }

        OutlinedButton(
            onClick = onCheckUpdate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.check_for_updates))
        }
    }
}

@Composable
private fun AboutAppContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoRow(stringResource(R.string.app_name), "SprachBrücke")
        InfoRow(stringResource(R.string.version), "1.1.0")
        InfoRow(stringResource(R.string.model), "Gemini Flash")
        InfoRow(stringResource(R.string.speech_recognition), "Android Speech Service")
        
        Spacer(Modifier.height(16.dp))
        Text(
            "Entwickelt für einfache Kommunikation zwischen Sprachen.\n\n" +
            "Nutzt Google Gemini für präzise Übersetzungen und Android Offline-Pakete für Spracherkennung.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
