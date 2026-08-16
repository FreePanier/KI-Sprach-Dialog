package com.sprachbruecke.translator.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprachbruecke.translator.data.ConversationBlock
import com.sprachbruecke.translator.data.LanguageOption
import com.sprachbruecke.translator.service.TranslationMode
import com.sprachbruecke.translator.ui.theme.*
import com.sprachbruecke.translator.viewmodel.ActiveField
import com.sprachbruecke.translator.viewmodel.TranslatorUiState
import kotlinx.coroutines.launch

/**
 * Haupt-Übersetzungsbildschirm.
 *
 * Layout (Portrait):
 *  ┌─ TopBar: Flagge A + ⚙️ ─────────────────┐
 *  │  OBERES FELD (Sprache A)                  │
 *  │  [Gesprächsblöcke scrollbar]              │
 *  ├─ Trennlinie mit Modus-Indikator ──────────┤
 *  │  UNTERES FELD (Sprache B)                 │
 *  │  [Gesprächsblöcke scrollbar]              │
 *  └─ BottomBar: Flagge B ─────────────────────┘
 */
@Composable
fun TranslatorScreen(
    uiState: TranslatorUiState,
    onFieldTapped: (ActiveField) -> Unit,
    onTopLanguageClick: () -> Unit,
    onBottomLanguageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSpeakBlock: (blockId: Long, text: String, localeCode: String) -> Unit,
    onSaveConversation: () -> Unit,
    onClearConversation: () -> Unit,
    onDismissError: () -> Unit,
    onTriggerDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val topListState = rememberLazyListState()
    val bottomListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // ── Synchrones Scrollen ──────────────────────────────────────────────────
    // Wenn das obere Feld scrollt, scrollt das untere proportional mit.
    var isSyncingFromTop by remember { mutableStateOf(false) }
    var isSyncingFromBottom by remember { mutableStateOf(false) }

    // Top → Bottom synchronisieren
    LaunchedEffect(topListState.firstVisibleItemIndex, topListState.firstVisibleItemScrollOffset) {
        if (!isSyncingFromBottom && uiState.blocks.isNotEmpty()) {
            isSyncingFromTop = true
            val idx = topListState.firstVisibleItemIndex.coerceAtMost(uiState.blocks.lastIndex)
            val offset = topListState.firstVisibleItemScrollOffset
            val scaledOffset = scaleScrollOffset(uiState.blocks, idx, offset, fromTop = true)
            bottomListState.scrollToItem(idx, scaledOffset)
            isSyncingFromTop = false
        }
    }

    // Bottom → Top synchronisieren
    LaunchedEffect(bottomListState.firstVisibleItemIndex, bottomListState.firstVisibleItemScrollOffset) {
        if (!isSyncingFromTop && uiState.blocks.isNotEmpty()) {
            isSyncingFromBottom = true
            val idx = bottomListState.firstVisibleItemIndex.coerceAtMost(uiState.blocks.lastIndex)
            val offset = bottomListState.firstVisibleItemScrollOffset
            val scaledOffset = scaleScrollOffset(uiState.blocks, idx, offset, fromTop = false)
            topListState.scrollToItem(idx, scaledOffset)
            isSyncingFromBottom = false
        }
    }

    // Automatisch zum neuesten Block scrollen wenn ein neuer hinzukommt
    LaunchedEffect(uiState.blocks.size) {
        if (uiState.blocks.isNotEmpty()) {
            coroutineScope.launch {
                topListState.animateScrollToItem(uiState.blocks.lastIndex)
            }
            coroutineScope.launch {
                bottomListState.animateScrollToItem(uiState.blocks.lastIndex)
            }
        }
    }
    // ────────────────────────────────────────────────────────────────────────

    Column(modifier = modifier
        .fillMaxSize()
        .systemBarsPadding()
    ) {

        // ── TopBar: Sprache A + Menü ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageButton(
                language = uiState.topLanguage,
                onClick = onTopLanguageClick
            )
            Row {
                if (uiState.blocks.isNotEmpty()) {
                    // Gespräch speichern
                    IconButton(onClick = onSaveConversation) {
                        Icon(Icons.Default.Save, contentDescription = "Gespräch speichern")
                    }
                    // Gespräch löschen
                    IconButton(onClick = onClearConversation) {
                        Icon(Icons.Default.Delete, contentDescription = "Gespräch löschen",
                             tint = MaterialTheme.colorScheme.error)
                    }
                }
                // Einstellungen
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                }
            }
        }

        // ── Oberes Sprachfeld (Sprache A) ────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (uiState.activeField == ActiveField.TOP) ActiveFieldBackground
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = if (uiState.activeField == ActiveField.TOP) 4.dp else 0.dp,
                    color = if (uiState.activeField == ActiveField.TOP) ActiveFieldBorder
                            else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFieldTapped(ActiveField.TOP)
                }
                .graphicsLayer { rotationZ = uiState.topRotation.toFloat() }
                .padding(8.dp)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides Color(uiState.topFontColor)
            ) {
                ConversationPanel(
                    blocks = uiState.blocks,
                    isTopPanel = true,
                    language = uiState.topLanguage,
                    listState = topListState,
                    activeField = uiState.activeField,
                    partialText = uiState.partialTopText,
                    ttsBlockId = uiState.ttsBlockId,
                    onSpeakBlock = onSpeakBlock,
                    fontSize = uiState.topFontSize,
                    fontColor = Color(uiState.topFontColor)
                )
            }

            // Mikrofon-Status-Overlay oben rechts
            if (uiState.activeField == ActiveField.TOP) {
                MicrophoneIndicator(
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }

        // ── Trennlinie mit Modus-Indikator ───────────────────────────────────
        DividerBar(
            translationMode = uiState.translationMode,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // ── Unteres Sprachfeld (Sprache B) ───────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (uiState.activeField == ActiveField.BOTTOM) ActiveFieldBackground
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = if (uiState.activeField == ActiveField.BOTTOM) 4.dp else 0.dp,
                    color = if (uiState.activeField == ActiveField.BOTTOM) ActiveFieldBorder
                            else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFieldTapped(ActiveField.BOTTOM)
                }
                .graphicsLayer { rotationZ = uiState.bottomRotation.toFloat() }
                .padding(8.dp)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides Color(uiState.bottomFontColor)
            ) {
                ConversationPanel(
                    blocks = uiState.blocks,
                    isTopPanel = false,
                    language = uiState.bottomLanguage,
                    listState = bottomListState,
                    activeField = uiState.activeField,
                    partialText = uiState.partialBottomText.ifBlank { uiState.partialTranslation },
                    ttsBlockId = uiState.ttsBlockId,
                    onSpeakBlock = onSpeakBlock,
                    fontSize = uiState.bottomFontSize,
                    fontColor = Color(uiState.bottomFontColor)
                )
            }

            if (uiState.activeField == ActiveField.BOTTOM) {
                MicrophoneIndicator(
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }

        // ── BottomBar: Sprache B ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            LanguageButton(
                language = uiState.bottomLanguage,
                onClick = onBottomLanguageClick
            )
        }
    }

    // ── Fehler-Snackbar ───────────────────────────────────────────────────────
    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Hinweis") },
            text = { Text(error, fontSize = 16.sp) },
            confirmButton = {
                if (uiState.errorLocale != null) {
                    Button(onClick = onTriggerDownload) {
                        Text("Jetzt laden", fontSize = 16.sp)
                    }
                } else {
                    TextButton(onClick = onDismissError) {
                        Text("OK", fontSize = 16.sp)
                    }
                }
            },
            dismissButton = {
                if (uiState.errorLocale != null) {
                    TextButton(onClick = onDismissError) {
                        Text("Abbrechen", fontSize = 16.sp)
                    }
                }
            }
        )
    }
}

