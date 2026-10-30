package ch.magori.blox.core.garden

/** What placing a piece in a [Garden] gives. */
public sealed interface GardenPlacement {
    /** The piece is placed: here is the [garden] with it. */
    public data class Placed(public val garden: Garden) : GardenPlacement

    /** The piece is refused, for [reason]: the garden is unchanged. */
    public data class Refused(public val reason: GardenRefusal) : GardenPlacement
}

/** Why a placement is refused. */
public enum class GardenRefusal {
    /** A cell of the piece lies off the grid. */
    OUT_OF_GRID,

    /** A cell of the piece lies on a placed piece. */
    OVERLAP,
}
