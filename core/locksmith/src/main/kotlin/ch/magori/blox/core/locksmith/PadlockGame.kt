package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.music.Chord
import ch.magori.blox.core.music.Progression

/**
 * A game of the Locksmith on [padlock]: the pieces [given] to the player, identified by their rank,
 * each in its own orientation, those [placed] in the hole, in the order they were placed, the
 * [selected] one, and the counters of accepted placements ([moves], those taken back included) and
 * of [refusals] and [hints]. Immutable: every action gives a new game. Once the padlock [isOpen], only
 * [restart] changes it. A rank that is not given is a programming error.
 */
@ConsistentCopyVisibility
public data class PadlockGame private constructor(
    public val padlock: Padlock,
    private val orientations: List<Orientation>,
    private val placements: List<Pair<Int, Piece>>,
    public val selected: Int?,
    public val moves: Int,
    public val refusals: Int,
    public val hints: Int,
) {
    /** The pieces given to the player, by rank. */
    public val given: List<PieceType> get() = padlock.givenPieces

    /** The placed pieces, by rank. */
    public val placed: Map<Int, Piece> get() = placements.toMap()

    /** The ranks of the placed pieces, in the order they were placed. */
    public val placementOrder: List<Int> get() = placements.map { it.first }

    /**
     * The chords heard so far, in [PadlockChords.KEY]: one per piece on the grid, the first ones of the
     * progression of the padlock, so that the last placement resolves on I.
     */
    public val chords: List<Chord> get() = progressionChords.take(placements.size)

    /** The chords of this padlock, one per given piece, in the order they are heard. */
    public val progression: Progression get() = PadlockChords.progression(given.size)

    /** The chords of the [progression], in [PadlockChords.KEY]: all of them are heard once the padlock is open. */
    public val progressionChords: List<Chord> get() = progression.numerals.map(PadlockChords.KEY::chord)

    /** Whether every given piece is placed: the padlock is open. */
    public val isOpen: Boolean get() = placements.size == given.size

    /** The result of the opening once the padlock [isOpen], `null` before. */
    public val result: PadlockResult? get() = if (isOpen) PadlockResult(given.size, moves, refusals, hints) else null

    /** The orientation the given piece of [rank] is in. */
    public fun orientationOf(rank: Int): Orientation = orientations[requireGiven(rank)]

    /** The rank of the placed piece that covers [cell], or `null` when none does. */
    public fun rankAt(cell: Position): Int? = placements.firstOrNull { (_, piece) -> cell in piece.cells }?.first

    /** The [selected] piece, in its orientation, its pivot on [pivot], as [place] would put it; `null` if none is. */
    public fun selectedAt(pivot: Position): Piece? = selected?.let { Piece(given[it], orientations[it], pivot) }

    /** Selects the free given piece of [rank]; a placed one is taken back by [takeBack]. */
    public fun select(rank: Int): PadlockGame =
        if (isOpen || requireGiven(rank) in placementOrder) this else copy(selected = rank)

    /** Turns the [selected] piece a quarter turn by [rotation], round its pivot. */
    public fun turn(rotation: Rotation): PadlockGame {
        val rank = selected ?: return this
        val turned = ClassicRotationSystem.rotate(given[rank], orientations[rank], rotation)
        return copy(orientations = orientations.toMutableList().also { it[rank] = turned })
    }

    /**
     * Places the [selected] piece, in its orientation, its pivot on [pivot]: accepted when every cell
     * lies in the hole, on a free cell, then the first free piece is selected, and the padlock opens
     * with the last one; refused otherwise, a cell out of the hole before one over a placed piece.
     */
    public fun place(pivot: Position): PadlockUpdate {
        val rank = selected ?: return PadlockUpdate(this, emptyList())
        val piece = checkNotNull(selectedAt(pivot))
        val refusal = when {
            !piece.cells.all { it in padlock.hole } -> Refusal.OUT_OF_HOLE
            placements.any { (_, other) -> other.cells.any { it in piece.cells } } -> Refusal.OVERLAP
            else -> null
        }
        if (refusal != null) {
            return PadlockUpdate(copy(refusals = refusals + 1), listOf(PadlockEvent.PlacementRefused(rank, refusal)))
        }
        val nowPlaced = placements + (rank to piece)
        val game = copy(
            placements = nowPlaced,
            selected = freeRanks(nowPlaced).firstOrNull(),
            moves = moves + 1,
        )
        val placedEvent = PadlockEvent.PiecePlaced(rank, piece)
        val events = if (game.isOpen) listOf(placedEvent, PadlockEvent.PadlockOpened) else listOf(placedEvent)
        return PadlockUpdate(game, events)
    }

    /** Takes the placed piece of [rank] back out of the hole: it becomes the selected one, in its orientation. */
    public fun takeBack(rank: Int): PadlockUpdate {
        if (isOpen || requireGiven(rank) !in placementOrder) return PadlockUpdate(this, emptyList())
        return PadlockUpdate(
            copy(placements = placements.filterNot { it.first == rank }, selected = rank),
            listOf(PadlockEvent.PieceTakenBack(rank)),
        )
    }

    /**
     * A hint, counted in [hints]: where a free piece fits on a way to open the padlock, found by
     * [PadlockSolver] in its fixed order; when no way is left, the latest placed piece whose removal
     * alone leaves one, or else the last piece placed.
     */
    public fun hint(): PadlockUpdate {
        if (isOpen) return PadlockUpdate(this, emptyList())
        val hint = wayWith(placements)?.first()?.let { (rank, piece) -> Hint.Fit(rank, piece) }
            ?: Hint.Blocking(
                placementOrder.lastOrNull { rank -> wayWith(placements.filterNot { it.first == rank }) != null }
                    // Nothing placed always leaves a way: a padlock is built from its solution.
                    ?: checkNotNull(placementOrder.lastOrNull()) { "A padlock with nothing placed has a way" },
            )
        return PadlockUpdate(copy(hints = hints + 1), listOf(PadlockEvent.HintGiven(hint)))
    }

    /** A way to fill the hole left free by the [kept] placements with the other given pieces. */
    private fun wayWith(kept: List<Pair<Int, Piece>>): List<Pair<Int, Piece>>? {
        val free = padlock.hole - kept.flatMap { it.second.cells }.toSet()
        return PadlockSolver.solve(free, freeRanks(kept).associateWith { given[it] })
    }

    /** The ranks of the given pieces left out of [placements], in order. */
    private fun freeRanks(placements: List<Pair<Int, Piece>>): List<Int> =
        given.indices.filter { rank -> placements.none { it.first == rank } }

    /** The same padlock, from the start: nothing placed, every counter back to zero. */
    public fun restart(): PadlockGame = start(padlock)

    private fun requireGiven(rank: Int): Int = rank.also {
        require(it in given.indices) { "No given piece of rank $it among ${given.size}" }
    }

    public companion object {
        /** A new game on [padlock]: nothing placed, every piece in its spawn orientation, the first one selected. */
        public fun start(padlock: Padlock): PadlockGame = PadlockGame(
            padlock = padlock,
            orientations = padlock.givenPieces.map(ClassicRotationSystem::spawnOrientation),
            placements = emptyList(),
            selected = 0,
            moves = 0,
            refusals = 0,
            hints = 0,
        )
    }
}
