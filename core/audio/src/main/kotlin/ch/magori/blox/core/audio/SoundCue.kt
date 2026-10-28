package ch.magori.blox.core.audio

import ch.magori.blox.core.music.SoundEffect
import kotlinx.coroutines.delay

/**
 * A sound [effect] to play [afterMillis] after the action that gives it, at once by default: the cues of an action of
 * the Locksmith (a chord, then the opening flourish) or of the musical garden (the notes of the preview, one after the
 * other). A wait is never negative.
 */
public data class SoundCue(public val effect: SoundEffect, public val afterMillis: Long = 0) {
    init {
        require(afterMillis >= 0) { "A cue waits a time that is not negative, was $afterMillis ms" }
    }
}

/**
 * Plays [cues], each once its wait is over, counted from the call, in the order of their waits (those of the same wait
 * in their order). It returns once the last one is played; cancelled before, the cues still waiting play no more: a
 * screen left, or the next arpeggio, cuts them.
 */
public suspend fun SoundEffectPlayer.playCues(cues: List<SoundCue>) {
    var waited = 0L
    cues.sortedBy(SoundCue::afterMillis).forEach { cue ->
        delay(cue.afterMillis - waited)
        waited = cue.afterMillis
        play(cue.effect)
    }
}
