package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.data.OpeningOutcome
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.locksmith.PadlockResult
import kotlin.math.PI
import kotlin.math.sin

/**
 * What the card « Serrure ouverte » shows: the counters of the game, apart, [moves] (the placements accepted, those
 * taken back included), [refusals] and [hints] (rules, Partie); the [best] opening kept of the padlock, when it is not
 * this one; the [sticker] that tells what the opening was; the frontier to resume at, [resumeAt], once an old padlock
 * is replayed; whether the opening is [unrecorded], the progress not written.
 */
internal data class OpenCard(
    val moves: Int,
    val refusals: Int,
    val hints: Int,
    val best: PadlockResult? = null,
    val sticker: OpenSticker? = null,
    val resumeAt: Int? = null,
    val unrecorded: Boolean = false,
)

/** The sticker of the card: NOUVEAU MEILLEUR, or PARFAITE. */
internal enum class OpenSticker { NEW_BEST, PERFECT }

/**
 * The card « Serrure ouverte » of [state]; none while the padlock is closed. A perfect opening bears PARFAITE, a fact of
 * the game; otherwise a new best, as the progress tells it, NOUVEAU MEILLEUR. The best opening kept waits for the
 * progress to tell what this opening was, written or not, so that it does not show and then go. When the next padlock
 * is open already, after the replay of an old one, the card offers to resume at the frontier.
 */
internal fun openCardOf(state: LocksmithUiState): OpenCard? {
    val game = state.game
    if (!game.isOpen) return null
    val sticker = when {
        game.result?.isPerfect == true -> OpenSticker.PERFECT
        state.outcome == OpeningOutcome.NEW_BEST -> OpenSticker.NEW_BEST
        else -> null
    }
    return OpenCard(
        moves = game.moves,
        refusals = game.refusals,
        hints = game.hints,
        best = state.best.takeIf { state.outcome != null || state.unrecorded }?.takeUnless { it == game.result },
        sticker = sticker,
        resumeAt = state.frontier?.takeIf { it > state.level + 1 },
        unrecorded = state.unrecorded,
    )
}

/**
 * How large the sticker of the card is drawn [tick]s after it shows, in parts of its size: it bounces in, from nothing a
 * little past its size, and rests at it once `motion-sticker` is over; in reduced effects, it is fixed at its size.
 */
internal fun stickerScale(
    tick: Int,
    effects: BloxEffects,
): Float {
    if (effects.reduced || tick >= BloxMotion.sticker) return 1f
    val progress = tick.coerceAtLeast(0).toFloat() / BloxMotion.sticker
    return if (progress < STICKER_PEAK_AT) {
        STICKER_PEAK * sin(progress / STICKER_PEAK_AT * PI / 2).toFloat()
    } else {
        STICKER_PEAK - (STICKER_PEAK - 1f) * (progress - STICKER_PEAK_AT) / (1f - STICKER_PEAK_AT)
    }
}

/** How far past its size the sticker bounces. */
private const val STICKER_PEAK = 1.15f

/** When the sticker is at its largest, in parts of its motion. */
private const val STICKER_PEAK_AT = 0.6f
