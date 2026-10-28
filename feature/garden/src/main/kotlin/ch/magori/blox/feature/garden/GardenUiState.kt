package ch.magori.blox.feature.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenPlacement
import ch.magori.blox.core.garden.GardenRefusal

/**
 * The screen of the musical garden (V40): the [garden] under way, its mood with it; the piece [chosen] in the tray, in
 * its [orientation]; the cell the player [aim]s it at, its pivot there: the preview, a state of the screen that the
 * garden does not hold; the garden from before Tout effacer or Au hasard, which « Annuler » gives back while it is
 * offered ([undo]); while the garden is listened to, the column played, [playedColumn], from 0, null otherwise. The
 * last [change] of the garden, which the screen animates (docs/jardin-musical.md, Retours), null when it animates none;
 * [move] counts the changes of the garden, so that two same changes in a row stay two feedbacks.
 */
@Immutable
internal data class GardenUiState(
    val garden: Garden,
    val chosen: PieceType = PieceType.T,
    val orientation: Orientation = ClassicRotationSystem.spawnOrientation(chosen),
    val aim: Position? = null,
    val undo: Garden? = null,
    val playedColumn: Int? = null,
    val change: GardenChange? = null,
    val move: Int = 0,
) {
    /** The chosen piece where the player aims it, its pivot on [aim]; null without an aim. */
    val preview: Piece?
        get() = aim?.let { Piece(chosen, orientation, it) }

    /** Why the garden refuses the [preview] where it is; null without a preview, or when it fits. */
    val refusal: GardenRefusal?
        get() = preview?.let { (garden.place(it) as? GardenPlacement.Refused)?.reason }
}

/** A change of the garden that the screen animates (docs/jardin-musical.md, Retours). */
@Immutable
internal sealed interface GardenChange {
    /** Poser: [piece] placed. */
    data class Placed(val piece: Piece) : GardenChange

    /** Au hasard: the pieces of the garden composed. */
    data object Rolled : GardenChange

    /** A mood chosen, the pieces where they were. */
    data object MoodChosen : GardenChange
}
