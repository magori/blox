package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position

/** [solution] written as [Padlock.parse] reads it: `#` for metal, each piece by its rank in the solution. */
fun asciiOf(solution: List<Piece>): String = (0 until Padlock.SIZE).joinToString("\n") { row ->
    (0 until Padlock.SIZE).joinToString("") { column ->
        val rank = solution.indexOfFirst { Position(column, row) in it.cells }
        if (rank == -1) "#" else "${rank + 1}"
    }
}
