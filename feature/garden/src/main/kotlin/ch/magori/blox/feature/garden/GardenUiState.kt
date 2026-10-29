package ch.magori.blox.feature.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.data.GardenId
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.KeptGarden
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
 * [move] counts the changes of the garden, so that two same changes in a row stay two feedbacks. The kept garden the
 * garden under way comes from, [keptFrom], null if none, what the screen knows of « Mes jardins », [myGardens], and its
 * [sheet], while it is open; the name of the garden just kept, [keptName], while the message « « X » gardé. » shows.
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
    val keptFrom: GardenId? = null,
    val myGardens: MyGardens = MyGardens.Loading,
    val sheet: GardensSheet? = null,
    val keptName: String? = null,
) {
    /**
     * Why the garden under way cannot be kept as a new garden: the gardens not read, or unreadable; an empty garden;
     * then a full library. Null when it can.
     */
    val keepRefusal: KeepRefusal?
        get() {
            val gardens = when (myGardens) {
                MyGardens.Loading -> return KeepRefusal.LOADING
                MyGardens.Unreadable -> return KeepRefusal.UNREADABLE
                is MyGardens.Listed -> myGardens.gardens
            }
            return when {
                garden.pieces.isEmpty() -> KeepRefusal.EMPTY_GARDEN
                gardens.size >= GardenLibrary.MAX_GARDENS -> KeepRefusal.FULL
                else -> null
            }
        }

    /**
     * The kept gardens as the sheet lists them, from the most recent change to the oldest, the last one kept first
     * among those of the same time; none until they are read.
     */
    val keptGardens: List<KeptGarden>
        get() = (myGardens as? MyGardens.Listed)?.gardens.orEmpty().asReversed().sortedByDescending { it.modifiedAt }

    /** The kept garden the garden under way comes from, once the list is read and while it holds it; null otherwise. */
    val linked: KeptGarden?
        get() = (myGardens as? MyGardens.Listed)?.gardens?.find { it.id == keptFrom }

    /** Whether the garden under way changed since it was kept: the dot of the disc « Mes jardins ». */
    val modified: Boolean
        get() = linked?.let { it.garden != garden } ?: false

    /**
     * Whether « Enregistrer « X » » may replace X, the [linked] garden, with the garden under way: once it changed
     * since, and holds a piece; a full library still may.
     */
    val canSaveInPlace: Boolean
        get() = modified && garden.pieces.isNotEmpty()

    /** Whether the garden under way is as its [linked] garden was kept: « Enregistrer « X » » greyed, and why. */
    val unchanged: Boolean
        get() = linked != null && !modified

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
