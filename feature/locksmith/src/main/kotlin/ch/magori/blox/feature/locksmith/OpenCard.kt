package ch.magori.blox.feature.locksmith

/**
 * What the card « Serrure ouverte » shows: the counters of the game, apart, [moves] (the placements accepted, those
 * taken back included), [refusals] and [hints] (rules, Partie).
 */
internal data class OpenCard(val moves: Int, val refusals: Int, val hints: Int)

/** The card « Serrure ouverte » of [state]; none while the padlock is closed. */
internal fun openCardOf(state: LocksmithUiState): OpenCard? {
    val game = state.game
    return if (game.isOpen) OpenCard(moves = game.moves, refusals = game.refusals, hints = game.hints) else null
}
