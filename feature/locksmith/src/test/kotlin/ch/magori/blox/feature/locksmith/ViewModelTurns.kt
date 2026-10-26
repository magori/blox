package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Rotation

/** Turns the selected piece of the game of this view model clockwise until it takes [orientation]. */
internal fun LocksmithViewModel.turnTo(orientation: Orientation) {
    while (uiState.value.game.let { it.orientationOf(it.selected!!) } != orientation) turn(Rotation.CLOCKWISE)
}
