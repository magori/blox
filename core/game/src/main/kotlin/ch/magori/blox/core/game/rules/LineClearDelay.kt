package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Ticks

/**
 * Line clear delay policy: how many ticks the line clear animation lasts, between the lock that fills
 * rows and the cleared board. The policy is interchangeable: [ClassicLineClearDelay] holds the NES values.
 */
public interface LineClearDelay {
    /**
     * Ticks of the animation when the piece locked on [lockTick], the tick count of the game on that tick:
     * the NES aligns its animation on a frame counter. The entry delay comes after it.
     *
     * @throws IllegalArgumentException if [lockTick] is negative.
     */
    public fun ticksToClear(lockTick: Int): Ticks
}
