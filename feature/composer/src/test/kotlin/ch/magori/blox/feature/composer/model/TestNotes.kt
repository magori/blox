package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.bassVoice
import ch.magori.blox.core.music.melodyVoice

internal fun pitch(name: String): Pitch = Pitch.parse(name)

/** Notes written `E4:1.5` (pitch and beats) or `r:0.5` (a rest), separated by spaces. */
internal fun notes(text: String): List<Note> = text.trim().split(" ").filter(String::isNotEmpty).map { token ->
    val (name, beats) = token.split(":")
    Note(if (name == "r") null else pitch(name), beats.toDouble())
}

/** [notes] played in the first bars, then [bars] bars of silence. */
internal fun thenSilence(
    notes: List<Note>,
    bars: Int,
): List<Note> = notes + List(bars) { Note(null, 4.0) }

/** A score of four beats a bar at [bpm], with the voices of the scores: the [melody], then the [bass]. */
internal fun score(
    melody: List<Note>,
    bass: List<Note>,
    bpm: Int = 120,
): Score = Score(
    id = ScoreId.KOROBEINIKI,
    bpm = bpm,
    beatsPerBar = 4,
    voices = listOf(
        melodyVoice(melody),
        bassVoice(bass),
    ),
)

/** Eight bars of silence. */
internal fun silence(): List<Note> = thenSilence(emptyList(), bars = 8)