/**
 * Eine Seite (oben oder unten) des Gesprächs als scrollbare Liste.
 */
@Composable
private fun ConversationPanel(
    blocks: List<ConversationBlock>,
    isTopPanel: Boolean,
    language: LanguageOption,
    listState: LazyListState,
    activeField: ActiveField,
    partialText: String,
    ttsBlockId: Long?,
    onSpeakBlock: (Long, String, String) -> Unit,
    fontSize: Float,
    fontColor: Color,
    modifier: Modifier = Modifier
) {
    val isThisFieldActive = (isTopPanel && activeField == ActiveField.TOP) ||
                             (!isTopPanel && activeField == ActiveField.BOTTOM)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (blocks.isEmpty() && !isThisFieldActive) {
            item {
                // Platzhalter-Text wenn keine Blöcke
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = language.flag,
                            fontSize = 48.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Tippen zum Sprechen",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
            val text = if (isTopPanel) block.topText else block.bottomText
            val speakerLocale = if (isTopPanel) language.localeCode else language.localeCode

            ConversationBlockItem(
                text = text,
                isFromThisSide = (isTopPanel && block.isFromTop) || (!isTopPanel && !block.isFromTop),
                blockId = block.id,
                isTtsSpeaking = ttsBlockId == block.id,
                onSpeakClick = { onSpeakBlock(block.id, text, speakerLocale) },
                showDivider = index < blocks.lastIndex,
                isFieldActive = isThisFieldActive,
                fontSize = fontSize,
                fontColor = fontColor
            )
        }

        // Partial-Ergebnis (wird während der Erkennung/Übersetzung angezeigt)
        if (partialText.isNotBlank() && isThisFieldActive) {
            item {
                Text(
                    text = partialText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize.sp,
                        color = fontColor // Gleiche Farbe wie finaler Text
                    ),
                    fontStyle = FontStyle.Normal // Nicht mehr kursiv, damit es "echt" aussieht
                )
            }
        }
    }
}

