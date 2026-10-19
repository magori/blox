package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Ticks

/**
 * Held inputs of the Classic mode (NES, 1989, NTSC). Source: the ROM disassembly in meatfighter.com,
 * "Applying AI to Nintendo Tetris".
 */
public object ClassicInputRepeat : InputRepeat {
    /** A shift moves on the press, then 16 ticks after the press, then every 6 ticks. */
    override val shift: RepeatTiming = RepeatTiming(stepsOnPress = true, firstRepeat = Ticks(16), interval = Ticks(6))

    /**
     * A soft drop moves down a first row 2 ticks after the press, then every 2 ticks. The ROM sets
     * its counter to 1 on the press (`$8934`), increments it every tick and waits while it is below
     * 3 (`$894E`); at 3, the piece moves down and the counter goes back to 1 (`$8954`).
     */
    override val softDrop: RepeatTiming =
        RepeatTiming(stepsOnPress = false, firstRepeat = Ticks(2), interval = Ticks(2))
}
