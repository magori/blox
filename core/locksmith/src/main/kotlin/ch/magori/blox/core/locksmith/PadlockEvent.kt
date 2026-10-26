package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece

/** What an action of the player made happen in a [PadlockGame], for the screen to show and play. */
public sealed interface PadlockEvent {
    /** The given piece of [rank] is placed in the hole as [piece]. */
    public data class PiecePlaced(public val rank: Int, public val piece: Piece) : PadlockEvent

    /** The given piece of [rank] is not placed, for [reason]. */
    public data class PlacementRefused(public val rank: Int, public val reason: Refusal) : PadlockEvent

    /** The placed piece of [rank] is taken back out of the hole. */
    public data class PieceTakenBack(public val rank: Int) : PadlockEvent

    /** Every given piece is placed: the padlock opens. Follows the last [PiecePlaced]. */
    public data object PadlockOpened : PadlockEvent
}

/** Why a placement is refused. */
public enum class Refusal {
    /** A cell of the piece lies on the metal or off the grid. */
    OUT_OF_HOLE,

    /** A cell of the piece lies on a placed piece. */
    OVERLAP,
}

/** A [game] after an action, and the [events] the action made happen, in order. */
public data class PadlockUpdate(public val game: PadlockGame, public val events: List<PadlockEvent>)
