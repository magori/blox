package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Ticks

/**
 * Line clear delay of the Classic mode (NES, 1989): 5 passes, one every 4 ticks, the last one on a
 * multiple of 4 of the tick count: 16 ticks, plus the ticks up to the next multiple of 4 of the lock
 * tick, 17 to 20 ticks in all. The NES counts every frame since power on; the game tick count is used
 * instead, so that all follows from the seed.
 */
public object ClassicLineClearDelay : LineClearDelay {
    /** The fixed part of the animation, in ticks. */
    private const val ANIMATION_TICKS = 16

    /** The animation ends on a multiple of this number of ticks. */
    private const val STEP = 4

    /** @throws IllegalArgumentException if [lockTick] is negative. */
    override fun ticksToClear(lockTick: Int): Ticks {
        require(lockTick >= 0) { "A lock tick cannot be negative: $lockTick" }
        return Ticks(ANIMATION_TICKS + (STEP - lockTick % STEP))
    }
}
