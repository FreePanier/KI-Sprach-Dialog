package com.sprachbruecke.translator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprachbruecke.translator.data.LanguageOption

/**
 * Großer, senior-freundlicher Sprach-Button mit Emoji-Flagge und Sprachname.
 * Minimale Touch-Größe: 60dp Höhe.
 */
@Composable
fun LanguageButton(
    language: LanguageOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Emoji-Flagge – groß und gut sichtbar
            Text(
                text = language.flag,
                fontSize = 28.sp
            )
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
