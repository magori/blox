package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Rotation

/** This game, its selected piece turned clockwise until it takes [orientation]. */
fun PadlockGame.turnedTo(orientation: Orientation): PadlockGame =
    generateSequence(this) { it.turn(Rotation.CLOCKWISE) }.first { game ->
        game.orientationOf(game.selected!!) ==
            orientation
    }

/** This game, [piece] placed: a free given piece of its type selected, turned as it and put down on its pivot. */
fun PadlockGame.placing(piece: Piece): PadlockGame {
    val rank = given.indices.first { it !in placed && given[it] == piece.type }
    return select(rank).turnedTo(piece.orientation).place(piece.pivot).game
}

/** This game, every piece of the solution placed where it lies: the padlock open. */
fun PadlockGame.solved(): PadlockGame = padlock.solution.fold(this) { game, piece -> game.placing(piece) }
