package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.data.PadlockOpening
import ch.magori.blox.core.locksmith.PadlockResult

/*
 * The opening of a game, `PadlockResult` of `core:locksmith`, and the opening the progress keeps, `PadlockOpening` of
 * `core:data`, which does not see the domain (ADR 0025, amendment P7.28): the same four integers under the same rules,
 * held in agreement by PadlockOpeningsTest, here, in the one module that sees both.
 */

/** The opening the progress keeps of this result. */
internal fun PadlockResult.toOpening(): PadlockOpening =
    PadlockOpening(pieces = pieces, moves = moves, refusals = refusals, hints = hints)

/** The result of the game this opening kept. */
internal fun PadlockOpening.toResult(): PadlockResult =
    PadlockResult(pieces = pieces, moves = moves, refusals = refusals, hints = hints)
