package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Rotation

/** Turns the selected piece of the game of this view model clockwise until it takes [orientation]. */
internal fun LocksmithViewModel.turnTo(orientation: Orientation) {
    while (uiState.value.game.let { it.orientationOf(it.selected!!) } != orientation) turn(Rotation.CLOCKWISE)
}

/** Selects the first free given piece of [type]. */
internal fun LocksmithViewModel.selectFree(type: PieceType) {
    val game = uiState.value.game
    select(game.given.indices.first { it !in game.placed && game.given[it] == type })
}

/** Places [piece] as the player would: a free given piece of its type chosen, turned as it and put on its pivot. */
internal fun LocksmithViewModel.placing(piece: Piece) {
    selectFree(piece.type)
    turnTo(piece.orientation)
    place(piece.pivot)
}
