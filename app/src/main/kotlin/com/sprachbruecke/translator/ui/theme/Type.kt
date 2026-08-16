package com.sprachbruecke.translator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typografie optimiert für Lesbarkeit – große Schrift, hoher Kontrast.
 * Senioren-optimiert: Minimum 20sp für Gesprächstext.
 */
val AppTypography = Typography(
    // Gesprächstext – groß und klar
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp
    ),
    // Sprach-Label (Flagge + Name)
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    // Zwischen-Ergebnis (partielle ASR)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = androidx.compose.ui.graphics.Color.Gray
    ),
)