/**
 * Einzelner Gesprächsblock mit Text und TTS-Button.
 */
@Composable
private fun ConversationBlockItem(
    text: String,
    isFromThisSide: Boolean,
    blockId: Long,
    isTtsSpeaking: Boolean,
    onSpeakClick: () -> Unit,
    showDivider: Boolean,
    isFieldActive: Boolean,
    fontSize: Float,
    fontColor: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Text des Blocks
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp),
                modifier = Modifier.weight(1f),
                color = when {
                    isFieldActive -> Color.Black
                    isFromThisSide -> fontColor
                    else -> fontColor.copy(alpha = 0.7f)
                },
                fontWeight = if (isFromThisSide) FontWeight.Normal else FontWeight.Light
            )

            // TTS Lautsprecher-Button
            IconButton(
                onClick = onSpeakClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isTtsSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = if (isTtsSpeaking) "Vorlesen stoppen" else "Vorlesen",
                    tint = if (isTtsSpeaking) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Trennlinie zwischen Blöcken
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
        }
    }
}

/**
 * Mikrofon-Indikator – pulsiert wenn aktiv.
 */
@Composable
private fun MicrophoneIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "mic_alpha"
    )

    Box(
        modifier = modifier
            .padding(8.dp)
            .size(40.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Mikrofon aktiv",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Berechnet den skalierten Scroll-Offset für synchrones Scrollen.
 *
 * Da Blöcke in beiden Sprachen unterschiedlich viele Zeilen haben,
 * wird der Offset proportional zur Zeilenzahl-Differenz skaliert.
 *
 * @param blocks Alle Gesprächsblöcke
 * @param index Aktueller Block-Index
 * @param offset Pixel-Offset im aktuellen Block
 * @param fromTop true = oben→unten skalieren, false = unten→oben
 */
private fun scaleScrollOffset(
    blocks: List<ConversationBlock>,
    index: Int,
    offset: Int,
    fromTop: Boolean
): Int {
    if (index >= blocks.size) return offset
    val block = blocks[index]
    val ratio = if (fromTop) {
        if (block.topLineCount > 0) block.bottomLineCount.toFloat() / block.topLineCount
        else 1f
    } else {
        if (block.bottomLineCount > 0) block.topLineCount.toFloat() / block.bottomLineCount
        else 1f
    }
    return (offset * ratio).toInt()
}
