package com.sprachbruecke.translator.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sprachbruecke.translator.service.TranslationMode
import com.sprachbruecke.translator.ui.theme.*

/**
 * Trennlinie zwischen den beiden Sprachfeldern.
 *
 * Zeigt in der Mitte den aktuellen Übersetzungsmodus an:
 *  ☁️ Blau  = Cloud (Gemini Flash)
 *  📱 Grün  = On-Device (Gemini Nano)
 *  🔴 Rot pulsierend = Fehler / kein Netz
 */
@Composable
fun DividerBar(
    translationMode: TranslationMode,
    modifier: Modifier = Modifier
) {
    val (iconColor, icon, contentDesc) = when (translationMode) {
        TranslationMode.CLOUD -> Triple(
            CloudModeColor,
            Icons.Default.Cloud,
            "Cloud-Übersetzung aktiv"
        )
        TranslationMode.ON_DEVICE -> Triple(
            OnDeviceModeColor,
            Icons.Default.PhoneAndroid,
            "Gerät-interne Übersetzung aktiv"
        )
        TranslationMode.ERROR -> Triple(
            ErrorModeColor,
            Icons.Default.SignalWifiOff,
            "Kein Netz – Übersetzung nicht möglich"
        )
    }

    // Pulsier-Animation für Fehlerzustand
    val infiniteTransition = rememberInfiniteTransition(label = "divider_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (translationMode == TranslationMode.ERROR) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Linke Linie
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.5.dp,
            color = DividerColor
        )

        // Modus-Indikator in der Mitte
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // Hintergrundkreis
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(if (translationMode == TranslationMode.ERROR) scale else 1f)
                    .background(
                        color = iconColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = iconColor,
                modifier = Modifier
                    .size(20.dp)
                    .scale(if (translationMode == TranslationMode.ERROR) scale else 1f)
            )
        }

        // Rechte Linie
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.5.dp,
            color = DividerColor
        )
    }
}
