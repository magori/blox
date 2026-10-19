package ch.magori.blox.core.music

import kotlin.random.Random

/** The tempos a category allows, from [slowest] to [fastest] beats per minute, both included. */
public data class TempoRange(public val slowest: Int, public val fastest: Int) {
    init {
        require(slowest > 0) { "The slowest tempo is a positive number of beats per minute, was $slowest" }
        require(fastest >= slowest) { "The fastest tempo, $fastest, is slower than the slowest one, $slowest" }
    }

    /** A tempo of the range, drawn with [random]: the same draw gives the same tempo. */
    public fun draw(random: Random): Int = random.nextInt(slowest, fastest + 1)
}
