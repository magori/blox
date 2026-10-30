package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.model.Points

/** Something [TempoScore] saw in a tick of Au tempo, for the screen and the sound. */
public sealed interface TempoEvent {
    /** A piece was locked on the beat, by the held soft drop: it earned [bonus]. */
    public data class OnBeat(public val bonus: Points) : TempoEvent

    /** Lines were cleared by a piece locked on the beat in bar 4 of the phrase: they earned [bonus] once more. */
    public data class CadenceCleared(public val bonus: Points) : TempoEvent
}
