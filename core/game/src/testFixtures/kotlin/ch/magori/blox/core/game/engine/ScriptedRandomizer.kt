package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.Randomizer

/**
 * A randomizer that deals its pieces in order and records the `previous` it was given on each draw.
 * Once they are dealt, a further draw fails, unless the randomizer was made [cycling].
 */
public class ScriptedRandomizer private constructor(private val deck: List<PieceType>, private val cycles: Boolean) :
    Randomizer {
    /** Deals [pieces] in order, once. */
    public constructor(vararg pieces: PieceType) : this(pieces.toList(), cycles = false)

    private var drawn = 0
    private val previousSoFar = mutableListOf<PieceType?>()

    /** The `previous` argument of each draw so far, in order. */
    public val previousArguments: List<PieceType?> get() = previousSoFar.toList()

    override fun next(previous: PieceType?): PieceType {
        check(cycles || drawn < deck.size) { "Only ${deck.size} pieces were scripted" }
        previousSoFar += previous
        return deck[drawn++ % deck.size]
    }

    public companion object {
        /** Deals [pieces] in order, again and again: for tests that play a whole game. */
        public fun cycling(vararg pieces: PieceType): ScriptedRandomizer {
            require(pieces.isNotEmpty()) { "At least one piece must be scripted" }
            return ScriptedRandomizer(pieces.toList(), cycles = true)
        }
    }
}
