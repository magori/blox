package ch.magori.blox.core.music

import kotlin.random.Random

/** What the player picks before rolling the dice: a [Category], or [Surprise] to let the draw pick one. */
public sealed interface CategoryChoice {
    /** The category to play, drawn with [random] if need be: the same draw gives the same category. */
    public fun resolve(random: Random): Category
}

/** The category drawn at random among all of them, never itself. */
public data object Surprise : CategoryChoice {
    override fun resolve(random: Random): Category = Category.entries.random(random)
}
