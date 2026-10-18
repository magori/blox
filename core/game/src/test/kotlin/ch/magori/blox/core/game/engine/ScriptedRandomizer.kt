package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.Randomizer

/** A randomizer that deals [pieces] in order and records the `previous` it was given on each draw. */
internal class ScriptedRandomizer(vararg pieces: PieceType) : Randomizer {
    private val remaining = ArrayDeque(pieces.toList())
    private val previousSoFar = mutableListOf<PieceType?>()

    /** The `previous` argument of each draw so far, in order. */
    val previousArguments: List<PieceType?> get() = previousSoFar.toList()

    override fun next(previous: PieceType?): PieceType {
        previousSoFar += previous
        return remaining.removeFirst()
    }
}
