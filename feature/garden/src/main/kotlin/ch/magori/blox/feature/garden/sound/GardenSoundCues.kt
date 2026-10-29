package ch.magori.blox.feature.garden.sound

import ch.magori.blox.core.audio.SoundCue
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.music.GardenSounds
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.SoundEffect

/**
 * The sound effects the garden may play, warmed up once when it opens and never released (ADR 0025, ADR 0027): a
 * note per pitch of the garden ([GardenMood.allPitches]), 25 in all.
 */
internal val GARDEN_WARM_UP: List<SoundEffect> = GardenMood.allPitches.map(GardenSounds::note)

/**
 * The sound of [preview] heard in [mood]: its notes once each, those of its cells inside the grid, in a rising
 * arpeggio. A note out of the effects [warmed] up would open a track that is never released: a bug, refused with an
 * `IllegalStateException` rather than played.
 */
internal fun previewCuesOf(
    preview: Piece,
    mood: GardenMood,
    warmed: Set<Pitch> = GardenMood.allPitches,
): List<SoundCue> = preview.cells
    .filter { it.column in 0 until Garden.COLUMNS && it.row in 0 until Garden.ROWS }
    .map { mood.pitchAt(it.row) }
    .distinct()
    .sortedBy(Pitch::midi)
    .let { arpeggioOf(it, warmed) }

/**
 * The sound of a garden kept in [mood] (docs/jardin-musical.md, Mes jardins): the chord of its tonic, the first of its
 * progression, as a rising arpeggio of its 3 lowest notes on the grid. A note out of the effects [warmed] up is refused
 * as for the preview.
 */
internal fun keptCuesOf(
    mood: GardenMood,
    warmed: Set<Pitch> = GardenMood.allPitches,
): List<SoundCue> {
    val tonic = mood.chordAt(0)
    return (Garden.ROWS - 1 downTo 0)
        .map(mood::pitchAt)
        .filter { it in tonic }
        .take(KEPT_NOTES)
        .let { arpeggioOf(it, warmed) }
}

/**
 * [pitches] played one after the other, in their order, [GardenSounds.NOTE_STEP_MILLIS] apart. A note out of the effects
 * [warmed] up would open a track that is never released: a bug, refused with an `IllegalStateException` rather than
 * played.
 */
private fun arpeggioOf(
    pitches: List<Pitch>,
    warmed: Set<Pitch>,
): List<SoundCue> = pitches.mapIndexed { index, pitch ->
    check(pitch in warmed) { "The note $pitch is not one of the effects warmed up" }
    SoundCue(GardenSounds.note(pitch), afterMillis = index.toLong() * GardenSounds.NOTE_STEP_MILLIS)
}

/** The notes of the arpeggio of a garden kept. */
private const val KEPT_NOTES = 3
