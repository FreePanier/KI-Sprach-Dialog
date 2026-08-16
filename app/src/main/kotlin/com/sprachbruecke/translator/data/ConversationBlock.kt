package com.sprachbruecke.translator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein Gesprächsblock repräsentiert einen einzelnen Sprechakt mit Original und Übersetzung.
 * Beide Textfelder werden synchron gescrollt anhand der Zeilenzahlen.
 */
@Entity(tableName = "conversation_blocks")
data class ConversationBlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Text in der oberen Sprache (Sprache A) */
    val topText: String,

    /** Text in der unteren Sprache (Sprache B) */
    val bottomText: String,

    /** Gezählte Zeilen im oberen Feld – für proportionales Scrollen */
    val topLineCount: Int = 1,

    /** Gezählte Zeilen im unteren Feld – für proportionales Scrollen */
    val bottomLineCount: Int = 1,

    /** Wer hat gesprochen: true = Person A (oben), false = Person B (unten) */
    val isFromTop: Boolean,

    /** Unix-Timestamp für Sortierung und Anzeige */
    val timestamp: Long = System.currentTimeMillis(),

    /** Sitzungs-ID für Gesprächsgruppen */
    val sessionId: Long = 0,
)
