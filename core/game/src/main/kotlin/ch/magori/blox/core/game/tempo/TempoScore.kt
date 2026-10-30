package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.engine.GameUpdate
import ch.magori.blox.core.game.model.Points

/**
 * The bonuses of Au tempo, kept out of the engine (ADR 0028): an immutable value fed, tick after tick, by the events
 * of the engine and read against [meter]. The score shown is the points of the engine plus [bonus] ([total]).
 *
 * - **On the beat**: a piece locked by the held soft drop ([GameEvent.PieceLocked.bySoftDrop]) on a tick on the beat
 *   ([Meter.isOnBeat]) earns [BEAT_BONUS] ([TempoEvent.OnBeat]). A piece locked by the gravity alone earns nothing.
 * - **Cadence**: the lines of a piece locked on the beat in bar 4 of the phrase, the bar of the nearest quarter note
 *   ([Meter.barOfNearestBeat]), earn their points once more ([TempoEvent.CadenceCleared]). They are cleared at the end
 *   of their animation, often in the next bar: the tick of the lock is kept until the [GameEvent.LinesCleared] that
 *   follows, and the points of that clear are read as the difference of the points of the engine over its tick.
 *
 * @property bonus the points earned on top of the points of the engine.
 * @property piecesOnBeat the pieces locked on the beat.
 * @property piecesLocked every piece locked, on the beat or not.
 */
@ConsistentCopyVisibility
public data class TempoScore internal constructor(
    public val meter: Meter,
    public val bonus: Points,
    public val piecesOnBeat: Int,
    public val piecesLocked: Int,
    /** The tick of the last lock if it was on the beat, else null: the cadence of its lines is read from it. */
    internal val onBeatLockTick: Int?,
) {
    /** The score at the start of a game: no bonus, no piece. */
    public constructor(meter: Meter) : this(meter, Points(0), piecesOnBeat = 0, piecesLocked = 0, onBeatLockTick = null)

    /**
     * This score after one tick of the engine: [before] is exactly the state passed to `GameEngine.tick` for that tick,
     * and [update] what it gave back. Call it after every `GameEngine.tick`, each of the ticks of a frame included
     * (`FrameTicks`): the points of a clear are read as the difference between [before] and [update].
     *
     * It assumes that [GameEvent.PieceLocked] and [GameEvent.LinesCleared] never come in the same tick, the rows being
     * cleared after an animation of at least one tick (`LineClearDelay`): a lock without one (P6) would have to be read
     * again.
     */
    public fun after(
        before: GameState,
        update: GameUpdate,
    ): TempoUpdate {
        val locked = update.events.filterIsInstance<GameEvent.PieceLocked>().firstOrNull()
        return when {
            locked != null -> afterLock(locked, lockTick = update.state.tickCount)
            update.events.any { it is GameEvent.LinesCleared } -> afterClear(before, update.state)
            else -> TempoUpdate(this, emptyList())
        }
    }

    /** The score of the game in [state]: the points of the engine plus the bonuses. */
    public fun total(state: GameState): Points = state.progress.score + bonus

    private fun afterLock(
        locked: GameEvent.PieceLocked,
        lockTick: Int,
    ): TempoUpdate {
        val counted = copy(piecesLocked = piecesLocked + 1, onBeatLockTick = null)
        if (!locked.bySoftDrop || !meter.isOnBeat(lockTick)) return TempoUpdate(counted, emptyList())
        val onBeat = counted.copy(
            bonus = bonus + BEAT_BONUS,
            piecesOnBeat = piecesOnBeat + 1,
            onBeatLockTick = lockTick,
        )
        return TempoUpdate(onBeat, listOf(TempoEvent.OnBeat(BEAT_BONUS)))
    }

    private fun afterClear(
        before: GameState,
        after: GameState,
    ): TempoUpdate {
        val lockTick = onBeatLockTick
        if (lockTick == null || meter.barOfNearestBeat(lockTick) != CADENCE_BAR) return TempoUpdate(this, emptyList())
        val cadence = Points(after.progress.score.value - before.progress.score.value)
        return TempoUpdate(copy(bonus = bonus + cadence), listOf(TempoEvent.CadenceCleared(cadence)))
    }

    public companion object {
        /** The bonus of a piece locked on the beat, whatever the level. (à vérifier) to be set by playing. */
        public val BEAT_BONUS: Points = Points(80)

        /** The bar of the cadence, the last of the phrase. */
        private const val CADENCE_BAR = 4
    }
}

/** What [TempoScore.after] gives back: the new [score], and the [events] of that tick, in order. */
public data class TempoUpdate(public val score: TempoScore, public val events: List<TempoEvent>)
