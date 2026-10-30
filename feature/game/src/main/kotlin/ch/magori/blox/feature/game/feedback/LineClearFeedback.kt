package ch.magori.blox.feature.game.feedback

import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Rotation

/**
 * The feedback, on each tick, of a game whose [piece], turned by A, locked on [lockTick] and filled [rows], cleared
 * [clearDelay] ticks later as the engine says (`Phase.Clearing`), the score going from [scoreBefore] to [score]: the
 * ticks of the clear, then the tick of [GameEvent.LinesCleared] and those after it. For the previews and the tests,
 * which show the effects at fixed ticks.
 */
internal fun lineClearFeedback(
    piece: Piece,
    rows: List<Int>,
    lockTick: Int,
    clearDelay: Int,
    scoreBefore: Points,
    score: Points,
): (tick: Int) -> GameFeedback {
    val locked = GameFeedback().after(
        lockTick,
        listOf(GameEvent.PieceRotated(piece, Rotation.CLOCKWISE), GameEvent.PieceLocked(piece, bySoftDrop = false)),
        ClearingRows(rows, ticksLeft = clearDelay),
        scoreBefore,
        scoreBefore,
    )
    val clearTick = lockTick + clearDelay
    val cleared = locked.after(
        clearTick,
        listOf(GameEvent.LinesCleared(LineClear(rows.size))),
        null,
        scoreBefore,
        score,
    )
    return { tick ->
        if (tick < clearTick) {
            locked.after(tick, emptyList(), ClearingRows(rows, ticksLeft = clearTick - tick), scoreBefore, scoreBefore)
        } else {
            cleared.after(tick, emptyList(), null, score, score)
        }
    }
}
