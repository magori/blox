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

    /** A [hint] is given. */
    public data class HintGiven(public val hint: Hint) : PadlockEvent

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

/** What the hint points at. */
public sealed interface Hint {
    /** The free given piece of [rank] fits as [piece], on the way to open the padlock. */
    public data class Fit(public val rank: Int, public val piece: Piece) : Hint

    /** The placed piece of [rank] blocks: taken back, it leaves a way to open the padlock. */
    public data class Blocking(public val rank: Int) : Hint
}
