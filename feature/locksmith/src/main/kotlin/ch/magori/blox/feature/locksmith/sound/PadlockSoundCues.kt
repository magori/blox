package ch.magori.blox.feature.locksmith.sound

import ch.magori.blox.core.audio.SoundCue
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockUpdate
import ch.magori.blox.core.music.Chord
import ch.magori.blox.core.music.LocksmithSounds
import ch.magori.blox.core.music.SoundEffect

/**
 * How long the opening flourish waits after the end of the arpeggio of the final chord (440 ms today, so 450 ms in
 * all): it comes once the cadence has resolved, without a silence between them.
 */
private const val FLOURISH_MARGIN_MILLIS = 10L

/**
 * The sound cues of [update], in the order of its events: a placement plays the arpeggio and the bass of the chord it
 * adds, the last of the chords of the game, together; the padlock opened adds the opening flourish, once the arpeggio
 * of the final chord is over; a refusal plays the refusal. Taking back and a hint play nothing: taking back cuts no
 * sound either.
 */
internal fun soundCuesFor(update: PadlockUpdate): List<SoundCue> = update.events.flatMap { event ->
    when (event) {
        is PadlockEvent.PiecePlaced -> LocksmithSounds.effectsOf(update.game.chords.last()).map(::SoundCue)
        PadlockEvent.PadlockOpened -> listOf(flourishAfter(update.game.chords.last()))
        is PadlockEvent.PlacementRefused -> listOf(SoundCue(LocksmithSounds.refusal))
        is PadlockEvent.PieceTakenBack, is PadlockEvent.HintGiven -> emptyList()
    }
}

/** The opening flourish, once the arpeggio of [finalChord] is over. */
private fun flourishAfter(finalChord: Chord): SoundCue =
    SoundCue(LocksmithSounds.openingFlourish, LocksmithSounds.arpeggio(finalChord).millis + FLOURISH_MARGIN_MILLIS)

/**
 * The sound effects [game] may play, once each, to warm up when its screen opens: those of every chord of its
 * progression, the opening flourish and the refusal.
 */
internal fun warmUpEffectsOf(game: PadlockGame): List<SoundEffect> {
    val effects = game.progressionChords.flatMap(LocksmithSounds::effectsOf) +
        LocksmithSounds.openingFlourish + LocksmithSounds.refusal
    return effects.distinct()
}
